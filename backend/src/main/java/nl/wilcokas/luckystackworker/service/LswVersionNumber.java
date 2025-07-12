package nl.wilcokas.luckystackworker.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@Builder
public class LswVersionNumber {
  private int major;
  private int minor;
  private int patch;
  private boolean isInBeta;
  private List<String> releaseNotes;

  public static Optional<LswVersionNumber> fromString(String version) {
    return fromString(version, null);
  }

  public static Optional<LswVersionNumber> fromString(String version, String body) {
    String[] versionString =
        version != null && version.length() > 2 && version.contains(".")
            ? version.split("\\.")
            : null;
    if (versionString != null && versionString.length == 3) {
      int majorVersion = Integer.parseInt(versionString[0].replaceAll("[^0-9]", ""));
      int minorVersion = Integer.parseInt(versionString[1].replaceAll("[^0-9]", ""));
      int patchVersion = Integer.parseInt(versionString[2].replaceAll("[^0-9]", ""));
      List<String> releaseNotes = new ArrayList<>();
      if (body != null) {
        String releaseNotesString =
            body.contains("## Release notes")
                ? body.substring(body.indexOf("## Release notes") + 16)
                : StringUtils.EMPTY;
        releaseNotes =
            new ArrayList<>(
                    Arrays.asList(
                        releaseNotesString.replaceAll("-", "").replaceAll("\\r", "").split("\\n")))
                .stream().map(String::trim).filter(StringUtils::isNotEmpty).toList();
      }
      return Optional.of(
          LswVersionNumber.builder()
              .major(majorVersion)
              .minor(minorVersion)
              .patch(patchVersion)
              .releaseNotes(releaseNotes)
              .isInBeta(versionString[2].contains("beta"))
              .build());
    }
    return Optional.empty();
  }

  public int getConvertedVersion() {
    return major * 100000 + minor * 1000 + patch * 10 + (isInBeta ? -1 : 0);
  }

  public String toString() {
    return major + "." + minor + "." + patch + (isInBeta ? "-beta":"");
  }
}
