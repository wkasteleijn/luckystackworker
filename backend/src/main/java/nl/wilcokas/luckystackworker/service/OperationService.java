package nl.wilcokas.luckystackworker.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
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
import nl.wilcokas.luckystackworker.filter.LSWSharpenFilter;
import nl.wilcokas.luckystackworker.filter.RGBBalanceFilter;
import nl.wilcokas.luckystackworker.filter.SaturationFilter;
import nl.wilcokas.luckystackworker.filter.SavitzkyGolayFilter;
import nl.wilcokas.luckystackworker.filter.SigmaFilterPlus;
import nl.wilcokas.luckystackworker.filter.settings.LSWSharpenMode;
import nl.wilcokas.luckystackworker.filter.settings.LSWSharpenParameters;
import nl.wilcokas.luckystackworker.filter.settings.SavitzkyGolayRadius;
import nl.wilcokas.luckystackworker.filter.settings.UnsharpMaskParameters;
import nl.wilcokas.luckystackworker.model.OperationEnum;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.util.Util;

@Slf4j
@RequiredArgsConstructor
@Service
public class OperationService {

    private static String histogramStretchMacro;
    static {
        try {
            histogramStretchMacro = Util
                    .readFromInputStream(new ClassPathResource("/histogramstretch.ijm").getInputStream());
        } catch (IOException e) {
            log.error("Error loading histogram stretch script");
        }
    }

    private final LSWSharpenFilter lswSharpenFilter;
    private final RGBBalanceFilter rgbBalanceFilter;
    private final SaturationFilter saturationFilter;
    private final SavitzkyGolayFilter savitzkyGolayFilter;
    private final SigmaFilterPlus sigmaFilterPlusFilter;
    private final DispersionCorrectionFilter dispersionCorrectionFilter;

    public void correctExposure(ImagePlus image) throws IOException {
        image.setDefault16bitRange(16);
        image.resetDisplayRange();
    }

    public boolean isSharpenOperation(final OperationEnum operation) {
        return ((OperationEnum.AMOUNT == operation) || (OperationEnum.RADIUS == operation) || (OperationEnum.ITERATIONS == operation)
                || (OperationEnum.SHARPENMODE == operation) || (OperationEnum.CLIPPINGSTRENGTH == operation)
                || (OperationEnum.CLIPPINGRANGE == operation) || (OperationEnum.DERINGRADIUS == operation)
                || (OperationEnum.DERINGSTRENGTH == operation));
    }

    public boolean isDenoiseOperation(final OperationEnum operation) {
        return (OperationEnum.DENOISEAMOUNT == operation) || (OperationEnum.DENOISERADIUS == operation) || (OperationEnum.DENOISESIGMA == operation)
                || (OperationEnum.DENOISEITERATIONS == operation);
    }

    public boolean isLocalContrastOperation(final OperationEnum operation) {
        return (OperationEnum.LOCALCONTRASTFINE == operation) || (OperationEnum.LOCALCONTRASTMEDIUM == operation)
                || (OperationEnum.LOCALCONTRASTLARGE == operation);
    }

    public boolean isSavitzkyGolayDenoiseOperation(final OperationEnum operation) {
        return (OperationEnum.SAVITZKYGOLAYAMOUNT == operation) || (OperationEnum.SAVITZKYGOLAYITERATIONS == operation)
                || (OperationEnum.SAVITZKYGOLAYSIZE == operation);
    }

    public void applyAllOperationsExcept(final ImagePlus image, final Profile profile, final OperationEnum... operations) {
        List<OperationEnum> excludedOperationList = Arrays.asList(operations);
        if ((!excludedOperationList.contains(OperationEnum.AMOUNT)) && (!excludedOperationList.contains(OperationEnum.RADIUS))
                && (!excludedOperationList.contains(OperationEnum.ITERATIONS)) && (!excludedOperationList.contains(OperationEnum.CLIPPINGSTRENGTH))
                && (!excludedOperationList.contains(OperationEnum.CLIPPINGRANGE)) && (!excludedOperationList.contains(OperationEnum.DERINGRADIUS))
                && (!excludedOperationList.contains(OperationEnum.DERINGSTRENGTH)) && (!excludedOperationList.contains(OperationEnum.SHARPENMODE))) {
            applySharpen(image, profile);
        }
        if ((!excludedOperationList.contains(OperationEnum.DENOISEAMOUNT)) && (!excludedOperationList.contains(OperationEnum.DENOISESIGMA))
                && (!excludedOperationList.contains(OperationEnum.DENOISERADIUS))
                && (!excludedOperationList.contains(OperationEnum.DENOISEITERATIONS))) {
            applyDenoise(image, profile);
        }
        if ((!excludedOperationList.contains(OperationEnum.SAVITZKYGOLAYAMOUNT))
                && (!excludedOperationList.contains(OperationEnum.SAVITZKYGOLAYITERATIONS))
                && (!excludedOperationList.contains(OperationEnum.SAVITZKYGOLAYSIZE))) {
            applySavitzkyGolayDenoise(image, profile);
        }
        if ((!excludedOperationList.contains(OperationEnum.LOCALCONTRASTFINE)) && (!excludedOperationList.contains(OperationEnum.LOCALCONTRASTMEDIUM))
                && (!excludedOperationList.contains(OperationEnum.LOCALCONTRASTLARGE))) {
            applyLocalContrast(image, profile);
        }
        if ((!excludedOperationList.contains(OperationEnum.CONTRAST)) && (!excludedOperationList.contains(OperationEnum.BRIGHTNESS))
                && (!excludedOperationList.contains(OperationEnum.BACKGROUND))) {
            applyBrightnessAndContrast(image, profile, true);
        }
    }

    public void applyAllOperations(ImagePlus image, Profile profile) {
        applySharpen(image, profile);
        applyDenoise(image, profile);
        applySavitzkyGolayDenoise(image, profile);
        applyLocalContrast(image, profile);
        applyDispersionCorrection(image, profile);
        applyBrightnessAndContrast(image, profile, true);
        applyRGBBalance(image, profile);
        applyGamma(image, profile);
        applySaturation(image, profile);
    }

    public void applySharpen(final ImagePlus image, Profile profile) {
        int iterations = profile.getIterations() == 0 ? 1 : profile.getIterations();
        if (profile.getRadius() != null && profile.getAmount() != null) {
            log.info("Applying sharpen with radius {}, amount {}, iterations {} to image {}", profile.getRadius(),
                    profile.getAmount(), iterations, image.getID());
            float amount = profile.getAmount().divide(new BigDecimal("10000")).floatValue();
            float clippingStrength = (profile.getClippingStrength()) / 500f;
            float deringStrength = profile.getDeringStrength() / 100f;
            UnsharpMaskParameters usParams = UnsharpMaskParameters.builder().radius(profile.getRadius().doubleValue()).amount(amount)
                    .iterations(iterations).clippingStrength(clippingStrength).clippingRange(100 - profile.getClippingRange())
                    .deringRadius(profile.getDeringRadius().doubleValue()).deringStrength(deringStrength).build();
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

    public void applyLocalContrast(final ImagePlus image, Profile profile) {
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

    public void applyDenoise(final ImagePlus image, final Profile profile) {
        if (profile.getDenoiseSigma() != null && (profile.getDenoiseSigma().compareTo(BigDecimal.ZERO) > 0)) {
            int iterations = profile.getDenoiseIterations() == 0 ? 1 : profile.getDenoiseIterations();
            log.info("Applying Sigma denoise with value {} to image {}", profile.getDenoise(), image.getID());
            BigDecimal factor = profile.getDenoise().compareTo(new BigDecimal("100")) > 0 ? new BigDecimal(100)
                    : profile.getDenoise();
            BigDecimal minimum = factor.divide(new BigDecimal(100), 2, RoundingMode.HALF_EVEN);
            for (int i = 0; i < iterations; i++) {
                sigmaFilterPlusFilter.apply(image, profile.getDenoiseRadius().doubleValue(), profile.getDenoiseSigma().doubleValue(),
                        minimum.doubleValue());
            }
        }
    }

    public void applyGamma(final ImagePlus image, final Profile profile) {
        if (profile.getGamma() != null && (profile.getGamma().compareTo(BigDecimal.ONE) != 0)) {
            log.info("Applying gamma correction with value {} to image {}", profile.getGamma(), image.getID());
            for (int slice = 1; slice <= image.getStack().getSize(); slice++) {
                ImageProcessor ip = getImageStackProcessor(image, slice);
                ip.gamma(2d - profile.getGamma().doubleValue());
            }
        }

    }

    public void applyRGBBalance(final ImagePlus image, final Profile profile) {
        if ((profile.getRed() != null && (profile.getRed().compareTo(BigDecimal.ONE) > 0))
                || (profile.getGreen() != null && (profile.getGreen().compareTo(BigDecimal.ONE) > 0))
                || (profile.getBlue() != null && (profile.getBlue().compareTo(BigDecimal.ONE) > 0))) {
            if (validateRGBStack(image)) {
                log.info("Applying RGB balance correction to image {} with values R {}, G {}, B {}", image.getID(), profile.getRed(), profile.getGreen(),
                        profile.getBlue());
                rgbBalanceFilter.apply(image, profile.getRed().intValue(), profile.getGreen().intValue(), profile.getBlue().intValue());
            }
        }
    }

    public void applySaturation(final ImagePlus image, final Profile profile) {
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

    public void applyBrightnessAndContrast(ImagePlus image, final Profile profile, boolean copyMinMax) {
        if (profile.getContrast() != 0 || profile.getBrightness() != 0 || profile.getBackground() != 0) {
            if (copyMinMax) {
                Util.copyMinMax(image, image);
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

    public void applySavitzkyGolayDenoise(ImagePlus image, final Profile profile) {
        if (profile.getSavitzkyGolaySize() > 0) {
            log.info("Starting SavitzkyGolayDenoise filter");
            int iterations = profile.getSavitzkyGolayIterations() == 0 ? 1 : profile.getSavitzkyGolayIterations();
            for (int i = 0; i < iterations; i++) {
                savitzkyGolayFilter.apply(image, SavitzkyGolayRadius.valueOf(profile.getSavitzkyGolaySize()), profile.getSavitzkyGolayAmount());
            }
        }
    }

    public void applyDispersionCorrection(final ImagePlus image, final Profile profile) {
        if (profile.isDispersionCorrectionEnabled() && validateRGBStack(image)) {
            log.info("Applying dispersion correction");
            dispersionCorrectionFilter.apply(image, profile);
        }
    }

    public ImagePlus scaleImage(final ImagePlus image, final double scale) {
        int newWidth = (int) (image.getWidth() * scale);
        int newHeight = (int) (image.getHeight() * scale);
        return Scaler.resize(image, newWidth, newHeight, 3, "depth=3 interpolation=Bicubic create");
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
