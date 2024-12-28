package nl.wilcokas.luckystackworker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PSF {
    private double airyDiskRadius;
    private int seeingIndex;
    private double diffractionIntensity;
    private int wavelength;
    private String customPSF;
}
