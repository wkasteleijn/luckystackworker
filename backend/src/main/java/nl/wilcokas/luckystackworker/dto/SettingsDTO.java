package nl.wilcokas.luckystackworker.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import nl.wilcokas.luckystackworker.model.Settings;

@Data
@AllArgsConstructor
@Builder
public class SettingsDTO {

    public SettingsDTO(Settings settings) {
        this.rootFolder = settings.getRootFolder();
        this.extensions = settings.getExtensions();
        this.outputFormat = settings.getOutputFormat();
        this.defaultProfile = settings.getDefaultProfile();
        this.latestKnownVersion = settings.getLatestKnownVersion();
        this.latestKnownVersionChecked = settings.getLatestKnownVersionChecked();
    }

    private String rootFolder;
    private String extensions;
    private String outputFormat;
    private String defaultProfile;
    private String latestKnownVersion;
    private LocalDateTime latestKnownVersionChecked;
    private String operation;
    private boolean isLargeImage;
}
