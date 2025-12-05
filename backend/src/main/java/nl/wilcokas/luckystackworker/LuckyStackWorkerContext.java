package nl.wilcokas.luckystackworker;

import ij.gui.Roi;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.dto.StatusUpdateDTO;
import org.springframework.stereotype.Component;

@Component
public class LuckyStackWorkerContext {
    private String status = Constants.STATUS_WORKING;
    private int filesProcessedCount = 0;
    private int totalFilesCount = 0;

    private boolean realTimeEnabled = false;

    private boolean rootFolderSelected = false;

    private boolean profileBeingApplied = false;

    private boolean workerStopped = false;

    private String selectedProfile;

    private LocalDateTime activeOperationTime = null;

    private boolean roiActive = false;
    private Roi selectedRoi = null;

    public StatusUpdateDTO getStatusUpdateDTO() {
        return StatusUpdateDTO.builder()
                .message(status)
                .filesProcessedCount(filesProcessedCount)
                .totalFilesCount(totalFilesCount)
                .build();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getFilesProcessedCount() {
        return filesProcessedCount;
    }

    public void setFilesProcessedCount(int filesProcessedCount) {
        this.filesProcessedCount = filesProcessedCount;
    }

    public int getTotalFilesCount() {
        return totalFilesCount;
    }

    public void setTotalFilesCount(int totalFilesCount) {
        this.totalFilesCount = totalFilesCount;
    }

    public boolean isRealTimeEnabled() {
        return realTimeEnabled;
    }

    public void setRealTimeEnabled(boolean realTimeEnabled) {
        this.realTimeEnabled = realTimeEnabled;
    }

    public boolean isRootFolderSelected() {
        return rootFolderSelected;
    }

    public void setRootFolderSelected(boolean rootFolderSelected) {
        this.rootFolderSelected = rootFolderSelected;
    }

    public boolean isProfileBeingApplied() {
        return profileBeingApplied;
    }

    public void setProfileBeingApplied(boolean profileBeingApplied) {
        this.profileBeingApplied = profileBeingApplied;
    }

    public boolean isWorkerStopped() {
        return workerStopped;
    }

    public void setWorkerStopped(boolean workerStopped) {
        this.workerStopped = workerStopped;
    }

    public String getSelectedProfile() {
        return selectedProfile;
    }

    public void setSelectedProfile(String selectedProfile) {
        this.selectedProfile = selectedProfile;
    }

    public LocalDateTime getActiveOperationTime() {
        return activeOperationTime;
    }

    public void setActiveOperationTime(LocalDateTime activeOperationTime) {
        this.activeOperationTime = activeOperationTime;
    }

    public boolean isRoiActive() {
        return roiActive;
    }

    public void setRoiActive(boolean roiActive) {
        this.roiActive = roiActive;
    }

    public Roi getSelectedRoi() {
        return selectedRoi;
    }

    public void setSelectedRoi(Roi selectedRoi) {
        this.selectedRoi = selectedRoi;
    }
}
