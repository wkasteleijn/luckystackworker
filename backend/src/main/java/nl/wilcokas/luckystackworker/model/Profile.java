package nl.wilcokas.luckystackworker.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.wilcokas.luckystackworker.dto.ProfileDTO;

@Data
@Entity
@Table(name = "profiles")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {

    public Profile(ProfileDTO profile) {
        this.name = profile.getName();
        this.radius = profile.getRadius();
        this.amount = profile.getAmount();
        this.iterations = profile.getIterations();
        this.level = profile.getLevel();
        this.denoise = profile.getDenoise();
        this.denoiseSigma = profile.getDenoiseSigma();
        this.denoiseRadius = profile.getDenoiseRadius();
        this.denoiseIterations = profile.getDenoiseIterations();
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
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "name")
    private String name;

    @Column(name = "radius")
    private BigDecimal radius;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "iterations")
    private int iterations;

    @Column(name = "level")
    private int level;

    @Column(name = "denoise")
    private BigDecimal denoise;

    @Column(name = "denoise_sigma")
    private BigDecimal denoiseSigma;

    @Column(name = "denoise_radius")
    private BigDecimal denoiseRadius;

    @Column(name = "denoise_iterations")
    private int denoiseIterations;

    @Column(name = "gamma")
    private BigDecimal gamma;

    @Column(name = "red")
    private BigDecimal red;

    @Column(name = "green")
    private BigDecimal green;

    @Column(name = "blue")
    private BigDecimal blue;

    @Column(name = "purple")
    private BigDecimal purple;

    @Column(name = "saturation")
    private BigDecimal saturation;

    @Column(name = "contrast")
    private int contrast;

    @Column(name = "brightness")
    private int brightness;

    @Column(name = "background")
    private int background;

    @Column(name = "savitzkyGolaySize")
    private int savitzkyGolaySize;

    @Column(name = "savitzkyGolayAmount")
    private int savitzkyGolayAmount;

    @Column(name = "savitzkyGolayIterations")
    private int savitzkyGolayIterations;

    @Column(name = "clippingStrength")
    private int clippingStrength;

    @Column(name = "clippingRange")
    private int clippingRange;

    @Column(name = "deringRadius")
    private BigDecimal deringRadius;

    @Column(name = "deringStrength")
    private int deringStrength;

    @Column(name = "sharpenMode")
    private String sharpenMode;

    @Column(name = "localContrastMode")
    private String localContrastMode;

    @Column(name = "localContrastFine")
    private int localContrastFine;

    @Column(name = "localContrastMedium")
    private int localContrastMedium;

    @Column(name = "localContrastLarge")
    private int localContrastLarge;

    @Column(name = "dispersionCorrectionEnabled")
    private boolean dispersionCorrectionEnabled;

    @Column(name = "dispersionCorrectionRedX")
    private int dispersionCorrectionRedX;

    @Column(name = "dispersionCorrectionBlueX")
    private int dispersionCorrectionBlueX;

    @Column(name = "dispersionCorrectionRedY")
    private int dispersionCorrectionRedY;

    @Column(name = "dispersionCorrectionBlueY")
    private int dispersionCorrectionBlueY;

    @Column(name = "luminanceIncludeRed")
    private boolean luminanceIncludeRed;

    @Column(name = "luminanceIncludeGreen")
    private boolean luminanceIncludeGreen;

    @Column(name = "luminanceIncludeBlue")
    private boolean luminanceIncludeBlue;

    @Column(name = "luminanceIncludeColor")
    private boolean luminanceIncludeColor;

    @Column(name = "scale")
    private double scale;

    @Column(name = "threshold")
    private int threshold;

    // Not used any longer, needed for historical reasons. Removing this would now
    // break the profile loading of old yaml files created prior to 4.1.0.
    @Transient
    private String operation;
    @Transient
    private String rootFolder;
    @Transient
    private boolean isLargeImage;
}
