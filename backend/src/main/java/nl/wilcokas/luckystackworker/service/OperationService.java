package nl.wilcokas.luckystackworker.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import nl.wilcokas.luckystackworker.filter.*;
import nl.wilcokas.luckystackworker.filter.settings.*;
import nl.wilcokas.luckystackworker.ij.LswImageViewer;
import nl.wilcokas.luckystackworker.service.dto.LswImageLayersDto;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import ij.ImagePlus;
import ij.WindowManager;
import ij.macro.Interpreter;
import ij.plugin.Scaler;
import ij.process.ImageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.util.LswFileUtil;
import nl.wilcokas.luckystackworker.util.LswImageProcessingUtil;

@Slf4j
@RequiredArgsConstructor
@Service
public class OperationService {

    private final LSWSharpenFilter lswSharpenFilter;
    private final RGBBalanceFilter rgbBalanceFilter;
    private final SaturationFilter saturationFilter;
    private final SavitzkyGolayFilter savitzkyGolayFilter;
    private final SigmaFilterPlus sigmaFilterPlusFilter;
    private final DispersionCorrectionFilter dispersionCorrectionFilter;
    private final IansNoiseReductionFilter iansNoiseReductionFilter;
    private final EqualizeLocalHistogramsFilter equalizeLocalHistogramsFilter;
    private final ColorNormalisationFilter colorNormalisationFilter;
    private final HistogramStretchFilter histogramStretchFilter;
    private final BlendRawFilter blendRawFilter;
    private final ROFDenoiseFilter rofDenoiseFilter;
    private final WienerDeconvolutionFilter wienerDeconvolutionFilter;

    private int displayedProgress = 0;
    private Timer timer = new Timer();

    public void correctExposure(ImagePlus image) throws IOException {
        image.setDefault16bitRange(16);
        image.resetDisplayRange();
    }

    public void applyAllOperations(ImagePlus image, LswImageLayersDto unprocessedImageLayers, LswImageViewer viewer, Profile profile) throws IOException, InterruptedException {
        updateProgress(viewer, 0, true);

        // Sharpening filters
        applyWienerDeconvolution(image, profile);
        applySharpen(image, profile);
        updateProgress(viewer, 9);

        // Denoise filters -- pass 1
        applySigmaDenoise1(image, profile);
        applyIansNoiseReduction(image, profile);
        applyROFDenoise(image, profile);
        updateProgress(viewer, 18);

        // Denoise filters -- pass 2
        applySigmaDenoise2(image, profile);
        applySavitzkyGolayDenoise(image, profile);
        updateProgress(viewer, 27, true);

        // Light and contrast filters
        applyEqualizeLocalHistorgrams(image, profile);
        updateProgress(viewer, 36);
        applyLocalContrast(image, profile);
        updateProgress(viewer, 45);
        applyGamma(image, profile);
        updateProgress(viewer, 54);

        // Color filters
        applyColorNormalisation(image, profile);
        updateProgress(viewer, 63);
        applyRGBBalance(image, profile);
        updateProgress(viewer, 72);
        applySaturation(image, profile);
        updateProgress(viewer, 81);
        applyDispersionCorrection(image, profile);
        updateProgress(viewer, 90);

        // Always apply last
        applyBrightnessAndContrast(image, profile);
        updateProgress(viewer, 100);

        Timer resetProgressTimer = new Timer();
        resetProgressTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                displayedProgress = 0;
                resetProgressTimer.cancel();
                if (viewer != null) {
                    viewer.updateProgress(displayedProgress);
                }
            }
        }, Constants.ARTIFICIAL_PROGRESS_DELAY, Constants.ARTIFICIAL_PROGRESS_DELAY);
    }

    public ImagePlus scaleImage(final ImagePlus image, final double scale) {
        int newWidth = (int) (image.getWidth() * scale);
        int newHeight = (int) (image.getHeight() * scale);
        int depth = image.getStack().size();
        return Scaler.resize(image, newWidth, newHeight, depth, "depth=%s interpolation=Bicubic create".formatted(depth));
    }

    private void updateProgress(LswImageViewer viewer, int progress) {
        updateProgress(viewer, progress, false);
    }

    private void updateProgress(LswImageViewer viewer, int progress, boolean slowOperationNext) {
        if (displayedProgress < progress) {
            displayedProgress = progress;
        }
        if (viewer != null) {
            viewer.updateProgress(displayedProgress);
        }
        if (slowOperationNext) {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (viewer != null) {
                        viewer.updateProgress(displayedProgress++);
                    }
                }
            }, Constants.ARTIFICIAL_PROGRESS_DELAY, Constants.ARTIFICIAL_PROGRESS_DELAY);
        } else {
            timer.cancel();
        }
    }

    ;

    private void applySharpen(final ImagePlus image, Profile profile) {
        int iterations = profile.getIterations() == 0 ? 1 : profile.getIterations();
        if (profile.getApplyUnsharpMask().booleanValue() && profile.getRadius() != null && profile.getAmount() != null) {
            log.info("Applying sharpen with radius {}, amount {}, iterations {} to image {}", profile.getRadius(),
                    profile.getAmount(), iterations, image.getID());
            float amount = profile.getAmount().divide(new BigDecimal("10000")).floatValue();
            float clippingStrength = (profile.getClippingStrength()) / 500f;
            float deringStrength = profile.getDeringStrength() / 100f;
            float blendRaw = profile.getBlendRaw() / 100f;
            UnsharpMaskParameters usParams;
            LSWSharpenMode mode = (profile.getSharpenMode() == null) ? LSWSharpenMode.LUMINANCE : LSWSharpenMode.valueOf(profile.getSharpenMode());
            if (mode == LSWSharpenMode.LUMINANCE) {
                usParams = UnsharpMaskParameters.builder()
                        .radiusLuminance(profile.getRadius().doubleValue())
                        .amountLuminance(amount)
                        .iterationsLuminance(iterations)
                        .clippingStrengthLuminance(clippingStrength)
                        .clippingRangeLuminance(100 - profile.getClippingRange())
                        .deringRadiusLuminance(profile.getDeringRadius().doubleValue())
                        .deringStrengthLuminance(deringStrength)
                        .deringThresholdLuminance(profile.getDeringThreshold())
                        .blendRawLuminance(blendRaw)
                        .build();
            } else {
                int iterationsGreen = profile.getIterationsGreen() == 0 ? 1 : profile.getIterationsGreen();
                int iterationsBlue = profile.getIterationsBlue() == 0 ? 1 : profile.getIterationsBlue();
                float amountGreen = profile.getAmountGreen().divide(new BigDecimal("10000")).floatValue();
                float clippingStrengthGreen = (profile.getClippingStrengthGreen()) / 500f;
                float deringStrengthGreen = profile.getDeringStrengthGreen() / 100f;
                float amountBlue = profile.getAmountBlue().divide(new BigDecimal("10000")).floatValue();
                float clippingStrengthBlue = (profile.getClippingStrengthBlue()) / 500f;
                float deringStrengthBlue = profile.getDeringStrengthBlue() / 100f;
                float blendRawGreen = profile.getBlendRawGreen() / 100f;
                float blendRawBlue = profile.getBlendRawBlue() / 100f;
                usParams = UnsharpMaskParameters.builder()
                        .radiusRed(profile.getRadius().doubleValue())
                        .amountRed(amount)
                        .iterationsRed(iterations)
                        .clippingStrengthRed(clippingStrength)
                        .clippingRangeRed(100 - profile.getClippingRange())
                        .deringRadiusRed(profile.getDeringRadius().doubleValue())
                        .deringStrengthRed(deringStrength)
                        .deringThresholdRed(profile.getDeringThreshold())
                        .radiusGreen(profile.getRadiusGreen().doubleValue())
                        .amountGreen(amountGreen)
                        .iterationsGreen(iterationsGreen)
                        .clippingStrengthGreen(clippingStrengthGreen)
                        .clippingRangeGreen(100 - profile.getClippingRangeGreen())
                        .deringRadiusGreen(profile.getDeringRadiusGreen().doubleValue())
                        .deringStrengthGreen(deringStrengthGreen)
                        .deringThresholdGreen(profile.getDeringThresholdGreen())
                        .radiusBlue(profile.getRadiusBlue().doubleValue())
                        .amountBlue(amountBlue)
                        .iterationsBlue(iterationsBlue)
                        .clippingStrengthBlue(clippingStrengthBlue)
                        .clippingRangeBlue(100 - profile.getClippingRangeBlue())
                        .deringRadiusBlue(profile.getDeringRadiusBlue().doubleValue())
                        .deringStrengthBlue(deringStrengthBlue)
                        .deringThresholdBlue(profile.getDeringThresholdBlue())
                        .blendRawRed(blendRaw)
                        .blendRawGreen(blendRawGreen)
                        .blendRawBlue(blendRawBlue)
                        .build();
            }

            LSWSharpenParameters parameters = LSWSharpenParameters.builder().includeBlue(profile.isLuminanceIncludeBlue())
                    .includeGreen(profile.isLuminanceIncludeGreen()) //
                    .includeRed(profile.isLuminanceIncludeRed()).includeColor(profile.isLuminanceIncludeColor())
                    .saturation(1f).unsharpMaskParameters(usParams).mode(mode).build();
            if (!validateLuminanceInclusion(parameters)) {
                log.warn("Attempt to exclude all channels from luminance sharpen!");
                parameters.setIncludeRed(true);
                parameters.setIncludeGreen(true);
                parameters.setIncludeBlue(true);
            }
            if (profile.getSharpenMode().equals(LSWSharpenMode.RGB.toString()) || !validateRGBStack(image)) {
                if (profile.getClippingStrength() > 0) {
                    lswSharpenFilter.applyRGBModeClippingPrevention(image, parameters.getUnsharpMaskParameters());
                } else {
                    lswSharpenFilter.applyRGBMode(image, parameters.getUnsharpMaskParameters());
                }
            } else {
                if (profile.getClippingStrength() > 0) {
                    lswSharpenFilter.applyLuminanceModeClippingPrevention(image, parameters);
                } else {
                    lswSharpenFilter.applyLuminanceMode(image, parameters);
                }
            }
        }
    }

    private void applyWienerDeconvolution(final ImagePlus image, Profile profile) {
        if (profile.getApplyWienerDeconvolution().booleanValue()) {
            float deringStrength = profile.getDeringStrength() / 100f;
            float blendRaw = profile.getBlendRaw() / 100f;
            int iterations = profile.getWienerIterations() == 0 ? 1 : profile.getWienerIterations();
            WienerDeconvolutionParameters parameters;
            LSWSharpenMode mode = (profile.getSharpenMode() == null) ? LSWSharpenMode.LUMINANCE : LSWSharpenMode.valueOf(profile.getSharpenMode());
            if (mode == LSWSharpenMode.LUMINANCE) {
                parameters = WienerDeconvolutionParameters.builder()
                        .iterationsLuminance(iterations)
                        .deringRadiusLuminance(profile.getDeringRadius().doubleValue())
                        .deringStrengthLuminance(deringStrength)
                        .deringThresholdLuminance(profile.getDeringThreshold())
                        .blendRawLuminance(blendRaw)
                        .mode(LSWSharpenMode.LUMINANCE)
                        .build();
            } else {
                int iterationsGreen = profile.getWienerIterationsGreen() == 0 ? 1 : profile.getWienerIterationsGreen();
                int iterationsBlue = profile.getWienerIterationsBlue() == 0 ? 1 : profile.getWienerIterationsBlue();
                float deringStrengthGreen = profile.getDeringStrengthGreen() / 100f;
                float deringStrengthBlue = profile.getDeringStrengthBlue() / 100f;
                float blendRawGreen = profile.getBlendRawGreen() / 100f;
                float blendRawBlue = profile.getBlendRawBlue() / 100f;
                parameters = WienerDeconvolutionParameters.builder()
                        .iterationsRed(iterations)
                        .deringRadiusRed(profile.getDeringRadius().doubleValue())
                        .deringStrengthRed(deringStrength)
                        .deringThresholdRed(profile.getDeringThreshold())
                        .iterationsGreen(iterationsGreen)
                        .deringRadiusGreen(profile.getDeringRadiusGreen().doubleValue())
                        .deringStrengthGreen(deringStrengthGreen)
                        .deringThresholdGreen(profile.getDeringThresholdGreen())
                        .iterationsBlue(iterationsBlue)
                        .deringRadiusBlue(profile.getDeringRadiusBlue().doubleValue())
                        .deringStrengthBlue(deringStrengthBlue)
                        .deringThresholdBlue(profile.getDeringThresholdBlue())
                        .blendRawRed(blendRaw)
                        .blendRawGreen(blendRawGreen)
                        .blendRawBlue(blendRawBlue)
                        .mode(LSWSharpenMode.RGB)
                        .build();
            }

            if (!validateLuminanceInclusion(parameters)) {
                log.warn("Attempt to exclude all channels from luminance sharpen!");
                parameters.setIncludeRed(true);
                parameters.setIncludeGreen(true);
                parameters.setIncludeBlue(true);
            }
            wienerDeconvolutionFilter.apply(image, parameters);
        }
    }

    private void applyLocalContrast(final ImagePlus image, Profile profile) {
        LSWSharpenMode mode = (profile.getLocalContrastMode() == null) ? LSWSharpenMode.LUMINANCE
                : LSWSharpenMode.valueOf(profile.getLocalContrastMode());
        if (profile.getLocalContrastFine() != 0) {
            applyLocalContrast(image, profile.getLocalContrastFine(), Constants.LOCAL_CONTRAST_FINE_RADIUS, mode);
        }
        if (profile.getLocalContrastMedium() != 0) {
            applyLocalContrast(image, profile.getLocalContrastMedium(), Constants.LOCAL_CONTRAST_MEDIUM_RADIUS, mode);
        }
        if (profile.getLocalContrastLarge() != 0) {
            applyLocalContrast(image, profile.getLocalContrastLarge(), Constants.LOCAL_CONTRAST_LARGE_RADIUS, mode);
        }
    }

    private void applySigmaDenoise1(final ImagePlus image, final Profile profile) {
        if (Constants.DENOISE_ALGORITHM_SIGMA1.equals(profile.getDenoiseAlgorithm1())) {
            log.info("Applying Sigma denoise mode 1 with value {} to image {}", profile.getDenoise1Amount(), image.getID());
            sigmaFilterPlusFilter.applyDenoise1(image, profile);
        }
    }

    private void applySigmaDenoise2(final ImagePlus image, final Profile profile) {
        if (Constants.DENOISE_ALGORITHM_SIGMA2.equals(profile.getDenoiseAlgorithm2())) {
            int iterations = profile.getDenoise2Iterations() == 0 ? 1 : profile.getDenoise2Iterations();
            log.info("Applying Sigma denoise mode 2 with radius {} and {} iterations to image {}", profile.getDenoise2Radius(), iterations,
                    image.getID());
            sigmaFilterPlusFilter.applyDenoise2(image, profile);
        }
    }

    private void applyIansNoiseReduction(final ImagePlus image, final Profile profile) throws IOException, InterruptedException {
        if (Constants.DENOISE_ALGORITHM_IANS.equals(profile.getDenoiseAlgorithm1())) {
            log.info("Applying Ian's noise reduction to image {}", image.getID());
            IansNoiseReductionParameters parameters = IansNoiseReductionParameters.builder().fine(profile.getIansAmount()).medium(profile.getIansAmountMid())
                    .large(BigDecimal.ZERO).recovery(profile.getIansRecovery()).iterations(profile.getIansIterations()).build();
            iansNoiseReductionFilter.apply(image, profile.getName(), parameters, profile.getScale());
        }
    }

    private void applyROFDenoise(final ImagePlus image, final Profile profile) {
        if (Constants.DENOISE_ALGORITHM_ROF.equals(profile.getDenoiseAlgorithm1())) {
            log.info("Applying ROF denoising to image {}", image.getID());
            rofDenoiseFilter.apply(image, profile);
        }
    }

    private void applyGamma(final ImagePlus image, final Profile profile) {
        if (profile.getGamma() != null && (profile.getGamma().compareTo(BigDecimal.ONE) != 0)) {
            log.info("Applying gamma correction with value {} to image {}", profile.getGamma(), image.getID());
            for (int slice = 1; slice <= image.getStack().getSize(); slice++) {
                ImageProcessor ip = getImageStackProcessor(image, slice);
                ip.gamma(2d - profile.getGamma().doubleValue());
            }
        }

    }

    private void applyRGBBalance(final ImagePlus image, final Profile profile) {
        if ((profile.getRed() != null && (!profile.getRed().equals(BigDecimal.ZERO)))
                || (profile.getGreen() != null && (!profile.getGreen().equals(BigDecimal.ZERO)))
                || (profile.getBlue() != null && (!profile.getBlue().equals(BigDecimal.ZERO)))) {
            if (validateRGBStack(image)) {
                log.info("Applying RGB balance correction to image {} with values R {}, G {}, B {}", image.getID(), profile.getRed(), profile.getGreen(),
                        profile.getBlue());
                rgbBalanceFilter.apply(image, profile.getRed().intValue(), profile.getGreen().intValue(), profile.getBlue().intValue(),
                        profile.getPurple().intValue() / 255D, profile.isPreserveDarkBackground());
            }
        }
    }

    private void applyColorNormalisation(final ImagePlus image, final Profile profile) {
        if (profile.isNormalizeColorBalance()) {
            colorNormalisationFilter.apply(image);
        }
    }

    private void applySaturation(final ImagePlus image, final Profile profile) {
        if (profile.getSaturation() != null) {
            if (validateRGBStack(image)) {
                log.info("Applying saturation increase with factor {} to image {}", profile.getSaturation(),
                        image.getID());
                saturationFilter.apply(image, profile);
            } else {
                log.debug("Attemping to apply saturation increase to a non RGB image {}", image.getFileInfo());
            }
        }
    }

    private void applyEqualizeLocalHistorgrams(final ImagePlus image, final Profile profile) throws IOException, InterruptedException {
        log.info("Applying equalize local historgrams with strength {} to image {}", profile.getEqualizeLocalHistogramsStrength(), image.getID());
        equalizeLocalHistogramsFilter.apply(image, profile.getName(), profile.getEqualizeLocalHistogramsStrength(), profile.getScale());
    }

    private void applyBrightnessAndContrast(ImagePlus image, final Profile profile) {
        if (profile.getContrast() != 0 || profile.getBrightness() != 0 || profile.getLightness() != 0 || profile.getBackground() != 0) {
            log.info("Applying contrast increase with factor {} to image {}", profile.getContrast(),
                    image.getID());

            // Contrast
            double newMin = Math.round((profile.getContrast()) * (16384.0 / 100.0));
            double newMax = 65536 - newMin;

            // Brightness
            newMax = Math.round(newMax - (profile.getBrightness()) * (49152.0 / 100.0));

            histogramStretchFilter.apply(image, newMin, newMax, profile.getLightness(), profile.getBackground(), profile.isPreserveDarkBackground());

        }
    }

    private void applySavitzkyGolayDenoise(ImagePlus image, final Profile profile) {
        if (Constants.DENOISE_ALGORITHM_SAVGOLAY.equals(profile.getDenoiseAlgorithm2())) {
            log.info("Starting SavitzkyGolayDenoise filter");
            savitzkyGolayFilter.apply(image, profile);
        }
    }

    private void applyDispersionCorrection(final ImagePlus image, final Profile profile) {
        if (profile.isDispersionCorrectionEnabled() && validateRGBStack(image)) {
            log.info("Applying dispersion correction");
            dispersionCorrectionFilter.apply(image, profile);
        }
    }

    private boolean validateLuminanceInclusion(LSWSharpenParameters parameters) {
        return parameters.isIncludeRed() || parameters.isIncludeGreen() || parameters.isIncludeBlue();
    }

    private boolean validateLuminanceInclusion(WienerDeconvolutionParameters parameters) {
        return parameters.isIncludeRed() || parameters.isIncludeGreen() || parameters.isIncludeBlue();
    }

    private void applyLocalContrast(final ImagePlus image, int amount, BigDecimal radius, LSWSharpenMode localContrastMode) {
        log.info("Applying local contrast with mode {}, radius {} amount {} to image {}", localContrastMode, radius, amount, image.getID());
        float famount = (amount) / 100f;
        UnsharpMaskParameters usParams = UnsharpMaskParameters.builder()
                .radiusRed(radius.doubleValue()).amountRed(famount).iterationsRed(1)
                .radiusGreen(radius.doubleValue()).amountGreen(famount).iterationsGreen(1)
                .radiusBlue(radius.doubleValue()).amountBlue(famount).iterationsBlue(1)
                .build();
        LSWSharpenParameters parameters = LSWSharpenParameters.builder().includeBlue(true).includeGreen(true).includeRed(true).individual(false)
                .saturation(1f).unsharpMaskParameters(usParams).mode(localContrastMode).build();
        lswSharpenFilter.applyRGBMode(image, parameters.getUnsharpMaskParameters());
    }

    private ImageProcessor getImageStackProcessor(final ImagePlus img, final int stackPosition) {
        return img.getStack().getProcessor(stackPosition);
    }

    private boolean validateRGBStack(ImagePlus image) {
        return image.getStack().size() == 3;
    }

    private void blendRaw(ImagePlus image, final LswImageLayersDto unprocessedImageLayers, final Profile profile) {
        if (profile.getBlendRaw() > 0 || profile.getBlendRawGreen() > 0 || profile.getBlendRawBlue() > 0) {
            double blendRawRedFactor = profile.getBlendRaw() / 100.0;
            double blendRawGreenFactor = profile.getBlendRawGreen() / 100.0;
            double blendRawBlueFactor = profile.getBlendRawBlue() / 100.0;
            blendRawFilter.apply(image, unprocessedImageLayers, blendRawRedFactor, blendRawGreenFactor, blendRawBlueFactor);
        }
    }

}
