package nl.wilcokas.luckystackworker.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.wilcokas.luckystackworker.dto.DeRotationDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeRotation {
    private List<String> images;
    private String referenceImage;
    private int noiseRobustness;
    private int anchorStrength;
    private int accurateness;
    private String rootFolder;

    public DeRotation(DeRotationDTO deRotation) {
        mapFromDTO(deRotation);
    }

    private void mapFromDTO(final DeRotationDTO deRotation) {
        this.images = deRotation.getImages();
        this.referenceImage = deRotation.getReferenceImage();
        this.noiseRobustness = deRotation.getNoiseRobustness();
        this.anchorStrength = deRotation.getAnchorStrength();
        this.accurateness = deRotation.getAccurateness();
    }
}
