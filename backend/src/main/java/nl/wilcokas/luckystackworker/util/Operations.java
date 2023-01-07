package nl.wilcokas.luckystackworker.util;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.text.StringSubstitutor;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.macro.Interpreter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.filter.LSWSharpenFilter;
import nl.wilcokas.luckystackworker.filter.SavitzkyGolayFilter;
import nl.wilcokas.luckystackworker.filter.SavitzkyGolayRadius;
import nl.wilcokas.luckystackworker.filter.settings.LSWSharpenMode;
import nl.wilcokas.luckystackworker.filter.settings.LSWSharpenParameters;
import nl.wilcokas.luckystackworker.filter.settings.UnsharpMaskParameters;
import nl.wilcokas.luckystackworker.model.OperationEnum;
import nl.wilcokas.luckystackworker.model.Profile;

@Slf4j
public final class Operations {
    private static final int STACK_POSITION_RED = 1;
    private static final int STACK_POSITION_GREEN = 2;
    private static final int STACK_POSITION_BLUE = 3;
    private static final String HISTOGRAM_STRETCH_MACRO = Util
            .readFromInputStream(Operations.class.getResourceAsStream("/histogramstretch.ijm"));

    public static void correctExposure(ImagePlus image) throws IOException {
        image.setDefault16bitRange(16);
        image.resetDisplayRange();
        image.updateAndDraw();
    }

    public static boolean isSharpenOperation(final OperationEnum operation) {
        return ((OperationEnum.AMOUNT == operation) || (OperationEnum.RADIUS == operation)
                || (OperationEnum.ITERATIONS == operation) || (OperationEnum.SHARPENMODE == operation)
                || (OperationEnum.CLIPPINGSTRENGTH == operation) || (OperationEnum.CLIPPINGRANGE == operation));
    }

    public static boolean isDenoiseOperation(final OperationEnum operation) {
        return (OperationEnum.DENOISEAMOUNT == operation) || (OperationEnum.DENOISERADIUS == operation) || (OperationEnum.DENOISESIGMA == operation)
                || (OperationEnum.DENOISEITERATIONS == operation);
    }

    public static boolean isSavitzkyGolayDenoiseOperation(final OperationEnum operation) {
        return (OperationEnum.SAVITZKYGOLAYAMOUNT == operation)
                || (OperationEnum.SAVITZKYGOLAYITERATIONS == operation) || (OperationEnum.SAVITZKYGOLAYSIZE == operation);
    }

    public static void applyAllOperationsExcept(final ImagePlus image, final Profile profile,
            final OperationEnum... operations) {
        List<OperationEnum> excludedOperationList = Arrays.asList(operations);
        if ((!excludedOperationList.contains(OperationEnum.AMOUNT)) && (!excludedOperationList.contains(OperationEnum.RADIUS))
                && (!excludedOperationList.contains(OperationEnum.ITERATIONS)) && (!excludedOperationList.contains(OperationEnum.CLIPPINGSTRENGTH))
                && (!excludedOperationList.contains(OperationEnum.CLIPPINGRANGE)) && (!excludedOperationList.contains(OperationEnum.SHARPENMODE))) {
            applySharpen(image, profile);
        }
        if ((!excludedOperationList.contains(OperationEnum.DENOISEAMOUNT))
                && (!excludedOperationList.contains(OperationEnum.DENOISESIGMA))
                && (!excludedOperationList.contains(OperationEnum.DENOISERADIUS))
                && (!excludedOperationList.contains(OperationEnum.DENOISEITERATIONS))) {
            applyDenoise(image, profile);
        }
        if ((!excludedOperationList.contains(OperationEnum.SAVITZKYGOLAYAMOUNT))
                && (!excludedOperationList.contains(OperationEnum.SAVITZKYGOLAYITERATIONS))
                && (!excludedOperationList.contains(OperationEnum.SAVITZKYGOLAYSIZE))) {
            applySavitzkyGolayDenoise(image, profile);
        }
        if ((!excludedOperationList.contains(OperationEnum.CONTRAST))
                && (!excludedOperationList.contains(OperationEnum.BRIGHTNESS))
                && (!excludedOperationList.contains(OperationEnum.BACKGROUND))) {
            applyBrightnessAndContrast(image, profile, true);
        }
        if (!excludedOperationList.contains(OperationEnum.RED)) {
            applyRed(image, profile);
        }
        if (!excludedOperationList.contains(OperationEnum.GREEN)) {
            applyGreen(image, profile);
        }
        if (!excludedOperationList.contains(OperationEnum.BLUE)) {
            applyBlue(image, profile);
        }
    }

    public static void applyAllOperations(ImagePlus image, final Map<String, String> profileSettings,
            String profileName) {
        final Profile profile = Util.toProfile(profileSettings, profileName);
        applySharpen(image, profile);
        applyDenoise(image, profile);
        applySavitzkyGolayDenoise(image, profile);
        applyGamma(image, profile);
        applyBrightnessAndContrast(image, profile, true);
        applyRed(image, profile);
        applyGreen(image, profile);
        applyBlue(image, profile);
        applySaturation(image, profile);
    }

    public static void applySharpen(final ImagePlus image, Profile profile) {
        int iterations = profile.getIterations() == 0 ? 1 : profile.getIterations();
        if (profile.getRadius() != null && profile.getAmount() != null) {
            log.info("Applying sharpen with radius {}, amount {}, iterations {} to image {}", profile.getRadius(),
                    profile.getAmount(), iterations, image.getID());
            float amount = profile.getAmount().divide(new BigDecimal("10000")).floatValue();
            LSWSharpenFilter filter = new LSWSharpenFilter();
            float clippingStrength = (profile.getClippingStrength()) / 500f;
            UnsharpMaskParameters usParams = UnsharpMaskParameters.builder().radius(profile.getRadius().doubleValue()).amount(amount)
                    .iterations(iterations).clippingStrength(clippingStrength).clippingRange(100 - profile.getClippingRange()).build();
            LSWSharpenMode mode = LSWSharpenMode.valueOf(profile.getSharpenMode());
            LSWSharpenParameters parameters = LSWSharpenParameters.builder().includeBlue(true).includeGreen(true).includeRed(true).individual(false)
                    .saturation(1f).unsharpMaskParameters(usParams).mode(mode).build();
            if (profile.getSharpenMode().equals(LSWSharpenMode.RGB.toString())) {
                filter.applyRGBMode(image, parameters.getUnsharpMaskParameters());
            } else {
                filter.applyLuminanceMode(image, parameters);
            }
        }
    }

    public static void applyDenoise(final ImagePlus image, final Profile profile) {
        if (profile.getDenoiseSigma() != null && (profile.getDenoiseSigma().compareTo(BigDecimal.ZERO) > 0)) {
            int iterations = profile.getDenoiseIterations() == 0 ? 1 : profile.getDenoiseIterations();
            log.info("Applying denoise with value {} to image {}", profile.getDenoise(), image.getID());
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

    public static void applyGamma(final ImagePlus image, final Profile profile) {
        if (profile.getGamma() != null && (profile.getGamma().compareTo(BigDecimal.ONE) != 0)) {
            log.info("Applying gamma correction with value {} to image {}", profile.getGamma(), image.getID());
            IJ.run(image, "Gamma...", String.format("value=%s", 2 - profile.getGamma().doubleValue()));
        }
    }

    public static void applyRed(final ImagePlus image, final Profile profile) {
        if (profile.getRed() != null && (profile.getRed().compareTo(BigDecimal.ZERO) > 0)) {
            if (validateRGBStack(image)) {
                log.info("Applying red reduction with value {} to image {}", profile.getRed(), image.getID());
                getImageStackProcessor(image, STACK_POSITION_RED).gamma(getGammaColorValue(profile.getRed()));
                image.updateAndDraw();
            } else {
                log.warn("Attemping to apply red reduction to a non RGB image {}", image.getFileInfo());
            }
        }
    }

    public static void applyGreen(final ImagePlus image, final Profile profile) {
        if (profile.getGreen() != null && (profile.getGreen().compareTo(BigDecimal.ZERO) > 0)) {
            if (validateRGBStack(image)) {
                log.info("Applying green reduction with value {} to image {}", profile.getGreen(), image.getID());
                getImageStackProcessor(image, STACK_POSITION_GREEN).gamma(getGammaColorValue(profile.getGreen()));
                image.updateAndDraw();
            } else {
                log.warn("Attemping to apply green reduction to a non RGB image {}", image.getFileInfo());
            }
        }
    }

    public static void applyBlue(final ImagePlus image, final Profile profile) {
        if (profile.getBlue() != null && (profile.getBlue().compareTo(BigDecimal.ZERO) > 0)) {
            if (validateRGBStack(image)) {
                log.info("Applying blue reduction with value {} to image {}", profile.getBlue(), image.getID());
                getImageStackProcessor(image, STACK_POSITION_BLUE).gamma(getGammaColorValue(profile.getBlue()));
                image.updateAndDraw();
            } else {
                log.warn("Attemping to apply blue reduction to a non RGB image {}", image.getFileInfo());
            }
        }
    }

    public static void applySaturation(final ImagePlus image, final Profile profile) {
        if (profile.getSaturation() != null && (profile.getSaturation().compareTo(BigDecimal.ONE) > 0)) {
            if (validateRGBStack(image)) {
                log.info("Applying saturation increase with factor {} to image {}", profile.getSaturation(),
                        image.getID());
                ImageStack stack = image.getStack();
                ImageProcessor ipRed = stack.getProcessor(1);
                ImageProcessor ipGreen = stack.getProcessor(2);
                ImageProcessor ipBlue = stack.getProcessor(3);

                FloatProcessor fpRed = ipRed.toFloat(1, null);
                FloatProcessor fpGreen = ipGreen.toFloat(2, null);
                FloatProcessor fpBlue = ipBlue.toFloat(3, null);
                fpRed.snapshot();
                fpGreen.snapshot();
                fpBlue.snapshot();
                float[] pixelsRed = (float[]) fpRed.getPixels();
                float[] pixelsGreen = (float[]) fpGreen.getPixels();
                float[] pixelsBlue = (float[]) fpBlue.getPixels();

                float[] pixelsHue = new float[pixelsRed.length];
                float[] pixelsSat = new float[pixelsRed.length];
                float[] pixelsLum = new float[pixelsRed.length];
                for (int i = 0; i < pixelsRed.length; i++) {
                    LSWSharpenMode mode = LSWSharpenMode.valueOf(profile.getSharpenMode());
                    float[] hsl = Util.rgbToHsl(pixelsRed[i], pixelsGreen[i], pixelsBlue[i], true, true, true, mode);
                    pixelsHue[i] = hsl[0];
                    pixelsSat[i] = hsl[1] * profile.getSaturation().floatValue();
                    pixelsLum[i] = hsl[2];
                }

                for (int i = 0; i < pixelsRed.length; i++) {
                    float[] rgb = Util.hslToRgb(pixelsHue[i], pixelsSat[i], pixelsLum[i], 0f);
                    pixelsRed[i] = rgb[0];
                    pixelsGreen[i] = rgb[1];
                    pixelsBlue[i] = rgb[2];
                }

                ipRed.setPixels(1, fpRed);
                ipGreen.setPixels(2, fpGreen);
                ipBlue.setPixels(3, fpBlue);
                image.updateAndDraw();
            } else {
                log.warn("Attemping to apply saturation increase to a non RGB image {}", image.getFileInfo());
            }
        }
    }

    public static void applyBrightnessAndContrast(ImagePlus image, final Profile profile, boolean copyMinMax) {
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

    public static void applySavitzkyGolayDenoise(ImagePlus image, final Profile profile) {
        log.info("Starting filter");
        int iterations = profile.getSavitzkyGolayIterations() == 0 ? 1 : profile.getSavitzkyGolayIterations();
        for (int i = 0; i < iterations; i++) {
            new SavitzkyGolayFilter().apply(image, SavitzkyGolayRadius.valueOf(profile.getSavitzkyGolaySize()), profile.getSavitzkyGolayAmount());
        }
    }

    private static ImageProcessor getImageStackProcessor(final ImagePlus img, final int stackPosition) {
        return img.getStack().getProcessor(stackPosition);
    }

    private static float getGammaColorValue(BigDecimal colorValue) {
        float value = 1.0F + (colorValue.divide(new BigDecimal("255"), 3, RoundingMode.HALF_EVEN)).floatValue();
        return value;
    }

    private static boolean validateRGBStack(ImagePlus image) {
        return image.getStack().size() == 3;
    }

}
