package nl.wilcokas.luckystackworker.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatusUpdateDTO {
    private int filesProcessedCount;
    private int totalFilesCount;
    private String message;
}
