package nl.wilcokas.luckystackworker.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VersionDTO {
  private String latestVersion;
  private int latestVersionConverted;
  private boolean isNewVersion;
  private List<String> releaseNotes;
}
