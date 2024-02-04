package nl.wilcokas.luckystackworker.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.wilcokas.luckystackworker.dto.ProfileDTO;

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
        this.radius = profile.getRadius();
        this.amount = profile.getAmount();
        this.iterations = profile.getIterations();
        this.level = profile.getLevel();
        this.denoiseAlgorithm1 = profile.getDenoiseAlgorithm1();
        this.denoiseAlgorithm2 = profile.getDenoiseAlgorithm2();
        this.denoise1Amount = profile.getDenoise1Amount();
        this.denoise1Radius = profile.getDenoise1Radius();
        this.denoise1Iterations = profile.getDenoise1Iterations();
        this.denoise2Radius = profile.getDenoise2Radius();
        this.denoise2Iterations = profile.getDenoise2Iterations();
        this.iansAmount = profile.getIansAmount();
        this.iansRecovery = profile.getIansRecovery();
        this.gamma = profile.getGamma();
        this.red = profile.getRed();
        this.green = profile.getGreen();
        this.blue = profile.getBlue();
        this.purple = profile.getPurple();
        this.saturation = profile.getSaturation();
        this.contrast = profile.getContrast();
        this.brightness = profile.getBrightness();
        this.background = profile.getBackground();
        this.savitzkyGolaySize = profile.getSavitzkyGolaySize();
        this.savitzkyGolayAmount = profile.getSavitzkyGolayAmount();
        this.savitzkyGolayIterations = profile.getSavitzkyGolayIterations();
        this.clippingStrength = profile.getClippingStrength();
        this.clippingRange = profile.getClippingRange();
        this.deringRadius = profile.getDeringRadius();
        this.deringStrength = profile.getDeringStrength();
        this.sharpenMode = profile.getSharpenMode();
        this.localContrastMode = profile.getLocalContrastMode();
        this.localContrastFine = profile.getLocalContrastFine();
        this.localContrastMedium = profile.getLocalContrastMedium();
        this.localContrastLarge = profile.getLocalContrastLarge();
        this.dispersionCorrectionEnabled = profile.isDispersionCorrectionEnabled();
        this.dispersionCorrectionRedX = profile.getDispersionCorrectionRedX();
        this.dispersionCorrectionBlueX = profile.getDispersionCorrectionBlueX();
        this.dispersionCorrectionRedY = profile.getDispersionCorrectionRedY();
        this.dispersionCorrectionBlueY = profile.getDispersionCorrectionBlueY();
        this.luminanceIncludeRed = profile.isLuminanceIncludeRed();
        this.luminanceIncludeGreen = profile.isLuminanceIncludeGreen();
        this.luminanceIncludeBlue = profile.isLuminanceIncludeBlue();
        this.luminanceIncludeColor = profile.isLuminanceIncludeColor();
        this.scale = profile.getScale();
        this.threshold = profile.getDeringThreshold();
        this.equalizeLocalHistogramsStrength = profile.getEqualizeLocalHistogramsStrength();
    }

    private int id;
    private String name;
    private BigDecimal radius;
    private BigDecimal amount;
    private int iterations;
    private int level;
    private String denoiseAlgorithm1;
    private String denoiseAlgorithm2;
    private BigDecimal denoise1Amount;
    private BigDecimal denoise1Radius;
    private int denoise1Iterations;
    private BigDecimal iansAmount;
    private BigDecimal iansRecovery;
    private BigDecimal denoise2Radius;
    private int denoise2Iterations;
    private BigDecimal gamma;
    private BigDecimal red;
    private BigDecimal green;
    private BigDecimal blue;
    private BigDecimal purple;
    private BigDecimal saturation;
    private int contrast;
    private int brightness;
    private int background;
    private int savitzkyGolaySize;
    private int savitzkyGolayAmount;
    private int savitzkyGolayIterations;
    private int clippingStrength;
    private int clippingRange;
    private BigDecimal deringRadius;
    private int deringStrength;
    private String sharpenMode;
    private String localContrastMode;
    private int localContrastFine;
    private int localContrastMedium;
    private int localContrastLarge;
    private boolean dispersionCorrectionEnabled;
    private int dispersionCorrectionRedX;
    private int dispersionCorrectionBlueX;
    private int dispersionCorrectionRedY;
    private int dispersionCorrectionBlueY;
    private boolean luminanceIncludeRed;
    private boolean luminanceIncludeGreen;
    private boolean luminanceIncludeBlue;
    private boolean luminanceIncludeColor;
    private double scale;
    private int threshold;
    private int equalizeLocalHistogramsStrength;

    // Not used any longer, needed for historical reasons. Removing this would now
    // break the profile loading of old yaml files created prior to 4.1.0.
    private BigDecimal denoiseSigma; // Unused as of 5.0.0
    private String operation;
    private String rootFolder;
    private boolean isLargeImage;
    private BigDecimal denoise;
    private BigDecimal denoiseRadius;
    private int denoiseIterations;
}
