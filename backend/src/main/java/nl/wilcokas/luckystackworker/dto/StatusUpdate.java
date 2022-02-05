package nl.wilcokas.luckystackworker.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatusUpdate {
	private int filesProcessedCount;
	private int totalfilesCount;
	private String message;
}
