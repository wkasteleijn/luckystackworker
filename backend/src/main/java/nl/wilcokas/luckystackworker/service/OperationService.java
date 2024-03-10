package nl.wilcokas.luckystackworker.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

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
import nl.wilcokas.luckystackworker.filter.DispersionCorrectionFilter;
import nl.wilcokas.luckystackworker.filter.EqualizeLocalHistogramsFilter;
import nl.wilcokas.luckystackworker.filter.IansNoiseReductionFilter;
import nl.wilcokas.luckystackworker.filter.LSWSharpenFilter;
import nl.wilcokas.luckystackworker.filter.RGBBalanceFilter;
import nl.wilcokas.luckystackworker.filter.SaturationFilter;
import nl.wilcokas.luckystackworker.filter.SavitzkyGolayFilter;
import nl.wilcokas.luckystackworker.filter.SigmaFilterPlus;
import nl.wilcokas.luckystackworker.filter.settings.IansNoiseReductionParameters;
import nl.wilcokas.luckystackworker.filter.settings.LSWSharpenMode;
import nl.wilcokas.luckystackworker.filter.settings.LSWSharpenParameters;
import nl.wilcokas.luckystackworker.filter.settings.SavitzkyGolayRadius;
import nl.wilcokas.luckystackworker.filter.settings.UnsharpMaskParameters;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.util.LswFileUtil;
import nl.wilcokas.luckystackworker.util.LswImageProcessingUtil;

@Slf4j
@RequiredArgsConstructor
@Service
public class OperationService {

    private static String histogramStretchMacro;
    static {
        try {
            histogramStretchMacro = LswFileUtil
                    .readFromInputStream(new ClassPathResource("histogramstretch.ijm").getInputStream());
        } catch (IOException e) {
            log.error("Error loading histogram stretch script",e);
        }
    }

    private final LSWSharpenFilter lswSharpenFilter;
    private final RGBBalanceFilter rgbBalanceFilter;
    private final SaturationFilter saturationFilter;
    private final SavitzkyGolayFilter savitzkyGolayFilter;
    private final SigmaFilterPlus sigmaFilterPlusFilter;
    private final DispersionCorrectionFilter dispersionCorrectionFilter;
    private final IansNoiseReductionFilter iansNoiseReductionFilter;
    private final EqualizeLocalHistogramsFilter equalizeLocalHistogramsFilter;

    public void correctExposure(ImagePlus image) throws IOException {
        image.setDefault16bitRange(16);
        image.resetDisplayRange();
    }

    public void applyAllOperations(ImagePlus image, Profile profile) throws IOException, InterruptedException {
        applySharpen(image, profile);
        applySigmaDenoise1(image, profile);
        applyIansNoiseReduction(image, profile);
        applySigmaDenoise2(image, profile);
        applySavitzkyGolayDenoise(image, profile);
        applyEqualizeLocalHistorgrams(image, profile);
        applyLocalContrast(image, profile);
        applyDispersionCorrection(image, profile);
        applyBrightnessAndContrast(image, profile, true);
        applyRGBBalance(image, profile);
        applyGamma(image, profile);
        applySaturation(image, profile);
    }

    public ImagePlus scaleImage(final ImagePlus image, final double scale) {
        int newWidth = (int) (image.getWidth() * scale);
        int newHeight = (int) (image.getHeight() * scale);
        int depth = image.getStack().size();
        return Scaler.resize(image, newWidth, newHeight, depth, "depth=%s interpolation=Bicubic create".formatted(depth));
    }

    private void applySharpen(final ImagePlus image, Profile profile) {
        int iterations = profile.getIterations() == 0 ? 1 : profile.getIterations();
        if (profile.getRadius() != null && profile.getAmount() != null) {
            log.info("Applying sharpen with radius {}, amount {}, iterations {} to image {}", profile.getRadius(),
                    profile.getAmount(), iterations, image.getID());
            float amount = profile.getAmount().divide(new BigDecimal("10000")).floatValue();
            float clippingStrength = (profile.getClippingStrength()) / 500f;
            float deringStrength = profile.getDeringStrength() / 100f;
            UnsharpMaskParameters usParams = UnsharpMaskParameters.builder().radius(profile.getRadius().doubleValue()).amount(amount)
                    .iterations(iterations).clippingStrength(clippingStrength).clippingRange(100 - profile.getClippingRange())
                    .deringRadius(profile.getDeringRadius().doubleValue()).deringStrength(deringStrength)
                    .deringThreshold(profile.getDeringThreshold())
                    .build();
            LSWSharpenMode mode = (profile.getSharpenMode() == null) ? LSWSharpenMode.LUMINANCE : LSWSharpenMode.valueOf(profile.getSharpenMode());
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
            int iterations = profile.getDenoise1Iterations() == 0 ? 1 : profile.getDenoise1Iterations();
            log.info("Applying Sigma denoise mode 1 with value {} to image {}", profile.getDenoise1Amount(), image.getID());
            BigDecimal factor = profile.getDenoise1Amount().compareTo(new BigDecimal("100")) > 0 ? new BigDecimal(100) : profile.getDenoise1Amount();
            BigDecimal minimum = factor.divide(new BigDecimal(100), 2, RoundingMode.HALF_EVEN);
            for (int i = 0; i < iterations; i++) {
                sigmaFilterPlusFilter.apply(image, profile.getDenoise1Radius().doubleValue(), 2D,
                        minimum.doubleValue());
            }
        }
    }

    private void applySigmaDenoise2(final ImagePlus image, final Profile profile) {
        if (Constants.DENOISE_ALGORITHM_SIGMA2.equals(profile.getDenoiseAlgorithm2())) {
            int iterations = profile.getDenoise2Iterations() == 0 ? 1 : profile.getDenoise2Iterations();
            log.info("Applying Sigma denoise mode 2 with radius {} and {} iterations to image {}", profile.getDenoise2Radius(), iterations,
                    image.getID());
            for (int i = 0; i < iterations; i++) {
                sigmaFilterPlusFilter.apply(image, profile.getDenoise2Radius().doubleValue(), 5D, 1D);
            }
        }
    }

    private void applyIansNoiseReduction(final ImagePlus image, final Profile profile) throws IOException, InterruptedException {
        if (Constants.DENOISE_ALGORITHM_IANS.equals(profile.getDenoiseAlgorithm1())) {
            log.info("Applying Ian's noise reduction to image {}", image.getID());
            IansNoiseReductionParameters parameters = IansNoiseReductionParameters.builder().fine(profile.getIansAmount()).medium(BigDecimal.ZERO)
                    .large(BigDecimal.ZERO).recovery(profile.getIansRecovery()).build();
            iansNoiseReductionFilter.apply(image, profile.getName(), parameters);
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
                        profile.getPurple().intValue() / 255D);
            }
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
        equalizeLocalHistogramsFilter.apply(image, profile.getName(), profile.getEqualizeLocalHistogramsStrength());
    }

    private void applyBrightnessAndContrast(ImagePlus image, final Profile profile, boolean copyMinMax) {
        if (profile.getContrast() != 0 || profile.getBrightness() != 0 || profile.getBackground() != 0) {
            if (copyMinMax) {
                LswImageProcessingUtil.copyMinMax(image, image);
            }
            log.info("Applying contrast increase with factor {} to image {}", profile.getContrast(),
                    image.getID());

            // Contrast
            double newMin = Math.round((profile.getContrast()) * (16384.0 / 100.0));
            double newMax = 65536 - newMin;

            // Brightness
            newMax = Math.round(newMax - (profile.getBrightness()) * (49152.0 / 100.0));

            // Darken background
            newMin = Math.round(newMin + (profile.getBackground()) * (16384.0 / 100.0));

            StringSubstitutor stringSubstitutor = new StringSubstitutor(
                    Map.of("isStack", validateRGBStack(image), "newMin", newMin, "newMax", newMax));
            String result = stringSubstitutor.replace(histogramStretchMacro);
            WindowManager.setTempCurrentImage(image);
            new Interpreter().run(result);
        }
    }

    private void applySavitzkyGolayDenoise(ImagePlus image, final Profile profile) {
        if (Constants.DENOISE_ALGORITHM_SAVGOLAY.equals(profile.getDenoiseAlgorithm2())) {
            log.info("Starting SavitzkyGolayDenoise filter");
            int iterations = profile.getSavitzkyGolayIterations() == 0 ? 1 : profile.getSavitzkyGolayIterations();
            for (int i = 0; i < iterations; i++) {
                savitzkyGolayFilter.apply(image, SavitzkyGolayRadius.valueOf(profile.getSavitzkyGolaySize()), profile.getSavitzkyGolayAmount());
            }
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

    private void applyLocalContrast(final ImagePlus image, int amount, BigDecimal radius, LSWSharpenMode localContrastMode) {
        log.info("Applying local contrast with mode {}, radius {} amount {} to image {}", localContrastMode, radius, amount, image.getID());
        float famount = (amount) / 100f;
        UnsharpMaskParameters usParams = UnsharpMaskParameters.builder().radius(radius.doubleValue()).amount(famount).iterations(1).build();
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

}
