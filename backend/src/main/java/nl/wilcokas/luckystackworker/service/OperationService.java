package nl.wilcokas.luckystackworker.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.text.StringSubstitutor;
import org.springframework.stereotype.Service;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.macro.Interpreter;
import ij.process.ImageProcessor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.filter.LSWSharpenFilter;
import nl.wilcokas.luckystackworker.filter.RGBBalanceFilter;
import nl.wilcokas.luckystackworker.filter.SaturationFilter;
import nl.wilcokas.luckystackworker.filter.SavitzkyGolayFilter;
import nl.wilcokas.luckystackworker.filter.settings.LSWSharpenMode;
import nl.wilcokas.luckystackworker.filter.settings.LSWSharpenParameters;
import nl.wilcokas.luckystackworker.filter.settings.SavitzkyGolayRadius;
import nl.wilcokas.luckystackworker.filter.settings.UnsharpMaskParameters;
import nl.wilcokas.luckystackworker.model.OperationEnum;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.util.Util;

@Slf4j
@Service
public class OperationService {
    private static final String HISTOGRAM_STRETCH_MACRO = Util
            .readFromInputStream(OperationService.class.getResourceAsStream("/histogramstretch.ijm"));

    private final LSWSharpenFilter lswSharpenFilter;
    private final RGBBalanceFilter rgbBalanceFilter;
    private final SaturationFilter saturationFilter;
    private final SavitzkyGolayFilter savitzkyGolayFilter;

    public OperationService(final LSWSharpenFilter lswSharpenFilter, final RGBBalanceFilter rgbBalanceFilter,
            final SaturationFilter saturationFilter, final SavitzkyGolayFilter savitzkyGolayFilter) {
        this.lswSharpenFilter = lswSharpenFilter;
        this.rgbBalanceFilter = rgbBalanceFilter;
        this.saturationFilter = saturationFilter;
        this.savitzkyGolayFilter = savitzkyGolayFilter;
    }

    public void correctExposure(ImagePlus image) throws IOException {
        image.setDefault16bitRange(16);
        image.resetDisplayRange();
    }

    public boolean isSharpenOperation(final OperationEnum operation) {
        return ((OperationEnum.AMOUNT == operation) || (OperationEnum.RADIUS == operation) || (OperationEnum.ITERATIONS == operation)
                || (OperationEnum.SHARPENMODE == operation) || (OperationEnum.CLIPPINGSTRENGTH == operation)
                || (OperationEnum.CLIPPINGRANGE == operation));
    }

    public boolean isDenoiseOperation(final OperationEnum operation) {
        return (OperationEnum.DENOISEAMOUNT == operation) || (OperationEnum.DENOISERADIUS == operation) || (OperationEnum.DENOISESIGMA == operation)
                || (OperationEnum.DENOISEITERATIONS == operation);
    }

    public boolean isSavitzkyGolayDenoiseOperation(final OperationEnum operation) {
        return (OperationEnum.SAVITZKYGOLAYAMOUNT == operation) || (OperationEnum.SAVITZKYGOLAYITERATIONS == operation)
                || (OperationEnum.SAVITZKYGOLAYSIZE == operation);
    }

    public void applyAllOperationsExcept(final ImagePlus image, final Profile profile, final OperationEnum... operations) {
        List<OperationEnum> excludedOperationList = Arrays.asList(operations);
        if ((!excludedOperationList.contains(OperationEnum.AMOUNT)) && (!excludedOperationList.contains(OperationEnum.RADIUS))
                && (!excludedOperationList.contains(OperationEnum.ITERATIONS)) && (!excludedOperationList.contains(OperationEnum.CLIPPINGSTRENGTH))
                && (!excludedOperationList.contains(OperationEnum.CLIPPINGRANGE)) && (!excludedOperationList.contains(OperationEnum.SHARPENMODE))) {
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
        if ((!excludedOperationList.contains(OperationEnum.CONTRAST)) && (!excludedOperationList.contains(OperationEnum.BRIGHTNESS))
                && (!excludedOperationList.contains(OperationEnum.BACKGROUND))) {
            applyBrightnessAndContrast(image, profile, true);
        }
    }

    public void applyAllOperations(ImagePlus image, final Map<String, String> profileSettings, String profileName) {
        final Profile profile = Util.toProfile(profileSettings, profileName);
        applyAllOperations(image, profile);
    }

    public void applyAllOperations(ImagePlus image, Profile profile) {
        applySharpen(image, profile);
        applyDenoise(image, profile);
        applySavitzkyGolayDenoise(image, profile);
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
            UnsharpMaskParameters usParams = UnsharpMaskParameters.builder().radius(profile.getRadius().doubleValue()).amount(amount)
                    .iterations(iterations).clippingStrength(clippingStrength).clippingRange(100 - profile.getClippingRange()).build();
            LSWSharpenMode mode = LSWSharpenMode.valueOf(profile.getSharpenMode());
            LSWSharpenParameters parameters = LSWSharpenParameters.builder().includeBlue(true).includeGreen(true).includeRed(true).individual(false)
                    .saturation(1f).unsharpMaskParameters(usParams).mode(mode).build();
            if (profile.getSharpenMode().equals(LSWSharpenMode.RGB.toString()) || !validateRGBStack(image)) {
                if (profile.getClippingStrength() > 0) {
                    lswSharpenFilter.applyRGBModeAdaptive(image, parameters.getUnsharpMaskParameters());
                } else {
                    lswSharpenFilter.applyRGBMode(image, parameters.getUnsharpMaskParameters());
                }
            } else {
                if (profile.getClippingStrength() > 0) {
                    lswSharpenFilter.applyLuminanceModeAdaptive(image, parameters);
                } else {
                    lswSharpenFilter.applyLuminanceMode(image, parameters);
                }
            }
        }
    }

    public void applyDenoise(final ImagePlus image, final Profile profile) {
        if (profile.getDenoiseSigma() != null && (profile.getDenoiseSigma().compareTo(BigDecimal.ZERO) > 0)) {
            int iterations = profile.getDenoiseIterations() == 0 ? 1 : profile.getDenoiseIterations();
            log.info("Applying Sigma denoise with value {} to image {}", profile.getDenoise(), image.getID());
            BigDecimal factor = profile.getDenoise().compareTo(new BigDecimal("100")) > 0 ? new BigDecimal(100)
                    : profile.getDenoise();
            BigDecimal minimum = factor.divide(new BigDecimal(100), 2, RoundingMode.HALF_EVEN);
            String parameters = String.format("radius=%s use=%s minimum=%s outlier", profile.getDenoiseRadius(),
                    profile.getDenoiseSigma(),
                    minimum);
            for (int i = 0; i < iterations; i++) {
                IJ.run(image, "SigmaFilterPlus...", parameters);
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
        if (profile.getSaturation() != null && (profile.getSaturation().compareTo(BigDecimal.ONE) > 0)) {
            if (validateRGBStack(image)) {
                log.info("Applying saturation increase with factor {} to image {}", profile.getSaturation(),
                        image.getID());
                saturationFilter.apply(image, profile);
            } else {
                log.warn("Attemping to apply saturation increase to a non RGB image {}", image.getFileInfo());
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
            String result = stringSubstitutor.replace(HISTOGRAM_STRETCH_MACRO);
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

    private ImageProcessor getImageStackProcessor(final ImagePlus img, final int stackPosition) {
        return img.getStack().getProcessor(stackPosition);
    }

    private boolean validateRGBStack(ImagePlus image) {
        return image.getStack().size() == 3;
    }

}
