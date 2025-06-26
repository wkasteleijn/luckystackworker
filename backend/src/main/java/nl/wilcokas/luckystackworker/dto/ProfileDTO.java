package nl.wilcokas.luckystackworker.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.wilcokas.luckystackworker.model.ChannelEnum;
import nl.wilcokas.luckystackworker.model.PSF;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.service.dto.OpenImageModeEnum;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileDTO {

    public ProfileDTO(Profile profile) {
        this.name = profile.getName();

        // sharpen
        this.applySharpenToChannel = ChannelEnum.RGB;
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

        this.sharpenMode = profile.getSharpenMode();
        this.luminanceIncludeRed = profile.isLuminanceIncludeRed();
        this.luminanceIncludeGreen = profile.isLuminanceIncludeGreen();
        this.luminanceIncludeBlue = profile.isLuminanceIncludeBlue();
        this.luminanceIncludeColor = profile.isLuminanceIncludeColor();
        this.applyWienerDeconvolution = profile.getApplyWienerDeconvolution();
        this.applyUnsharpMask = profile.getApplyUnsharpMask();

        // denoise
        this.denoiseAlgorithm1 = profile.getDenoiseAlgorithm1();
        this.denoise1Amount = profile.getDenoise1Amount();
        this.denoise1Radius = profile.getDenoise1Radius();
        this.denoise1Iterations = profile.getDenoise1Iterations();
        this.iansAmount = profile.getIansAmount();
        this.iansAmountMid = profile.getIansAmountMid();
        this.iansRecovery = profile.getIansRecovery();
        this.iansIterations = profile.getIansIterations();
        this.rofTheta = profile.getRofTheta();
        this.rofIterations = profile.getRofIterations();
        this.denoiseAlgorithm2 = profile.getDenoiseAlgorithm2();
        this.denoise2Radius = profile.getDenoise2Radius();
        this.denoise2Iterations = profile.getDenoise2Iterations();
        this.savitzkyGolaySize = profile.getSavitzkyGolaySize();
        this.savitzkyGolayAmount = profile.getSavitzkyGolayAmount();
        this.savitzkyGolayIterations = profile.getSavitzkyGolayIterations();
        this.bilateralIterations = profile.getBilateralIterations();
        this.bilateralRadius = profile.getBilateralRadius();
        this.bilateralSigmaColor = profile.getBilateralSigmaColor();
        this.bilateralSigmaSpace = profile.getBilateralSigmaSpace();

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
        this.bilateralIterationsGreen = profile.getBilateralIterationsGreen();
        this.bilateralRadiusGreen = profile.getBilateralRadiusGreen();
        this.bilateralSigmaColorGreen = profile.getBilateralSigmaColorGreen();
        this.bilateralSigmaSpaceGreen = profile.getBilateralSigmaSpaceGreen();

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
        this.bilateralIterationsBlue = profile.getBilateralIterationsBlue();
        this.bilateralRadiusBlue = profile.getBilateralRadiusBlue();
        this.bilateralSigmaColorBlue = profile.getBilateralSigmaColorBlue();
        this.bilateralSigmaSpaceBlue = profile.getBilateralSigmaSpaceBlue();

        // color & dispersion
        this.red = profile.getRed();
        this.green = profile.getGreen();
        this.blue = profile.getBlue();
        this.purple = profile.getPurple();
        this.saturation = profile.getSaturation();
        this.normalizeColorBalance = profile.isNormalizeColorBalance();

        // constrast & light
        this.gamma = profile.getGamma();
        this.contrast = profile.getContrast();
        this.brightness = profile.getBrightness();
        this.lightness = profile.getLightness();
        this.background = profile.getBackground();
        this.localContrastMode = profile.getLocalContrastMode();
        this.localContrastFine = profile.getLocalContrastFine();
        this.localContrastMedium = profile.getLocalContrastMedium();
        this.localContrastLarge = profile.getLocalContrastLarge();
        this.dispersionCorrectionEnabled = profile.isDispersionCorrectionEnabled();
        this.dispersionCorrectionRedX = profile.getDispersionCorrectionRedX();
        this.dispersionCorrectionBlueX = profile.getDispersionCorrectionBlueX();
        this.dispersionCorrectionRedY = profile.getDispersionCorrectionRedY();
        this.dispersionCorrectionBlueY = profile.getDispersionCorrectionBlueY();
        this.preserveDarkBackground = profile.isPreserveDarkBackground();
        this.gain = profile.getGain();

        this.scale = profile.getScale();
        this.rotationAngle = profile.getRotationAngle();
        this.equalizeLocalHistogramsStrength = profile.getEqualizeLocalHistogramsStrength();
        this.openImageMode = profile.getOpenImageMode();
        PSF profilePsf = profile.getPsf();
        this.psf = PSFDTO.builder()
                .airyDiskRadius(profilePsf.getAiryDiskRadius())
                .seeingIndex(profilePsf.getSeeingIndex())
                .diffractionIntensity(profilePsf.getDiffractionIntensity())
                .type(profilePsf.getType())
                .build();
    }

    private String name;

    // sharpen
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
    private double gain;

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

    private String sharpenMode;
    private ChannelEnum applySharpenToChannel;
    private boolean applyUnsharpMask;
    private boolean applyWienerDeconvolution;

    // denoise
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
    private String denoiseAlgorithm2;
    private int savitzkyGolaySize;
    private int savitzkyGolayAmount;
    private int savitzkyGolayIterations;
    private BigDecimal denoise2Radius;
    private int denoise2Iterations;
    private ChannelEnum applyDenoiseToChannel;
    private int bilateralSigmaColor;
    private int bilateralSigmaSpace;
    private int bilateralRadius;
    private int bilateralIterations;


    private BigDecimal denoise1AmountGreen;
    private BigDecimal denoise1RadiusGreen;
    private int denoise1IterationsGreen;
    private BigDecimal iansAmountGreen;
    private BigDecimal iansRecoveryGreen;
    private int rofThetaGreen;
    private int rofIterationsGreen;
    private int savitzkyGolaySizeGreen;
    private int savitzkyGolayAmountGreen;
    private int savitzkyGolayIterationsGreen;
    private BigDecimal denoise2RadiusGreen;
    private int denoise2IterationsGreen;
    private int bilateralSigmaColorGreen;
    private int bilateralSigmaSpaceGreen;
    private int bilateralRadiusGreen;
    private int bilateralIterationsGreen;

    private BigDecimal denoise1AmountBlue;
    private BigDecimal denoise1RadiusBlue;
    private int denoise1IterationsBlue;
    private BigDecimal iansAmountBlue;
    private BigDecimal iansRecoveryBlue;
    private int rofThetaBlue;
    private int rofIterationsBlue;
    private int savitzkyGolaySizeBlue;
    private int savitzkyGolayAmountBlue;
    private int savitzkyGolayIterationsBlue;
    private BigDecimal denoise2RadiusBlue;
    private int denoise2IterationsBlue;
    private int bilateralSigmaColorBlue;
    private int bilateralSigmaSpaceBlue;
    private int bilateralRadiusBlue;
    private int bilateralIterationsBlue;

    // light & contrast
    private BigDecimal gamma;
    private int contrast;
    private int brightness;
    private int lightness;
    private int background;
    private String localContrastMode;
    private int localContrastFine;
    private int localContrastMedium;
    private int localContrastLarge;
    private boolean preserveDarkBackground;

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
    private boolean luminanceIncludeRed;
    private boolean luminanceIncludeGreen;
    private boolean luminanceIncludeBlue;
    private boolean luminanceIncludeColor;
    private boolean normalizeColorBalance;

    private double scale;
    private double rotationAngle;
    private int equalizeLocalHistogramsStrength;
    private OpenImageModeEnum openImageMode;
    private PSFDTO psf;
}
