package nl.wilcokas.luckystackworker.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.wilcokas.luckystackworker.dto.ProfileDTO;
import nl.wilcokas.luckystackworker.service.dto.OpenImageModeEnum;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {

    public Profile(ProfileDTO profile) {
        mapFromDTO(profile);
    }

    public void mapFromDTO(final ProfileDTO profile) {
        this.name = profile.getName();


        // sharpen
        this.applySharpenToChannel = profile.getApplySharpenToChannel();
        if (applySharpenToChannel == ChannelEnum.RGB || applySharpenToChannel == ChannelEnum.R) {
            this.radius = profile.getRadius();
            this.amount = profile.getAmount();
            this.iterations = profile.getIterations();
            this.level = profile.getLevel();
            this.clippingStrength = profile.getClippingStrength();
            this.clippingRange = profile.getClippingRange();
            this.deringRadius = profile.getDeringRadius();
            this.deringStrength = profile.getDeringStrength();
            this.deringThreshold = profile.getDeringThreshold();
        }

        if (applySharpenToChannel == ChannelEnum.RGB || applySharpenToChannel == ChannelEnum.G) {
            this.radiusGreen = profile.getRadius();
            this.amountGreen = profile.getAmount();
            this.iterationsGreen = profile.getIterations();
            this.levelGreen = profile.getLevel();
            this.clippingStrengthGreen = profile.getClippingStrength();
            this.clippingRangeGreen = profile.getClippingRange();
            this.deringRadiusGreen = profile.getDeringRadius();
            this.deringStrengthGreen = profile.getDeringStrength();
            this.deringThresholdGreen = profile.getDeringThreshold();
        }

        if (applySharpenToChannel == ChannelEnum.RGB || applySharpenToChannel == ChannelEnum.B) {
            this.radiusBlue = profile.getRadius();
            this.amountBlue = profile.getAmount();
            this.iterationsBlue = profile.getIterations();
            this.levelBlue = profile.getLevel();
            this.clippingStrengthBlue = profile.getClippingStrength();
            this.clippingRangeBlue = profile.getClippingRange();
            this.deringRadiusBlue = profile.getDeringRadius();
            this.deringStrengthBlue = profile.getDeringStrength();
            this.deringThresholdBlue = profile.getDeringThreshold();
        }

        this.sharpenMode = profile.getSharpenMode();
        this.luminanceIncludeRed = profile.isLuminanceIncludeRed();
        this.luminanceIncludeGreen = profile.isLuminanceIncludeGreen();
        this.luminanceIncludeBlue = profile.isLuminanceIncludeBlue();
        this.luminanceIncludeColor = profile.isLuminanceIncludeColor();

        // denoise
        this.denoiseAlgorithm1 = profile.getDenoiseAlgorithm1();
        this.denoiseAlgorithm2 = profile.getDenoiseAlgorithm2();
        this.denoise1Amount = profile.getDenoise1Amount();
        this.denoise1Radius = profile.getDenoise1Radius();
        this.denoise1Iterations = profile.getDenoise1Iterations();
        this.denoise2Radius = profile.getDenoise2Radius();
        this.denoise2Iterations = profile.getDenoise2Iterations();
        this.iansAmount = profile.getIansAmount();
        this.iansRecovery = profile.getIansRecovery();
        this.savitzkyGolaySize = profile.getSavitzkyGolaySize();
        this.savitzkyGolayAmount = profile.getSavitzkyGolayAmount();
        this.savitzkyGolayIterations = profile.getSavitzkyGolayIterations();

        // contrast & light
        this.gamma = profile.getGamma();
        this.contrast = profile.getContrast();
        this.brightness = profile.getBrightness();
        this.background = profile.getBackground();
        this.localContrastMode = profile.getLocalContrastMode();
        this.localContrastFine = profile.getLocalContrastFine();
        this.localContrastMedium = profile.getLocalContrastMedium();
        this.localContrastLarge = profile.getLocalContrastLarge();
        this.equalizeLocalHistogramsStrength = profile.getEqualizeLocalHistogramsStrength();

        // color & dispersion
        this.red = profile.getRed();
        this.green = profile.getGreen();
        this.blue = profile.getBlue();
        this.purple = profile.getPurple();
        this.saturation = profile.getSaturation();
        this.dispersionCorrectionEnabled = profile.isDispersionCorrectionEnabled();
        this.dispersionCorrectionRedX = profile.getDispersionCorrectionRedX();
        this.dispersionCorrectionBlueX = profile.getDispersionCorrectionBlueX();
        this.dispersionCorrectionRedY = profile.getDispersionCorrectionRedY();
        this.dispersionCorrectionBlueY = profile.getDispersionCorrectionBlueY();

        this.scale = profile.getScale();
        this.openImageMode = profile.getOpenImageMode();
    }

    private int id;
    private String name;

    // sharpen (red or all channels)
    private BigDecimal radius;
    private BigDecimal amount;
    private int iterations;
    private int level;
    private int clippingStrength;
    private int clippingRange;
    private BigDecimal deringRadius;
    private int deringStrength;
    private int deringThreshold;

    // general sharpen settings
    private String sharpenMode;
    private boolean luminanceIncludeRed;
    private boolean luminanceIncludeGreen;
    private boolean luminanceIncludeBlue;
    private boolean luminanceIncludeColor;
    private ChannelEnum applySharpenToChannel;

    // sharpen green
    private BigDecimal radiusGreen;
    private BigDecimal amountGreen;
    private int iterationsGreen;
    private int levelGreen;
    private int clippingStrengthGreen;
    private int clippingRangeGreen;
    private BigDecimal deringRadiusGreen;
    private int deringStrengthGreen;
    private int deringThresholdGreen;

    // sharpen blue
    private BigDecimal radiusBlue;
    private BigDecimal amountBlue;
    private int iterationsBlue;
    private int levelBlue;
    private int clippingStrengthBlue;
    private int clippingRangeBlue;
    private BigDecimal deringRadiusBlue;
    private int deringStrengthBlue;
    private int deringThresholdBlue;

    // denoise (red and all channels)
    private String denoiseAlgorithm1;
    private String denoiseAlgorithm2;
    private BigDecimal denoise1Amount;
    private BigDecimal denoise1Radius;
    private int denoise1Iterations;
    private BigDecimal iansAmount;
    private BigDecimal iansRecovery;
    private BigDecimal denoise2Radius;
    private int denoise2Iterations;
    private int savitzkyGolaySize;
    private int savitzkyGolayAmount;
    private int savitzkyGolayIterations;
    private ChannelEnum applyDenoiseToChannel;

    // denoise green
    private String denoiseAlgorithm1Green;
    private String denoiseAlgorithm2Green;
    private BigDecimal denoise1AmountGreen;
    private BigDecimal denoise1RadiusGreen;
    private int denoise1IterationsGreen;
    private BigDecimal iansAmountGreen;
    private BigDecimal iansRecoveryGreen;
    private BigDecimal denoise2RadiusGreen;
    private int denoise2IterationsGreen;
    private int savitzkyGolaySizeGreen;
    private int savitzkyGolayAmountGreen;
    private int savitzkyGolayIterationsGreen;

    // denoise blue
    private String denoiseAlgorithm1Blue;
    private String denoiseAlgorithm2Blue;
    private BigDecimal denoise1AmountBlue;
    private BigDecimal denoise1RadiusBlue;
    private int denoise1IterationsBlue;
    private BigDecimal iansAmountBlue;
    private BigDecimal iansRecoveryBlue;
    private BigDecimal denoise2RadiusBlue;
    private int denoise2IterationsBlue;
    private int savitzkyGolaySizeBlue;
    private int savitzkyGolayAmountBlue;
    private int savitzkyGolayIterationsBlue;

    // constrast & light
    private BigDecimal gamma;
    private int contrast;
    private int brightness;
    private int background;
    private String localContrastMode;
    private int localContrastFine;
    private int localContrastMedium;
    private int localContrastLarge;
    private int equalizeLocalHistogramsStrength;

    // color & dispersion
    private BigDecimal red;
    private BigDecimal green;
    private BigDecimal blue;
    private BigDecimal purple;
    private BigDecimal saturation;
    private boolean dispersionCorrectionEnabled;
    private int dispersionCorrectionRedX;
    private int dispersionCorrectionBlueX;
    private int dispersionCorrectionRedY;
    private int dispersionCorrectionBlueY;

    private double scale;
    private OpenImageModeEnum openImageMode;

    // Not used any longer, needed for historical reasons. Removing this would now
    // break the profile loading of old yaml files created prior to 4.1.0.

    // Unused as of 5.2.0
    private int threshold;

    // Unused as of 5.0.0
    private BigDecimal denoiseSigma;

    // Unused as of 4.1.0
    private String operation;
    private String rootFolder;
    private boolean isLargeImage;
    private BigDecimal denoise;
    private BigDecimal denoiseRadius;
    private int denoiseIterations;
}
