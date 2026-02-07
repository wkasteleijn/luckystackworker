package nl.wilcokas.luckystackworker.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.wilcokas.luckystackworker.model.DeRotation;
import nl.wilcokas.luckystackworker.util.LswFileUtil;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeRotationDTO {
    private List<String> images;
    private String referenceImage;
    private String referenceTime;
    private int anchorStrength;

    public DeRotationDTO(DeRotation deRotation) {
        this.images = deRotation.getImages();
        this.anchorStrength = deRotation.getAnchorStrength();
        LocalDateTime localDateTime = deRotation.getReferenceTime();
        if (localDateTime != null) {
            this.referenceTime = LswFileUtil.toWinjuposTimestamp(localDateTime);
        }
    }
}
