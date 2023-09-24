package nl.wilcokas.luckystackworker.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.wilcokas.luckystackworker.model.Profile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileDTO {

    public ProfileDTO(Profile profile) {
        this.name=profile.getName();
        this.radius=profile.getRadius();
        this.amount=profile.getAmount();
        this.iterations=profile.getIterations();
        this.level=profile.getLevel();
        this.denoise=profile.getDenoise();
        this.denoiseSigma=profile.getDenoiseSigma();
        this.denoiseRadius=profile.getDenoiseRadius();
        this.denoiseIterations=profile.getDenoiseIterations();
        this.gamma=profile.getGamma();
        this.red=profile.getRed();
        this.green=profile.getGreen();
        this.blue = profile.getBlue();
        this.saturation = profile.getSaturation();
        this.contrast=profile.getContrast();
        this.brightness=profile.getBrightness();
        this.background=profile.getBackground();
        this.savitzkyGolaySize=profile.getSavitzkyGolaySize();
        this.savitzkyGolayAmount=profile.getSavitzkyGolayAmount();
        this.savitzkyGolayIterations=profile.getSavitzkyGolayIterations();
        this.clippingStrength=profile.getClippingStrength();
        this.clippingRange=profile.getClippingRange();
        this.deringRadius=profile.getDeringRadius();
        this.deringStrength=profile.getDeringStrength();
        this.sharpenMode=profile.getSharpenMode();
        this.localContrastMode=profile.getLocalContrastMode();
        this.localContrastFine=profile.getLocalContrastFine();
        this.localContrastMedium=profile.getLocalContrastMedium();
        this.localContrastLarge=profile.getLocalContrastLarge();
        this.dispersionCorrectionEnabled=profile.isDispersionCorrectionEnabled();
        this.dispersionCorrectionRedX=profile.getDispersionCorrectionRedX();
        this.dispersionCorrectionBlueX=profile.getDispersionCorrectionBlueX();
        this.dispersionCorrectionRedY=profile.getDispersionCorrectionRedY();
        this.dispersionCorrectionBlueY=profile.getDispersionCorrectionBlueY();
        this.luminanceIncludeRed=profile.isLuminanceIncludeRed();
        this.luminanceIncludeGreen=profile.isLuminanceIncludeGreen();
        this.luminanceIncludeBlue=profile.isLuminanceIncludeBlue();
        this.luminanceIncludeColor=profile.isLuminanceIncludeColor();
        this.scale = profile.getScale();
    }

    private String name;
    private BigDecimal radius;
    private BigDecimal amount;
    private int iterations;
    private int level;
    private BigDecimal denoise;
    private BigDecimal denoiseSigma;
    private BigDecimal denoiseRadius;
    private int denoiseIterations;
    private BigDecimal gamma;
    private BigDecimal red;
    private BigDecimal green;
    private BigDecimal blue;
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
}
