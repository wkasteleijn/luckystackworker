package nl.wilcokas.luckystackworker.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Version {
	private String latestVersion;
	private boolean isNewVersion;
}
