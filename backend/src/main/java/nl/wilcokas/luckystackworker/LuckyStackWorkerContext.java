package nl.wilcokas.luckystackworker;

import java.time.LocalDateTime;

import ij.gui.Roi;
import lombok.Getter;
import lombok.Setter;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.dto.StatusUpdateDTO;

public class LuckyStackWorkerContext {
    private static String status = Constants.STATUS_WORKING;
    private static int filesProcessedCount = 0;
    private static int totalfilesCount = 0;
    private static boolean realTimeEnabled = false;
    private static boolean rootFolderSelected = false;
    private static boolean profileBeingApplied = false;
    private static boolean workerStopped = false;
    private static String selectedProfile;

    @Getter
    @Setter
    private static LocalDateTime activeOperationTime = null;

    @Getter
    @Setter
    private static Roi selectedRoi = null;

    private LuckyStackWorkerContext() {
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

    public static StatusUpdateDTO getStatus() {
        return StatusUpdateDTO.builder().message(status).filesProcessedCount(filesProcessedCount)
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

    public static void setRootFolderIsSelected() {
        rootFolderSelected = true;
    }

    public static boolean isProfileBeingApplied() {
        return profileBeingApplied;
    }

    public static void setProfileBeingApplied(boolean profileBeingApplied) {
        LuckyStackWorkerContext.profileBeingApplied = profileBeingApplied;
    }

    public static void setWorkerStopped(boolean workerStopped) {
        LuckyStackWorkerContext.workerStopped = workerStopped;
    }

    public static boolean isWorkerStopped() {
        return workerStopped;
    }

    public static String getSelectedProfile() {
        return selectedProfile;
    }

    public static void setSelectedProfile(String profileName) {
        selectedProfile = profileName;
    }
}
