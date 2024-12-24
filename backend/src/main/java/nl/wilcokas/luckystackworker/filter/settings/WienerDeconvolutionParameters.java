package nl.wilcokas.luckystackworker.filter.settings;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class WienerDeconvolutionParameters {

    private LSWSharpenMode mode;
    private boolean includeRed;
    private boolean includeGreen;
    private boolean includeBlue;
    private boolean includeColor;

    // Luminance
    private int iterationsLuminance;
    private float deringStrengthLuminance;
    private double deringRadiusLuminance;
    private int deringThresholdLuminance;
    private float blendRawLuminance;

    // Red
    private int iterationsRed;
    private float deringStrengthRed;
    private double deringRadiusRed;
    private int deringThresholdRed;
    private float blendRawRed;

    // Green
    private int iterationsGreen;
    private float deringStrengthGreen;
    private double deringRadiusGreen;
    private int deringThresholdGreen;
    private float blendRawGreen;

    // Blue
    private int iterationsBlue;
    private float deringStrengthBlue;
    private double deringRadiusBlue;
    private int deringThresholdBlue;
    private float blendRawBlue;
}
