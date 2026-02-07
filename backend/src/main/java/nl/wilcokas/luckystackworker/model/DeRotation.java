package nl.wilcokas.luckystackworker.model;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.wilcokas.luckystackworker.dto.DeRotationDTO;
import nl.wilcokas.luckystackworker.util.LswFileUtil;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeRotation {
    private List<String> images;
    private String referenceImage;
    private LocalDateTime referenceTime;
    private int anchorStrength;

    public DeRotation(DeRotationDTO deRotation) {
        mapFromDTO(deRotation);
    }

    private void mapFromDTO(final DeRotationDTO deRotation) {
        this.images = deRotation.getImages();
        this.referenceImage = deRotation.getReferenceImage();
        this.referenceTime = deRotation.getReferenceTime() == null
                ? null
                : LswFileUtil.getObjectDateTime(deRotation.getReferenceTime());
        this.anchorStrength = deRotation.getAnchorStrength();
    }
}
