package nl.wilcokas.luckystackworker.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class VersionDTO {
	private String latestVersion;
	private int latestVersionConverted;
	private boolean isNewVersion;
	private List<String> releaseNotes;
}
