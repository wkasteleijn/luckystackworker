package nl.wilcokas.luckystackworker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@AllArgsConstructor
@Data
public class ResponseDTO {
    private ProfileDTO profile;
    private SettingsDTO settings;
    private PSFImageDto psfImage;
}
