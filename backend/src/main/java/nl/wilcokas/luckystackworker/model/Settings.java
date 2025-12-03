package nl.wilcokas.luckystackworker.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settings {
    private int id;
    private String rootFolder;
    private String extensions;
    private String outputFormat;
    private String defaultProfile;
    private String latestKnownVersion;
    private LocalDateTime latestKnownVersionChecked;
}
