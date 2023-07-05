package nl.wilcokas.luckystackworker;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import ij.gui.Roi;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.dto.StatusUpdate;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.model.Settings;
import nl.wilcokas.luckystackworker.worker.Worker;
import nl.wilcokas.luckystackworker.worker.WorkerException;

@Slf4j
public class LuckyStackWorkerContext {
    private static Worker worker;
    private static Map<String, String> workerProperties;
    private static String status = Constants.STATUS_WORKING;
    private static int filesProcessedCount = 0;
    private static int totalfilesCount = 0;
    private static boolean realTimeEnabled = false;
    private static boolean rootFolderSelected = false;
    private static boolean profileBeingApplied = false;

    @Getter
    @Setter
    private static LocalDateTime activeOperationTime = null;

    @Getter
    @Setter
    private static Roi selectedRoi = null;

    private LuckyStackWorkerContext() {
    }

    public static Worker getWorker() throws IOException {
        if (worker == null) {
            worker = new Worker(workerProperties);
        }
        return worker;
    }

    public static Map<String, String> getWorkerProperties() {
        if (workerProperties == null) {
            throw new WorkerException("Properties not loaded");
        }
        return workerProperties;
    }

    public static void loadWorkerProperties(Iterator<Profile> profiles, Settings settings) {
        if (workerProperties == null) {
            workerProperties = new HashMap<String, String>();
        }
        while (profiles.hasNext()) {
            Profile profile = profiles.next();
            updateWorkerForProfile(profile);
        }
        workerProperties.put("inputFolder", settings.getRootFolder());
        workerProperties.put("extensions", settings.getExtensions());
        workerProperties.put("outputFormat", settings.getOutputFormat());
        workerProperties.put("defaultProfile", settings.getDefaultProfile());
    }

    public static void updateWorkerForProfile(Profile profile) {
        String name = profile.getName();
        workerProperties.put(name + ".radius", String.valueOf(profile.getRadius()));
        workerProperties.put(name + ".amount", String.valueOf(profile.getAmount()));
        workerProperties.put(name + ".iterations", String.valueOf(profile.getIterations()));
        workerProperties.put(name + ".denoise", String.valueOf(profile.getDenoise()));
        workerProperties.put(name + ".denoiseSigma", String.valueOf(profile.getDenoiseSigma()));
        workerProperties.put(name + ".denoiseRadius", String.valueOf(profile.getDenoiseRadius()));
        workerProperties.put(name + ".denoiseIterations", String.valueOf(profile.getDenoiseIterations()));
        workerProperties.put(name + ".savitzkyGolayAmount", String.valueOf(profile.getSavitzkyGolayAmount()));
        workerProperties.put(name + ".savitzkyGolaySize", String.valueOf(profile.getSavitzkyGolaySize()));
        workerProperties.put(name + ".savitzkyGolayIterations", String.valueOf(profile.getSavitzkyGolayIterations()));
        workerProperties.put(name + ".clippingStrength", String.valueOf(profile.getClippingStrength()));
        workerProperties.put(name + ".clippingRange", String.valueOf(profile.getClippingRange()));
        workerProperties.put(name + ".deringRadius", String.valueOf(profile.getDeringRadius()));
        workerProperties.put(name + ".deringStrength", String.valueOf(profile.getDeringStrength()));
        workerProperties.put(name + ".sharpenMode", String.valueOf(profile.getSharpenMode()));
        workerProperties.put(name + ".gamma", String.valueOf(profile.getGamma()));
        workerProperties.put(name + ".contrast", String.valueOf(profile.getContrast()));
        workerProperties.put(name + ".brightness", String.valueOf(profile.getBrightness()));
        workerProperties.put(name + ".background", String.valueOf(profile.getBackground()));
        workerProperties.put(name + ".localContrastMode", String.valueOf(profile.getLocalContrastMode()));
        workerProperties.put(name + ".localContrastFine", String.valueOf(profile.getLocalContrastFine()));
        workerProperties.put(name + ".localContrastMedium", String.valueOf(profile.getLocalContrastMedium()));
        workerProperties.put(name + ".localContrastLarge", String.valueOf(profile.getLocalContrastLarge()));
        workerProperties.put(name + ".red", String.valueOf(profile.getRed()));
        workerProperties.put(name + ".green", String.valueOf(profile.getGreen()));
        workerProperties.put(name + ".blue", String.valueOf(profile.getBlue()));
        workerProperties.put(name + ".saturation", String.valueOf(profile.getSaturation()));
        workerProperties.put(name + ".dispersionCorrectionEnabled", Boolean.toString(profile.isDispersionCorrectionEnabled()));
        workerProperties.put(name + ".dispersionCorrectionRedX", String.valueOf(profile.getDispersionCorrectionRedX()));
        workerProperties.put(name + ".dispersionCorrectionRedY", String.valueOf(profile.getDispersionCorrectionRedY()));
        workerProperties.put(name + ".dispersionCorrectionBlueX", String.valueOf(profile.getDispersionCorrectionBlueX()));
        workerProperties.put(name + ".dispersionCorrectionBlueY", String.valueOf(profile.getDispersionCorrectionBlueY()));
        workerProperties.put(name + ".luminanceIncludeRed", String.valueOf(profile.isLuminanceIncludeRed()));
        workerProperties.put(name + ".luminanceIncludeGreen", String.valueOf(profile.isLuminanceIncludeGreen()));
        workerProperties.put(name + ".luminanceIncludeBlue", String.valueOf(profile.isLuminanceIncludeBlue()));
        workerProperties.put(name + ".luminanceIncludeColor", String.valueOf(profile.isLuminanceIncludeColor()));
    }

    public static String getSelectedProfile() {
        String profileName = workerProperties.get("selectedProfile");
        if (profileName == null) {
            log.info("Getting selected profile, no profile has been selected yet");
        } else {
            log.info("Getting selected profile, profile {} has been selected", profileName);
        }
        return profileName;
    }

    public static void setSelectedProfile(String profileName) {
        log.info("Setting selected profile to {}", profileName);
        workerProperties.put("selectedProfile", profileName);
    }

    public static void updateWorkerForRootFolder(String rootFolder) {
        rootFolderSelected = true;
        workerProperties.put("inputFolder", rootFolder);
    }

    public static void statusUpdate(String message) {
        status = message;
    }

    public static void setFilesProcessedCount(int aFilesProcessedCount) {
        filesProcessedCount = aFilesProcessedCount;
    }

    public static void setTotalfilesCount(int aTotalfilesCount) {
        totalfilesCount = aTotalfilesCount;
    }

    public static StatusUpdate getStatus() {
        return StatusUpdate.builder().message(status).filesProcessedCount(filesProcessedCount)
                .totalfilesCount(totalfilesCount).build();
    }

    public static boolean isRealTimeEnabled() {
        return realTimeEnabled;
    }

    public static void disableRealTimeEnabled() {
        realTimeEnabled = false;
    }

    public static void enableRealTimeEnabled() {
        realTimeEnabled = true;
    }

    public static boolean isRootFolderSelected() {
        return rootFolderSelected;
    }

    public static boolean isProfileBeingApplied() {
        return profileBeingApplied;
    }

    public static void setProfileBeingApplied(boolean profileBeingApplied) {
        LuckyStackWorkerContext.profileBeingApplied = profileBeingApplied;
    }

}
