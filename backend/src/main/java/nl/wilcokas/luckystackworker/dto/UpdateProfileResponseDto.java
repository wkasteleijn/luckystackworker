package nl.wilcokas.luckystackworker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileResponseDto {
    private ProfileDTO profile;
    private PSFImageDto psfImage;
}
