package nl.wilcokas.luckystackworker.filter;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.GaussianBlur;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import java.awt.Rectangle;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.exceptions.FilterException;
import nl.wilcokas.luckystackworker.filter.settings.LSWSharpenMode;
import nl.wilcokas.luckystackworker.filter.settings.LSWSharpenParameters;
import nl.wilcokas.luckystackworker.filter.settings.UnsharpMaskParameters;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.util.LswImageProcessingUtil;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LSWSharpenFilter implements LSWFilter {

    private static final float FLOAT_MAX_SATURATED_VALUE = 65535f;

    @Override
    public boolean apply(final ImagePlus image, Profile profile, boolean isMono, String... additionalArguments) {
        int iterations = profile.getIterations() == 0 ? 1 : profile.getIterations();
        if (isApplied(profile, image)) {
            log.info(
                    "Applying sharpen with radius {}, amount {}, iterations {} to image {}",
                    profile.getRadius(),
                    profile.getAmount(),
                    iterations,
                    image.getID());
            float amount = profile.getAmount().divide(new BigDecimal("10000")).floatValue();
            float clippingStrength = (profile.getClippingStrength()) / 500f;
            float deringStrength = profile.getDeringStrength() / 100f;
            float blendRaw = profile.getBlendRaw() / 100f;
            UnsharpMaskParameters usParams;
            LSWSharpenMode mode = (profile.getSharpenMode() == null)
                    ? LSWSharpenMode.LUMINANCE
                    : LSWSharpenMode.valueOf(profile.getSharpenMode());
            if (mode == LSWSharpenMode.LUMINANCE) {
                usParams = UnsharpMaskParameters.builder()
                        .radiusLuminance(profile.getRadius().doubleValue())
                        .amountLuminance(amount)
                        .iterationsLuminance(iterations)
                        .clippingStrengthLuminance(clippingStrength)
                        .clippingRangeLuminance(100 - profile.getClippingRange())
                        .deringRadiusLuminance(profile.getDeringRadius().doubleValue())
                        .deringStrengthLuminance(deringStrength)
                        .blendRawLuminance(blendRaw)
                        .build();
            } else {
                int iterationsGreen = profile.getIterationsGreen() == 0 ? 1 : profile.getIterationsGreen();
                int iterationsBlue = profile.getIterationsBlue() == 0 ? 1 : profile.getIterationsBlue();
                float amountGreen =
                        profile.getAmountGreen().divide(new BigDecimal("10000")).floatValue();
                float clippingStrengthGreen = (profile.getClippingStrengthGreen()) / 500f;
                float deringStrengthGreen = profile.getDeringStrengthGreen() / 100f;
                float amountBlue =
                        profile.getAmountBlue().divide(new BigDecimal("10000")).floatValue();
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
                        .radiusGreen(profile.getRadiusGreen().doubleValue())
                        .amountGreen(amountGreen)
                        .iterationsGreen(iterationsGreen)
                        .clippingStrengthGreen(clippingStrengthGreen)
                        .clippingRangeGreen(100 - profile.getClippingRangeGreen())
                        .deringRadiusGreen(profile.getDeringRadiusGreen().doubleValue())
                        .deringStrengthGreen(deringStrengthGreen)
                        .radiusBlue(profile.getRadiusBlue().doubleValue())
                        .amountBlue(amountBlue)
                        .iterationsBlue(iterationsBlue)
                        .clippingStrengthBlue(clippingStrengthBlue)
                        .clippingRangeBlue(100 - profile.getClippingRangeBlue())
                        .deringRadiusBlue(profile.getDeringRadiusBlue().doubleValue())
                        .deringStrengthBlue(deringStrengthBlue)
                        .blendRawRed(blendRaw)
                        .blendRawGreen(blendRawGreen)
                        .blendRawBlue(blendRawBlue)
                        .build();
            }

            LSWSharpenParameters parameters = LSWSharpenParameters.builder()
                    .includeBlue(profile.isLuminanceIncludeBlue())
                    .includeGreen(profile.isLuminanceIncludeGreen()) //
                    .includeRed(profile.isLuminanceIncludeRed())
                    .includeColor(profile.isLuminanceIncludeColor())
                    .saturation(1f)
                    .unsharpMaskParameters(usParams)
                    .mode(mode)
                    .build();
            if (mode == LSWSharpenMode.LUMINANCE && !validateLuminanceInclusion(parameters)) {
                log.warn("Attempt to exclude all channels from luminance sharpen!");
                parameters.setIncludeRed(true);
                parameters.setIncludeGreen(true);
                parameters.setIncludeBlue(true);
            }
            if (profile.getSharpenMode().equals(LSWSharpenMode.RGB.toString())
                    || !LswImageProcessingUtil.validateRGBStack(image)) {
                if (profile.getClippingStrength() > 0) {
                    applyRGBModeClippingPrevention(image, parameters.getUnsharpMaskParameters());
                    return true;
                } else {
                    applyRGBMode(image, parameters.getUnsharpMaskParameters());
                    return true;
                }
            } else {
                if (profile.getClippingStrength() > 0) {
                    applyLuminanceModeClippingPrevention(image, parameters);
                    return true;
                } else {
                    applyLuminanceMode(image, parameters);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isSlow() {
        return false;
    }

    @Override
    public boolean isApplied(Profile profile, ImagePlus image) {
        return profile.getApplyUnsharpMask().booleanValue()
                && profile.getRadius() != null
                && profile.getAmount() != null;
    }

    private boolean validateLuminanceInclusion(LSWSharpenParameters parameters) {
        return parameters.isIncludeRed() || parameters.isIncludeGreen() || parameters.isIncludeBlue();
    }

    public void applyRGBMode(ImagePlus image, final UnsharpMaskParameters unsharpMaskParameters) {
        ImageStack stack = image.getStack();
        try {

            // Run every stack in a seperate thread to increase performance.
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                CompletableFuture<?>[] futures = new CompletableFuture[3];
                futures[0] = CompletableFuture.runAsync(
                        () -> applyRGBModeToChannel(
                                stack,
                                unsharpMaskParameters.getRadiusRed(),
                                unsharpMaskParameters.getAmountRed(),
                                unsharpMaskParameters.getIterationsRed(),
                                unsharpMaskParameters.getBlendRawRed(),
                                unsharpMaskParameters.getDeringRadiusRed(),
                                unsharpMaskParameters.getDeringStrengthRed(),
                                1),
                        executor);
                futures[1] = CompletableFuture.runAsync(
                        () -> applyRGBModeToChannel(
                                stack,
                                unsharpMaskParameters.getRadiusGreen(),
                                unsharpMaskParameters.getAmountGreen(),
                                unsharpMaskParameters.getIterationsGreen(),
                                unsharpMaskParameters.getBlendRawGreen(),
                                unsharpMaskParameters.getDeringRadiusGreen(),
                                unsharpMaskParameters.getDeringStrengthGreen(),
                                2),
                        executor);
                futures[2] = CompletableFuture.runAsync(
                        () -> applyRGBModeToChannel(
                                stack,
                                unsharpMaskParameters.getRadiusBlue(),
                                unsharpMaskParameters.getAmountBlue(),
                                unsharpMaskParameters.getIterationsBlue(),
                                unsharpMaskParameters.getBlendRawBlue(),
                                unsharpMaskParameters.getDeringRadiusBlue(),
                                unsharpMaskParameters.getDeringStrengthBlue(),
                                3),
                        executor);
                CompletableFuture.allOf(futures).get();
            }
        } catch (InterruptedException | ExecutionException e) { // NOSONAR
            throw new FilterException(e.getMessage());
        }
    }

    public void applyRGBModeClippingPrevention(ImagePlus image, final UnsharpMaskParameters unsharpMaskParameters) {
        ImageStack finalStack = image.getStack();
        ImageStack intialStack = finalStack.duplicate();

        try {

            // Pass 1, first apply the filter normally to the intialStack.
            // Run every stack in a seperate thread to increase performance.
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                CompletableFuture<?>[] futures = new CompletableFuture[3];
                futures[0] = CompletableFuture.runAsync(
                        () -> applyRGBModeClippingPreventionToChannelPass1(
                                intialStack,
                                unsharpMaskParameters.getRadiusRed(),
                                unsharpMaskParameters.getAmountRed(),
                                unsharpMaskParameters.getIterationsRed(),
                                unsharpMaskParameters.getBlendRawRed(),
                                1),
                        executor);
                futures[1] = CompletableFuture.runAsync(
                        () -> applyRGBModeClippingPreventionToChannelPass1(
                                intialStack,
                                unsharpMaskParameters.getRadiusGreen(),
                                unsharpMaskParameters.getAmountGreen(),
                                unsharpMaskParameters.getIterationsGreen(),
                                unsharpMaskParameters.getBlendRawGreen(),
                                2),
                        executor);
                futures[2] = CompletableFuture.runAsync(
                        () -> applyRGBModeClippingPreventionToChannelPass1(
                                intialStack,
                                unsharpMaskParameters.getRadiusBlue(),
                                unsharpMaskParameters.getAmountBlue(),
                                unsharpMaskParameters.getIterationsBlue(),
                                unsharpMaskParameters.getBlendRawBlue(),
                                3),
                        executor);
                CompletableFuture.allOf(futures).get();
            }

            // Pass 2, apply the adaptive filter based on the result of pass 1.
            // Run every stack in a seperate thread to increase performance.
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                CompletableFuture<?>[] futures = new CompletableFuture[3];
                futures[0] = CompletableFuture.runAsync(
                        () -> doUnsharpMarkClippingPrevention(
                                unsharpMaskParameters.getIterationsRed(),
                                unsharpMaskParameters.getRadiusRed(),
                                unsharpMaskParameters.getAmountRed(),
                                unsharpMaskParameters.getClippingStrengthRed(),
                                unsharpMaskParameters.getClippingRangeRed(),
                                unsharpMaskParameters.getBlendRawRed(),
                                intialStack,
                                finalStack,
                                1),
                        executor);

                futures[1] = CompletableFuture.runAsync(
                        () -> doUnsharpMarkClippingPrevention(
                                unsharpMaskParameters.getIterationsGreen(),
                                unsharpMaskParameters.getRadiusGreen(),
                                unsharpMaskParameters.getAmountGreen(),
                                unsharpMaskParameters.getClippingStrengthGreen(),
                                unsharpMaskParameters.getClippingRangeGreen(),
                                unsharpMaskParameters.getBlendRawGreen(),
                                intialStack,
                                finalStack,
                                2),
                        executor);

                futures[2] = CompletableFuture.runAsync(
                        () -> doUnsharpMarkClippingPrevention(
                                unsharpMaskParameters.getIterationsBlue(),
                                unsharpMaskParameters.getRadiusBlue(),
                                unsharpMaskParameters.getAmountBlue(),
                                unsharpMaskParameters.getClippingStrengthBlue(),
                                unsharpMaskParameters.getClippingRangeBlue(),
                                unsharpMaskParameters.getBlendRawBlue(),
                                intialStack,
                                finalStack,
                                3),
                        executor);
                CompletableFuture.allOf(futures).get();
            }
        } catch (InterruptedException | ExecutionException e) { // NOSONAR
            throw new FilterException(e.getMessage());
        }
    }

    public void applyLuminanceMode(ImagePlus image, final LSWSharpenParameters parameters) {
        if (!parameters.isIncludeRed() && !parameters.isIncludeGreen() && !parameters.isIncludeBlue()) {
            log.error("Cannot have red, green and blue excluded");
            return;
        }
        UnsharpMaskParameters unsharpMaskParameters = parameters.getUnsharpMaskParameters();

        ImageStack finalStack = image.getStack();

        for (int it = 0; it < unsharpMaskParameters.getIterationsLuminance(); it++) {
            ImageProcessor ipRed = finalStack.getProcessor(1);
            ImageProcessor ipGreen = finalStack.getProcessor(2);
            ImageProcessor ipBlue = finalStack.getProcessor(3);

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
                float[] hsl = LswImageProcessingUtil.rgbToHsl(
                        pixelsRed[i],
                        pixelsGreen[i],
                        pixelsBlue[i],
                        parameters.isIncludeRed(),
                        parameters.isIncludeGreen(),
                        parameters.isIncludeBlue(),
                        parameters.isIncludeColor(),
                        parameters.getMode());
                pixelsHue[i] = hsl[0];
                pixelsSat[i] = hsl[1];
                pixelsLum[i] = hsl[2];
            }
            FloatProcessor fpLum = new FloatProcessor(image.getWidth(), image.getHeight(), pixelsLum);
            fpLum.snapshot();

            if (unsharpMaskParameters.getDeringStrengthLuminance() > 0.0f) {
                ImageProcessor ipLum = new ShortProcessor(image.getWidth(), image.getHeight());
                ipLum.setPixels(1, fpLum);
                final FloatProcessor fpMask = LswImageProcessingUtil.createDeringMaskFloatProcessor(
                        unsharpMaskParameters.getDeringRadiusLuminance(), 4, ipLum);
                ImageProcessor ipLumMask = new ShortProcessor(image.getWidth(), image.getHeight());
                ipLumMask.setPixels(1, fpMask);
                doUnsharpMaskDeringing(
                        unsharpMaskParameters.getRadiusLuminance(),
                        unsharpMaskParameters.getAmountLuminance(),
                        unsharpMaskParameters.getDeringStrengthLuminance(),
                        unsharpMaskParameters.getBlendRawLuminance(),
                        fpLum,
                        ipLumMask);
            } else {
                doUnsharpMask(
                        unsharpMaskParameters.getRadiusLuminance(),
                        unsharpMaskParameters.getAmountLuminance(),
                        unsharpMaskParameters.getBlendRawLuminance(),
                        fpLum);
            }

            for (int i = 0; i < pixelsRed.length; i++) {
                float[] rgb = LswImageProcessingUtil.hslToRgb(pixelsHue[i], pixelsSat[i], pixelsLum[i], 0f);
                pixelsRed[i] = rgb[0];
                pixelsGreen[i] = rgb[1];
                pixelsBlue[i] = rgb[2];
            }

            ipRed.setPixels(1, fpRed);
            ipGreen.setPixels(2, fpGreen);
            ipBlue.setPixels(3, fpBlue);
        }
    }

    public void applyLuminanceModeClippingPrevention(ImagePlus image, final LSWSharpenParameters parameters) {
        if (!parameters.isIncludeRed() && !parameters.isIncludeGreen() && !parameters.isIncludeBlue()) {
            log.error("Cannot have red, green and blue excluded");
            return;
        }
        UnsharpMaskParameters unsharpMaskParameters = parameters.getUnsharpMaskParameters();

        ImageStack finalStack = image.getStack();
        ImageStack intialStack = finalStack.duplicate();

        // Pass 1, first apply the filter normally to the intialStack.
        FloatProcessor fpLumInitial = null;
        for (int it = 0; it < unsharpMaskParameters.getIterationsLuminance(); it++) {
            ImageProcessor ipRed = intialStack.getProcessor(1);
            ImageProcessor ipGreen = intialStack.getProcessor(2);
            ImageProcessor ipBlue = intialStack.getProcessor(3);

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
                float[] hsl = LswImageProcessingUtil.rgbToHsl(
                        pixelsRed[i],
                        pixelsGreen[i],
                        pixelsBlue[i],
                        parameters.isIncludeRed(),
                        parameters.isIncludeGreen(),
                        parameters.isIncludeBlue(),
                        parameters.isIncludeColor(),
                        parameters.getMode());
                pixelsHue[i] = hsl[0];
                pixelsSat[i] = hsl[1];
                pixelsLum[i] = hsl[2];
            }
            fpLumInitial = new FloatProcessor(image.getWidth(), image.getHeight(), pixelsLum);
            fpLumInitial.snapshot();
            doUnsharpMask(
                    unsharpMaskParameters.getRadiusLuminance(),
                    unsharpMaskParameters.getAmountLuminance(),
                    unsharpMaskParameters.getBlendRawLuminance(),
                    fpLumInitial);

            for (int i = 0; i < pixelsRed.length; i++) {
                float[] rgb = LswImageProcessingUtil.hslToRgb(pixelsHue[i], pixelsSat[i], pixelsLum[i], 0f);
                pixelsRed[i] = rgb[0];
                pixelsGreen[i] = rgb[1];
                pixelsBlue[i] = rgb[2];
            }

            ipRed.setPixels(1, fpRed);
            ipGreen.setPixels(2, fpGreen);
            ipBlue.setPixels(3, fpBlue);
        }

        // Pass 2, apply the adaptive filter based on the result of pass 1.
        for (int it = 0; it < unsharpMaskParameters.getIterationsLuminance(); it++) {
            ImageProcessor ipRed = finalStack.getProcessor(1);
            ImageProcessor ipGreen = finalStack.getProcessor(2);
            ImageProcessor ipBlue = finalStack.getProcessor(3);

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
                float[] hsl = LswImageProcessingUtil.rgbToHsl(
                        pixelsRed[i],
                        pixelsGreen[i],
                        pixelsBlue[i],
                        parameters.isIncludeRed(),
                        parameters.isIncludeGreen(),
                        parameters.isIncludeBlue(),
                        parameters.isIncludeColor(),
                        parameters.getMode());
                pixelsHue[i] = hsl[0];
                pixelsSat[i] = hsl[1];
                pixelsLum[i] = hsl[2];
            }
            FloatProcessor fpLum = new FloatProcessor(image.getWidth(), image.getHeight(), pixelsLum);
            fpLum.snapshot();
            doUnsharpMaskClippingPrevention(
                    unsharpMaskParameters.getRadiusLuminance(),
                    unsharpMaskParameters.getAmountLuminance(),
                    unsharpMaskParameters.getClippingStrengthLuminance(),
                    unsharpMaskParameters.getClippingRangeLuminance(),
                    unsharpMaskParameters.getBlendRawLuminance(),
                    fpLumInitial,
                    fpLum);

            for (int i = 0; i < pixelsRed.length; i++) {
                float[] rgb = LswImageProcessingUtil.hslToRgb(pixelsHue[i], pixelsSat[i], pixelsLum[i], 0f);
                pixelsRed[i] = rgb[0];
                pixelsGreen[i] = rgb[1];
                pixelsBlue[i] = rgb[2];
            }

            ipRed.setPixels(1, fpRed);
            ipGreen.setPixels(2, fpGreen);
            ipBlue.setPixels(3, fpBlue);
        }
    }

    /*
     * Unsharp mask algorithm.
     * */
    public void doUnsharpMask(double radius, float amount, float blendRawFactor, FloatProcessor fp) {
        GaussianBlur gb = new GaussianBlur();
        gb.blurGaussian(fp, radius, radius, 0.01);
        float[] pixels = (float[]) fp.getPixels();
        float[] snapshotPixels = (float[]) fp.getSnapshotPixels();
        int width = fp.getWidth();
        Rectangle roi = fp.getRoi();
        for (int y = roi.y; y < roi.y + roi.height; y++) {
            for (int x = roi.x, p = width * y + x; x < roi.x + roi.width; x++, p++) {
                pixels[p] = getUnsharpMaskValue(snapshotPixels[p], pixels[p], amount, blendRawFactor);
            }
        }
    }

    private void applyRGBModeToChannel(
            ImageStack stack,
            double radius,
            float amount,
            int iterations,
            float blendRawFactor,
            double deringRadius,
            float deringStrength,
            int channel) {
        final ImageProcessor ipMask = LswImageProcessingUtil.createDeringMaskProcessor(
                deringStrength, deringRadius, 4, stack.getProcessor(channel));
        for (int i = 0; i < iterations; i++) {
            ImageProcessor ip = stack.getProcessor(channel);
            FloatProcessor fp = ip.toFloat(channel, null);
            fp.snapshot();
            if (ipMask == null) {
                doUnsharpMask(radius, amount, blendRawFactor, fp);
            } else {
                doUnsharpMaskDeringing(radius, amount, deringStrength, blendRawFactor, fp, ipMask);
            }
            ip.setPixels(channel, fp);
        }
    }

    private void applyRGBModeClippingPreventionToChannelPass1(
            ImageStack initialStack, double radius, float amount, int iterations, float blendRawFactor, int channel) {
        for (int i = 0; i < iterations; i++) {
            ImageProcessor ip = initialStack.getProcessor(channel);
            FloatProcessor fp = ip.toFloat(channel, null);
            fp.snapshot();
            doUnsharpMask(radius, amount, blendRawFactor, fp);
            ip.setPixels(channel, fp);
        }
    }

    private void doUnsharpMarkClippingPrevention(
            int iterations,
            double radius,
            float amount,
            float clippingStrength,
            float clippingRange,
            float blendRawFactor,
            ImageStack intialStack,
            ImageStack finalStack,
            int channel) {
        for (int i = 0; i < iterations; i++) {
            ImageProcessor ipInitial = intialStack.getProcessor(channel);
            FloatProcessor fpInitial = ipInitial.toFloat(channel, null);
            ImageProcessor ipFinal = finalStack.getProcessor(channel);
            FloatProcessor fpFinal = ipFinal.toFloat(channel, null);
            fpFinal.snapshot();
            doUnsharpMaskClippingPrevention(
                    radius, amount, clippingStrength, clippingRange, blendRawFactor, fpInitial, fpFinal);
            ipFinal.setPixels(channel, fpFinal);
        }
    }

    /*
     * Unsharp mask algorithm that prevents edge clipping effect.
     * @param radius
     * @param amount
     * @param clippingStrength the factor of the clipping suppression, if set to 0 it means no clipping supression is being applied.
     * @param clippingRange represents the histogram % value from where the clipping has to be suppressed by holding off the amount.
     * @param fp
     */
    private void doUnsharpMaskClippingPrevention(
            double radius,
            float amount,
            float clippingStrength,
            float clippingRange,
            float blendRawFactor,
            FloatProcessor fpInitial,
            FloatProcessor fpFinal) {
        GaussianBlur gb = new GaussianBlur();
        gb.blurGaussian(fpFinal, radius, radius, 0.01);
        float[] initialPixels = (float[]) fpInitial.getPixels();
        float[] pixels = (float[]) fpFinal.getPixels();
        float[] snapshotPixels = (float[]) fpFinal.getSnapshotPixels();
        int width = fpFinal.getWidth();
        Rectangle roi = fpFinal.getRoi();
        for (int y = roi.y; y < roi.y + roi.height; y++) {
            for (int x = roi.x, p = width * y + x; x < roi.x + roi.width; x++, p++) {
                int cutoffIndex = Math.round(
                        ((initialPixels[p] < 0f ? 0f : initialPixels[p]) / FLOAT_MAX_SATURATED_VALUE) * 100f);
                float cutoffFactor = getCutoffFactor(cutoffIndex, clippingRange) * clippingStrength;
                float amountNew = amount - (amount * (cutoffFactor / 100));
                float pixelValueNew = getUnsharpMaskValue(snapshotPixels[p], pixels[p], amountNew, blendRawFactor);
                pixels[p] = pixelValueNew;
            }
        }
    }

    /*
     * Unsharp mask algorithm that prevents ring effects at sharp planet edges (Jupiter, Mars, Venus).
     */
    private void doUnsharpMaskDeringing(
            double radius,
            float amount,
            float deringStrength,
            float blendRawFactor,
            FloatProcessor fp,
            ImageProcessor ipMask) {
        GaussianBlur gb = new GaussianBlur();
        gb.blurGaussian(fp, radius, radius, 0.01);
        float[] pixels = (float[]) fp.getPixels();
        short[] maskPixels = (short[]) ipMask.getPixels();
        float[] snapshotPixels = (float[]) fp.getSnapshotPixels();
        int width = fp.getWidth();
        Rectangle roi = fp.getRoi();
        for (int y = roi.y; y < roi.y + roi.height; y++) {
            for (int x = roi.x, p = width * y + x; x < roi.x + roi.width; x++, p++) {
                int maskValue = LswImageProcessingUtil.convertToUnsignedInt(maskPixels[p]);
                float cutoffFactor = (Constants.MAX_INT_VALUE - maskValue) / (Constants.MAX_INT_VALUE / 100);
                float amountNew = amount - (amount * ((cutoffFactor / 100) * deringStrength));
                float pixelValueNew = getUnsharpMaskValue(snapshotPixels[p], pixels[p], amountNew, blendRawFactor);
                pixels[p] = pixelValueNew;
            }
        }
    }

    /*
     * Calculates the cutoff value from 0..100 on a scale off 0..100 for index. The histogramValueFrom is the value between 0..100 where
     * the cutoff needs to start. The cutoff is exponential with exponent 2.
     */
    private float getCutoffFactor(int index, float histogramValueFrom) {
        float intervalSize = 100 - histogramValueFrom;
        float stepSize = 100 / intervalSize;
        float distance = index - histogramValueFrom;
        distance = distance < 0 ? 0 : distance;
        return (float) (Math.pow(distance * stepSize, 2) * 0.01);
    }

    private float getUnsharpMaskValue(float pixelValue, float pixelValueAfterBlur, float amount, float blendRawFactor) {
        return ((pixelValue - amount * pixelValueAfterBlur) / (1f - amount) * (1f - blendRawFactor))
                + (pixelValue * blendRawFactor);
    }
}
