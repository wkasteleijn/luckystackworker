package nl.wilcokas.luckystackworker.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VersionDTO {
	private String latestVersion;
	private boolean isNewVersion;
}
