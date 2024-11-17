package nl.wilcokas.luckystackworker.filter.settings;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UnsharpMaskParameters {

    // luminance
    private double radiusLuminance;
    private float amountLuminance;
    private int iterationsLuminance;
    private float clippingStrengthLuminance;
    private float clippingRangeLuminance;
    private double deringRadiusLuminance;
    private float deringStrengthLuminance;
    private int deringThresholdLuminance;
    private float blendRawLuminance;

    // red
    private double radiusRed;
    private float amountRed;
    private int iterationsRed;
    private float clippingStrengthRed;
    private float clippingRangeRed;
    private double deringRadiusRed;
    private float deringStrengthRed;
    private int deringThresholdRed;
    private float blendRawRed;

    // green
    private double radiusGreen;
    private float amountGreen;
    private int iterationsGreen;
    private float clippingStrengthGreen;
    private float clippingRangeGreen;
    private double deringRadiusGreen;
    private float deringStrengthGreen;
    private int deringThresholdGreen;
    private float blendRawGreen;

    // blue
    private double radiusBlue;
    private float amountBlue;
    private int iterationsBlue;
    private float clippingStrengthBlue;
    private float clippingRangeBlue;
    private double deringRadiusBlue;
    private float deringStrengthBlue;
    private int deringThresholdBlue;
    private float blendRawBlue;
}
