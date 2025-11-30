package nl.wilcokas.luckystackworker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import nl.wilcokas.luckystackworker.model.DeRotation;

import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class DeRotationDTO {
    private List<String> images;
    private String referenceImage;
    private int noiseRobustness;
    private int anchorStrength;
    private int accurateness;

    public DeRotationDTO (DeRotation deRotation) {
        this.images = deRotation.getImages();
    }
}
