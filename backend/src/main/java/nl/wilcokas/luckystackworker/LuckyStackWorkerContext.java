package nl.wilcokas.luckystackworker;

import ij.gui.Roi;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.dto.StatusUpdateDTO;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
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
}
