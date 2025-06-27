package nl.wilcokas.luckystackworker.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.wilcokas.luckystackworker.dto.PSFDTO;
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
    private int blendRaw;
    private int wienerIterations;

    // sharpen green
    private BigDecimal radiusGreen;
    private BigDecimal amountGreen;
    private int iterationsGreen;
    private int levelGreen;
    private int clippingStrengthGreen;
    private int clippingRangeGreen;
    private BigDecimal deringRadiusGreen;
    private int deringStrengthGreen;
    private int blendRawGreen;
    private int wienerIterationsGreen;

    // sharpen blue
    private BigDecimal radiusBlue;
    private BigDecimal amountBlue;
    private int iterationsBlue;
    private int levelBlue;
    private int clippingStrengthBlue;
    private int clippingRangeBlue;
    private BigDecimal deringRadiusBlue;
    private int deringStrengthBlue;
    private int blendRawBlue;
    private int wienerIterationsBlue;

    // general sharpen settings
    private String sharpenMode;
    private boolean luminanceIncludeRed;
    private boolean luminanceIncludeGreen;
    private boolean luminanceIncludeBlue;
    private boolean luminanceIncludeColor;
    private Boolean applyUnsharpMask;
    private Boolean applyWienerDeconvolution;

    // denoise (red and all channels)
    private String denoiseAlgorithm1;
    private BigDecimal denoise1Amount;
    private BigDecimal denoise1Radius;
    private int denoise1Iterations;
    private BigDecimal iansAmount;
    private BigDecimal iansAmountMid;
    private BigDecimal iansRecovery;
    private int iansIterations;
    private int rofTheta;
    private int rofIterations;
    private int bilateralSigmaColor;
    private int bilateralSigmaSpace;
    private int bilateralRadius;
    private int bilateralIterations;

    private String denoiseAlgorithm2;
    private BigDecimal denoise2Radius;
    private int denoise2Iterations;
    private int savitzkyGolaySize;
    private int savitzkyGolayAmount;
    private int savitzkyGolayIterations;

    // denoise green
    private BigDecimal denoise1AmountGreen;
    private BigDecimal denoise1RadiusGreen;
    private int denoise1IterationsGreen;
    private BigDecimal denoise2RadiusGreen;
    private int denoise2IterationsGreen;
    private int savitzkyGolaySizeGreen;
    private int savitzkyGolayAmountGreen;
    private int savitzkyGolayIterationsGreen;
    private int rofThetaGreen;
    private int rofIterationsGreen;
    private int bilateralSigmaColorGreen;
    private int bilateralSigmaSpaceGreen;
    private int bilateralRadiusGreen;
    private int bilateralIterationsGreen;

    // denoise blue
    private BigDecimal denoise1AmountBlue;
    private BigDecimal denoise1RadiusBlue;
    private int denoise1IterationsBlue;
    private BigDecimal denoise2RadiusBlue;
    private int denoise2IterationsBlue;
    private int savitzkyGolaySizeBlue;
    private int savitzkyGolayAmountBlue;
    private int savitzkyGolayIterationsBlue;
    private int rofThetaBlue;
    private int rofIterationsBlue;
    private int bilateralSigmaColorBlue;
    private int bilateralSigmaSpaceBlue;
    private int bilateralRadiusBlue;
    private int bilateralIterationsBlue;

    // constrast & light
    private BigDecimal gamma;
    private int contrast;
    private int brightness;
    private int lightness;
    private int background;
    private String localContrastMode;
    private int localContrastFine;
    private int localContrastMedium;
    private int localContrastLarge;
    private int equalizeLocalHistogramsStrength;
    private boolean preserveDarkBackground;
    private double clippingSuppression;

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
    private boolean normalizeColorBalance;

    private double scale;
    private double rotationAngle;
    private OpenImageModeEnum openImageMode;

    private PSF psf;

    // Not used any longer, needed for historical reasons. Removing this would now
    // break the profile loading of old yaml files.
    private int deringThreshold;
    private int deringThresholdGreen;
    private int deringThresholdBlue;

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

    private void mapFromDTO(final ProfileDTO profile) {
        this.name = profile.getName();

        // sharpen
        this.radius = profile.getRadius();
        this.amount = profile.getAmount();
        this.iterations = profile.getIterations();
        this.level = profile.getLevel();
        this.clippingStrength = profile.getClippingStrength();
        this.clippingRange = profile.getClippingRange();
        this.deringRadius = profile.getDeringRadius();
        this.deringStrength = profile.getDeringStrength();
        this.blendRaw = profile.getBlendRaw();
        this.wienerIterations = profile.getWienerIterations();

        if (profile.getApplySharpenToChannel() == ChannelEnum.RGB) {
            this.radiusGreen = profile.getRadius();
            this.amountGreen = profile.getAmount();
            this.iterationsGreen = profile.getIterations();
            this.levelGreen = profile.getLevel();
            this.clippingStrengthGreen = profile.getClippingStrength();
            this.clippingRangeGreen = profile.getClippingRange();
            this.deringRadiusGreen = profile.getDeringRadius();
            this.deringStrengthGreen = profile.getDeringStrength();
            this.blendRawGreen = profile.getBlendRaw();
            this.wienerIterationsGreen = profile.getWienerIterations();

            this.radiusBlue = profile.getRadius();
            this.amountBlue = profile.getAmount();
            this.iterationsBlue = profile.getIterations();
            this.levelBlue = profile.getLevel();
            this.clippingStrengthBlue = profile.getClippingStrength();
            this.clippingRangeBlue = profile.getClippingRange();
            this.deringRadiusBlue = profile.getDeringRadius();
            this.deringStrengthBlue = profile.getDeringStrength();
            this.blendRawBlue = profile.getBlendRaw();
            this.wienerIterationsBlue = profile.getWienerIterations();

        } else {
            this.radiusGreen = profile.getRadiusGreen();
            this.amountGreen = profile.getAmountGreen();
            this.iterationsGreen = profile.getIterationsGreen();
            this.levelGreen = profile.getLevelGreen();
            this.clippingStrengthGreen = profile.getClippingStrengthGreen();
            this.clippingRangeGreen = profile.getClippingRangeGreen();
            this.deringRadiusGreen = profile.getDeringRadiusGreen();
            this.deringStrengthGreen = profile.getDeringStrengthGreen();
            this.blendRawGreen = profile.getBlendRawGreen();
            this.wienerIterationsGreen = profile.getWienerIterationsGreen();

            this.radiusBlue = profile.getRadiusBlue();
            this.amountBlue = profile.getAmountBlue();
            this.iterationsBlue = profile.getIterationsBlue();
            this.levelBlue = profile.getLevelBlue();
            this.clippingStrengthBlue = profile.getClippingStrengthBlue();
            this.clippingRangeBlue = profile.getClippingRangeBlue();
            this.deringRadiusBlue = profile.getDeringRadiusBlue();
            this.deringStrengthBlue = profile.getDeringStrengthBlue();
            this.blendRawBlue = profile.getBlendRawBlue();
            this.wienerIterationsBlue = profile.getWienerIterationsBlue();
        }

        this.sharpenMode = profile.getSharpenMode();
        this.luminanceIncludeRed = profile.isLuminanceIncludeRed();
        this.luminanceIncludeGreen = profile.isLuminanceIncludeGreen();
        this.luminanceIncludeBlue = profile.isLuminanceIncludeBlue();
        this.luminanceIncludeColor = profile.isLuminanceIncludeColor();
        this.applyUnsharpMask = profile.isApplyUnsharpMask();
        this.applyWienerDeconvolution = profile.isApplyWienerDeconvolution();

        // denoise
        this.denoiseAlgorithm1 = profile.getDenoiseAlgorithm1();
        this.denoiseAlgorithm2 = profile.getDenoiseAlgorithm2();
        this.denoise1Amount = profile.getDenoise1Amount();
        this.denoise1Radius = profile.getDenoise1Radius();
        this.denoise1Iterations = profile.getDenoise1Iterations();
        this.denoise2Radius = profile.getDenoise2Radius();
        this.denoise2Iterations = profile.getDenoise2Iterations();
        this.iansAmount = profile.getIansAmount();
        this.iansAmountMid = profile.getIansAmountMid();
        this.iansIterations = profile.getIansIterations();
        this.iansRecovery = profile.getIansRecovery();
        this.rofTheta = profile.getRofTheta();
        this.rofIterations = profile.getRofIterations();
        this.savitzkyGolaySize = profile.getSavitzkyGolaySize();
        this.savitzkyGolayAmount = profile.getSavitzkyGolayAmount();
        this.savitzkyGolayIterations = profile.getSavitzkyGolayIterations();
        this.bilateralRadius = profile.getBilateralRadius();
        this.bilateralSigmaColor = profile.getBilateralSigmaColor();
        this.bilateralSigmaSpace = profile.getBilateralSigmaSpace();
        this.bilateralIterations = profile.getBilateralIterations();

        if (profile.getApplyDenoiseToChannel() == ChannelEnum.RGB) {
            this.denoise1AmountGreen = profile.getDenoise1Amount();
            this.denoise1RadiusGreen = profile.getDenoise1Radius();
            this.denoise1IterationsGreen = profile.getDenoise1Iterations();
            this.denoise2RadiusGreen = profile.getDenoise2Radius();
            this.denoise2IterationsGreen = profile.getDenoise2Iterations();
            this.savitzkyGolaySizeGreen = profile.getSavitzkyGolaySize();
            this.savitzkyGolayAmountGreen = profile.getSavitzkyGolayAmount();
            this.savitzkyGolayIterationsGreen = profile.getSavitzkyGolayIterations();
            this.rofThetaGreen = profile.getRofTheta();
            this.rofIterationsGreen = profile.getRofIterations();
            this.bilateralRadiusGreen = profile.getBilateralRadius();
            this.bilateralSigmaColorGreen = profile.getBilateralSigmaColor();
            this.bilateralSigmaSpaceGreen = profile.getBilateralSigmaSpace();
            this.bilateralIterationsGreen = profile.getBilateralIterations();

            this.denoise1AmountBlue = profile.getDenoise1Amount();
            this.denoise1RadiusBlue = profile.getDenoise1Radius();
            this.denoise1IterationsBlue = profile.getDenoise1Iterations();
            this.denoise2RadiusBlue = profile.getDenoise2Radius();
            this.denoise2IterationsBlue = profile.getDenoise2Iterations();
            this.savitzkyGolaySizeBlue = profile.getSavitzkyGolaySize();
            this.savitzkyGolayAmountBlue = profile.getSavitzkyGolayAmount();
            this.savitzkyGolayIterationsBlue = profile.getSavitzkyGolayIterations();
            this.rofThetaBlue = profile.getRofTheta();
            this.rofIterationsBlue = profile.getRofIterations();
            this.bilateralRadiusBlue = profile.getBilateralRadius();
            this.bilateralSigmaColorBlue = profile.getBilateralSigmaColor();
            this.bilateralSigmaSpace = profile.getBilateralSigmaSpace();
            this.bilateralIterationsBlue = profile.getBilateralIterations();

        } else {
            this.denoise1AmountGreen = profile.getDenoise1AmountGreen();
            this.denoise1RadiusGreen = profile.getDenoise1RadiusGreen();
            this.denoise1IterationsGreen = profile.getDenoise1IterationsGreen();
            this.denoise2RadiusGreen = profile.getDenoise2RadiusGreen();
            this.denoise2IterationsGreen = profile.getDenoise2IterationsGreen();
            this.savitzkyGolaySizeGreen = profile.getSavitzkyGolaySizeGreen();
            this.savitzkyGolayAmountGreen = profile.getSavitzkyGolayAmountGreen();
            this.savitzkyGolayIterationsGreen = profile.getSavitzkyGolayIterationsGreen();
            this.rofThetaGreen = profile.getRofThetaGreen();
            this.rofIterationsGreen = profile.getRofIterationsGreen();
            this.bilateralRadiusGreen = profile.getBilateralRadiusGreen();
            this.bilateralSigmaColorGreen = profile.getBilateralSigmaColorGreen();
            this.bilateralSigmaSpaceGreen = profile.getBilateralSigmaSpaceGreen();
            this.bilateralIterationsGreen = profile.getBilateralIterationsGreen();

            this.denoise1AmountBlue = profile.getDenoise1AmountBlue();
            this.denoise1RadiusBlue = profile.getDenoise1RadiusBlue();
            this.denoise1IterationsBlue = profile.getDenoise1IterationsBlue();
            this.denoise2RadiusBlue = profile.getDenoise2RadiusBlue();
            this.denoise2IterationsBlue = profile.getDenoise2IterationsBlue();
            this.savitzkyGolaySizeBlue = profile.getSavitzkyGolaySizeBlue();
            this.savitzkyGolayAmountBlue = profile.getSavitzkyGolayAmountBlue();
            this.savitzkyGolayIterationsBlue = profile.getSavitzkyGolayIterationsBlue();
            this.rofThetaBlue = profile.getRofThetaBlue();
            this.rofIterationsBlue = profile.getRofIterationsBlue();
            this.bilateralRadiusBlue = profile.getBilateralRadiusBlue();
            this.bilateralSigmaColorBlue = profile.getBilateralSigmaColorBlue();
            this.bilateralSigmaSpaceBlue = profile.getBilateralSigmaSpaceBlue();
            this.bilateralIterationsBlue = profile.getBilateralIterationsBlue();

        }

        // contrast & light
        this.gamma = profile.getGamma();
        this.contrast = profile.getContrast();
        this.brightness = profile.getBrightness();
        this.lightness = profile.getLightness();
        this.background = profile.getBackground();
        this.localContrastMode = profile.getLocalContrastMode();
        this.localContrastFine = profile.getLocalContrastFine();
        this.localContrastMedium = profile.getLocalContrastMedium();
        this.localContrastLarge = profile.getLocalContrastLarge();
        this.equalizeLocalHistogramsStrength = profile.getEqualizeLocalHistogramsStrength();
        this.preserveDarkBackground = profile.isPreserveDarkBackground();
        this.clippingSuppression = profile.getClippingSuppression();

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
        this.normalizeColorBalance = profile.isNormalizeColorBalance();

        this.scale = profile.getScale();
        this.rotationAngle = profile.getRotationAngle();
        this.openImageMode = profile.getOpenImageMode();
        PSFDTO psfDTO = profile.getPsf();
        this.psf = PSF.builder()
                .airyDiskRadius(psfDTO.getAiryDiskRadius())
                .diffractionIntensity(psfDTO.getDiffractionIntensity())
                .seeingIndex(psfDTO.getSeeingIndex())
                .type(psfDTO.getType())
                .build();
    }

}
