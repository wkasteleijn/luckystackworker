package nl.wilcokas.luckystackworker.worker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.LuckyStackWorkerContext;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.service.WorkerService;
import nl.wilcokas.luckystackworker.util.Util;

@Slf4j
public class Worker extends Thread {

    private static final int WAIT_DELAY = 4000;
    private static final int REALTIME_WAIT_LOOP = 7;

    private Map<String, String> properties;
    private WorkerService workerService;

    private boolean running = true;

    private int realtimeCountdown = REALTIME_WAIT_LOOP;

    public Worker(Map<String, String> properties) throws IOException {
        this.properties = properties;
        this.workerService = new WorkerService(properties);
    }

    @Override
    public void run() {
        log.info("Worker started");
        while (running) {
            try {
                String activeProfile = LuckyStackWorkerContext.getActiveProfile();
                if (activeProfile != null) {
                    applyProfile(activeProfile);
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
                Util.pause(WAIT_DELAY);
            } catch (Exception e) {
                log.error("Error:", e);
                LuckyStackWorkerContext.inactivateProfile();
                LuckyStackWorkerContext.statusUpdate(Constants.STATUS_IDLE);
                LuckyStackWorkerContext.setFilesProcessedCount(0);
                LuckyStackWorkerContext.setTotalfilesCount(0);
            }
        }
    }

    public void stopRunning() {
        running = false;
    }

    private void applyProfile(String activeProfile) {
        log.info("Applying profile {}", activeProfile);
        Collection<File> files = getImages(true);
        LuckyStackWorkerContext.setTotalfilesCount(files.size());
        LuckyStackWorkerContext.setFilesProcessedCount(0);
        if (!processFiles(files)) {
            log.warn("Worker did not process any file from {}, active profile {} not matching with any files", getInputFolder(), activeProfile);
        }
        LuckyStackWorkerContext.inactivateProfile();
        LuckyStackWorkerContext.statusUpdate(Constants.STATUS_IDLE);
        LuckyStackWorkerContext.setFilesProcessedCount(0);
        LuckyStackWorkerContext.setTotalfilesCount(0);
    }

    private Collection<File> getImages(boolean recursive) {
        return FileUtils.listFiles(Paths.get(getInputFolder()).toFile(), getExtensions(), recursive);
    }

    private void realtimeProcess() {
        log.debug("Checking if there is any file to process...");
        try {
            Collection<File> files = getImages(false);
            for (File file : files) {
                String name = Util.getFilename(file);
                String extension = Util.getFilenameExtension(file);
                if (!name.endsWith(Constants.OUTPUT_POSTFIX) && Arrays.asList(getExtensions()).contains(extension)
                        && !isInFileList(files, name + Constants.OUTPUT_POSTFIX)) {
                    log.info("Realtime processing file {}", name);
                    if (workerService.processFile(file, true)) {
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
        return files.stream().map(f -> Util.getFilename(f)).anyMatch(filename::equals);
    }

    private boolean processFiles(Collection<File> files) {
        boolean filesProcessed = false;
        int count = 0;
        for (File file : files) {
            String name = Util.getFilename(file);
            String extension = Util.getFilenameExtension(file);
            if (!name.endsWith(Constants.OUTPUT_POSTFIX) && Arrays.asList(getExtensions()).contains(extension)) {
                filesProcessed = filesProcessed | workerService.processFile(file, false);
            }
            LuckyStackWorkerContext.setFilesProcessedCount(++count);
        }
        return filesProcessed;
    }

    private String getInputFolder() {
        return properties.get("inputFolder");
    }

    private String[] getExtensions() {
        return properties.get("extensions").split(",");
    }
}
