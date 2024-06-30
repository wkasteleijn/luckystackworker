package nl.wilcokas.luckystackworker.service;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import nl.wilcokas.luckystackworker.service.dto.OpenImageModeEnum;
import org.apache.commons.io.FileUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import ij.ImagePlus;
import ij.io.Opener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.LuckyStackWorkerContext;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.exceptions.ProfileNotFoundException;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.util.LswFileUtil;

@Slf4j
@RequiredArgsConstructor
@Service
public class WorkerService {

    private final OperationService operationService;
    private final SettingsService settingsService;
    private final ProfileService profileService;

    private static final int WAIT_DELAY = 4000;
    private static final int REALTIME_WAIT_LOOP = 1;
    private int realtimeCountdown = REALTIME_WAIT_LOOP;

    @Scheduled(fixedDelay = WAIT_DELAY)
    public void doWork() {
        try {
            String profile = LuckyStackWorkerContext.getSelectedProfile();
            if (LuckyStackWorkerContext.isProfileBeingApplied()) {
                applyProfile(profile);
            } else {
                log.debug("Waiting for a profile to be applied...");
                if (LuckyStackWorkerContext.isRealTimeEnabled() && LuckyStackWorkerContext.isRootFolderSelected()) {
                    if (realtimeCountdown == 0) {
                        realtimeCountdown = REALTIME_WAIT_LOOP;
                        realtimeProcess();
                    } else {
                        realtimeCountdown--;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error:", e);
            LuckyStackWorkerContext.statusUpdate(Constants.STATUS_IDLE);
            LuckyStackWorkerContext.setFilesProcessedCount(0);
            LuckyStackWorkerContext.setTotalfilesCount(0);
            LuckyStackWorkerContext.setProfileBeingApplied(false);
        }
    }

    private void applyProfile(String activeProfile) {
        log.info("Applying profile {}", activeProfile);
        Collection<File> files = getImages(true);
        LuckyStackWorkerContext.setTotalfilesCount(files.size());
        LuckyStackWorkerContext.setFilesProcessedCount(0);
        if (!processFiles(files)) {
            log.warn("Worker did not process any file from {}, active profile {} not matching with any files", settingsService.getRootFolder(),
                    activeProfile);
        }
        LuckyStackWorkerContext.statusUpdate(Constants.STATUS_IDLE);
        LuckyStackWorkerContext.setFilesProcessedCount(0);
        LuckyStackWorkerContext.setTotalfilesCount(0);
        LuckyStackWorkerContext.setProfileBeingApplied(false);
    }

    private Collection<File> getImages(boolean recursive) {
        return FileUtils.listFiles(Paths.get(settingsService.getRootFolder()).toFile(), settingsService.getExtensions(), recursive);
    }

    private void realtimeProcess() {
        log.debug("Checking if there is any file to process...");
        try {
            Collection<File> files = getImages(true);
            for (File file : files) {
                String name = LswFileUtil.getFilename(LswFileUtil.getIJFileFormat(file.getAbsolutePath()));
                String extension = LswFileUtil.getFilenameExtension(file);
                if (!name.contains(Constants.OUTPUT_POSTFIX) && (!name.contains(Constants.OUTPUT_POSTFIX_SAVE))
                        && Arrays.asList(settingsService.getExtensions()).contains(extension)
                        && !isInFileList(files, name + Constants.OUTPUT_POSTFIX)) {
                    log.info("Realtime processing file {}", name);
                    if (processFile(file, true)) {
                        // If one file was successfully processed then keep it to that, else find a next
                        // one.
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error in realtime process:", e);
        }
    }

    private boolean isInFileList(Collection<File> files, String filename) {
        return files.stream().map(f -> LswFileUtil.getFilename(LswFileUtil.getIJFileFormat(f.getAbsolutePath()))).anyMatch(filename::equals);
    }

    private boolean processFiles(Collection<File> files) {
        boolean filesProcessed = false;
        int count = 0;
        for (File file : files) {
            String name = LswFileUtil.getFilename(file);
            String extension = LswFileUtil.getFilenameExtension(file);
            if (!name.contains(Constants.OUTPUT_POSTFIX) && (!name.contains(Constants.OUTPUT_POSTFIX_SAVE))
                    && Arrays.asList(settingsService.getExtensions()).contains(extension)) {
                filesProcessed = filesProcessed | processFile(file, false);
            }
            LuckyStackWorkerContext.setFilesProcessedCount(++count);
            if (LuckyStackWorkerContext.isWorkerStopped()) {
                LuckyStackWorkerContext.setWorkerStopped(false);
                break;
            }
        }
        return filesProcessed;
    }

    private boolean processFile(final File file, boolean realtime) {
        String filePath = file.getAbsolutePath();
        Optional<String> profileOpt = Optional.ofNullable(LswFileUtil.deriveProfileFromImageName(file.getAbsolutePath()));
        if (profileOpt.isEmpty()) {
            log.info("Could not determine a profile for file {}", filePath);
            return false;
        }
        String profileName = profileOpt.get();
        if (realtime || profileName.equals(LuckyStackWorkerContext.getSelectedProfile())) {
            try {
                final String filename = LswFileUtil.getImageName(LswFileUtil.getIJFileFormat(filePath));
                log.info("Applying profile '{}' to: {}", profileName, filename);
                if (!realtime) {
                    LuckyStackWorkerContext.statusUpdate("Processing : " + filename);
                }

                Profile profile = profileService.findByName(profileName)
                        .orElseThrow(() -> new ProfileNotFoundException(String.format("Unknown profile %s", profileName)));
                ImagePlus imp = LswFileUtil.openImage(filePath, OpenImageModeEnum.RGB, profile.getScale(),img -> operationService.scaleImage(img, profile.getScale()));
                if (LswFileUtil.validateImageFormat(imp, null, null)) {
                    if (LswFileUtil.isPngRgbStack(imp, filePath)) {
                        imp = LswFileUtil.fixNonTiffOpeningSettings(imp);
                    }
                    operationService.correctExposure(imp);
                    operationService.applyAllOperations(imp, null, profile);
                    imp.updateAndDraw();
                    if (LuckyStackWorkerContext.getSelectedRoi() != null) {
                        imp.setRoi(LuckyStackWorkerContext.getSelectedRoi());
                        imp = imp.crop();
                    }
                    LswFileUtil.saveImage(imp, profileName, getOutputFile(file), LswFileUtil.isPngRgbStack(imp, filePath) || profile.getScale() > 1.0,
                            LuckyStackWorkerContext.getSelectedRoi() != null, false, true);
                    return true;
                }
            } catch (Exception e) {
                log.error("Error processing file: ", e);
            }
        }
        return false;
    }

    private String getOutputFile(final File file) {
        return LswFileUtil.getPathWithoutExtension(file.getAbsolutePath()) + Constants.OUTPUT_POSTFIX + "."
                + Constants.DEFAULT_OUTPUT_FORMAT;
    }
}
