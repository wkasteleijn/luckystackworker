package nl.wilcokas.luckystackworker.filter;

import java.awt.Rectangle;
import java.util.concurrent.Executor;

import org.springframework.stereotype.Component;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.GaussianBlur;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.filter.settings.LSWSharpenParameters;
import nl.wilcokas.luckystackworker.filter.settings.UnsharpMaskParameters;
import nl.wilcokas.luckystackworker.util.LswImageProcessingUtil;
import nl.wilcokas.luckystackworker.util.LswUtil;

@Slf4j
@Component
public class LSWSharpenFilter {

    private static final float FLOAT_MAX_SATURATED_VALUE = 65535f;

    public void applyRGBMode(ImagePlus image, final UnsharpMaskParameters unsharpMaskParameters) {
        ImageStack stack = image.getStack();

        // Run every stack in a seperate thread to increase performance.
        Executor executor = LswUtil.getParallelExecutor();
        executor.execute(() -> applyRGBModeToChannel(stack,
                unsharpMaskParameters.getRadiusRed(),
                unsharpMaskParameters.getAmountRed(),
                unsharpMaskParameters.getIterationsRed(),
                unsharpMaskParameters.getBlendRawRed(),
                unsharpMaskParameters.getDeringRadiusRed(),
                unsharpMaskParameters.getDeringStrengthRed(),
                unsharpMaskParameters.getDeringThresholdRed(),
                1));
        executor.execute(() -> applyRGBModeToChannel(stack,
                unsharpMaskParameters.getRadiusGreen(),
                unsharpMaskParameters.getAmountGreen(),
                unsharpMaskParameters.getIterationsGreen(),
                unsharpMaskParameters.getBlendRawGreen(),
                unsharpMaskParameters.getDeringRadiusGreen(),
                unsharpMaskParameters.getDeringStrengthGreen(),
                unsharpMaskParameters.getDeringThresholdGreen(),
                2));
        executor.execute(() -> applyRGBModeToChannel(stack,
                unsharpMaskParameters.getRadiusBlue(),
                unsharpMaskParameters.getAmountBlue(),
                unsharpMaskParameters.getIterationsBlue(),
                unsharpMaskParameters.getBlendRawBlue(),
                unsharpMaskParameters.getDeringRadiusBlue(),
                unsharpMaskParameters.getDeringStrengthBlue(),
                unsharpMaskParameters.getDeringThresholdBlue(),
                3));
        LswUtil.stopAndAwaitParallelExecutor(executor);
    }

    public void applyRGBModeClippingPrevention(ImagePlus image, final UnsharpMaskParameters unsharpMaskParameters) {
        ImageStack finalStack = image.getStack();
        ImageStack intialStack = finalStack.duplicate();

        // Pass 1, first apply the filter normally to the intialStack.
        // Run every stack in a seperate thread to increase performance.
        Executor executor = LswUtil.getParallelExecutor();
        executor.execute(() -> applyRGBModeClippingPreventionToChannelPass1(intialStack,
                unsharpMaskParameters.getRadiusRed(),
                unsharpMaskParameters.getAmountRed(),
                unsharpMaskParameters.getIterationsRed(),
                unsharpMaskParameters.getBlendRawRed(),
                1));
        executor.execute(() -> applyRGBModeClippingPreventionToChannelPass1(intialStack,
                unsharpMaskParameters.getRadiusGreen(),
                unsharpMaskParameters.getAmountGreen(),
                unsharpMaskParameters.getIterationsGreen(),
                unsharpMaskParameters.getBlendRawGreen(),
                2));
        executor.execute(() -> applyRGBModeClippingPreventionToChannelPass1(intialStack,
                unsharpMaskParameters.getRadiusBlue(),
                unsharpMaskParameters.getAmountBlue(),
                unsharpMaskParameters.getIterationsBlue(),
                unsharpMaskParameters.getBlendRawBlue(),
                3));
        LswUtil.stopAndAwaitParallelExecutor(executor);

        // Pass 2, apply the adaptive filter based on the result of pass 1.
        // Run every stack in a seperate thread to increase performance.
        executor = LswUtil.getParallelExecutor();

        executor.execute(() -> doUnsharpMarkClippingPrevention(unsharpMaskParameters.getIterationsRed(),
                unsharpMaskParameters.getRadiusRed(),
                unsharpMaskParameters.getAmountRed(),
                unsharpMaskParameters.getClippingStrengthRed(),
                unsharpMaskParameters.getClippingRangeRed(),
                unsharpMaskParameters.getBlendRawRed(),
                intialStack,
                finalStack, 1));

        executor.execute(() -> doUnsharpMarkClippingPrevention(unsharpMaskParameters.getIterationsGreen(),
                unsharpMaskParameters.getRadiusGreen(),
                unsharpMaskParameters.getAmountGreen(),
                unsharpMaskParameters.getClippingStrengthGreen(),
                unsharpMaskParameters.getClippingRangeGreen(),
                unsharpMaskParameters.getBlendRawGreen(),
                intialStack,
                finalStack, 2));

        executor.execute(() -> doUnsharpMarkClippingPrevention(unsharpMaskParameters.getIterationsBlue(),
                unsharpMaskParameters.getRadiusBlue(),
                unsharpMaskParameters.getAmountBlue(),
                unsharpMaskParameters.getClippingStrengthBlue(),
                unsharpMaskParameters.getClippingRangeBlue(),
                unsharpMaskParameters.getBlendRawBlue(),
                intialStack,
                finalStack, 3));
        LswUtil.stopAndAwaitParallelExecutor(executor);
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
                float[] hsl = LswImageProcessingUtil.rgbToHsl(pixelsRed[i],
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
                final FloatProcessor fpMask = createDeringMaskFloatProcessor(unsharpMaskParameters.getDeringRadiusLuminance(),
                        unsharpMaskParameters.getDeringThresholdLuminance(),
                        ipLum);
                ImageProcessor ipLumMask = new ShortProcessor(image.getWidth(), image.getHeight());
                ipLumMask.setPixels(1, fpMask);
                doUnsharpMaskDeringing(unsharpMaskParameters.getRadiusLuminance(),
                        unsharpMaskParameters.getAmountLuminance(),
                        unsharpMaskParameters.getDeringStrengthLuminance(),
                        unsharpMaskParameters.getBlendRawLuminance(),
                        fpLum,
                        ipLumMask);
            } else {
                doUnsharpMask(unsharpMaskParameters.getRadiusLuminance(),
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
                float[] hsl = LswImageProcessingUtil.rgbToHsl(pixelsRed[i],
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
            doUnsharpMask(unsharpMaskParameters.getRadiusLuminance(),
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
                float[] hsl = LswImageProcessingUtil.rgbToHsl(pixelsRed[i],
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
            doUnsharpMaskClippingPrevention(unsharpMaskParameters.getRadiusLuminance(),
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

    private void applyRGBModeToChannel(
            ImageStack stack,
            double radius,
            float amount,
            int iterations,
            float blendRawFactor,
            double deringRadius,
            float deringStrength,
            int deringThreshold,
            int channel) {
        final ImageProcessor ipMask = createDeringMaskProcessor(deringStrength,
                deringRadius,
                deringThreshold,
                stack.getProcessor(channel));
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

    private ImageProcessor createDeringMaskProcessor(
            float deringStrength, double deringRadius, int deringThreshold, ImageProcessor ip) {
        if (deringStrength > 0.0f) {
            ImageProcessor maskIp = ip.duplicate();
            FloatProcessor fp = createDeringMaskFloatProcessor(deringRadius, deringThreshold, maskIp);
            maskIp.setPixels(1, fp);
            return maskIp;
        }
        return null;
    }

    private FloatProcessor createDeringMaskFloatProcessor(double radius, int threshold, ImageProcessor ip) {
        int minValue = 65535;
        int maxValue = 0;
        short[] maskPixels = (short[]) ip.getPixels();
        for (int position = 0; position < maskPixels.length; position++) {
            int value = LswImageProcessingUtil.convertToUnsignedInt(maskPixels[position]);
            if (value > maxValue) {
                maxValue = value;
            }
            if (value < minValue) {
                minValue = value;
            }
        }
        int average = (maxValue - minValue) / threshold; // start cutting of from threshold times the average.
        for (int position = 0; position < maskPixels.length; position++) {
            int value = LswImageProcessingUtil.convertToUnsignedInt(maskPixels[position]);
            if (value < average) {
                maskPixels[position] = 0;
            } else if (value > average) {
                maskPixels[position] = -1;
            }
        }
        FloatProcessor fp = ip.toFloat(1, null);
        GaussianBlur gb = new GaussianBlur();
        gb.blurGaussian(fp, radius, radius, 0.01);
        return fp;
    }

    /*
     * Unsharp mask algorithm.
     * @param radius
     * @param amount
     * @param clippingStrength the factor of the clipping suppression, if set to 0 it means no clipping supression is being applied.
     * @param clippingRange represents the histogram % value from where the clipping has to be suppressed by holding off the amount.
     * @param fp
     */
    private void doUnsharpMask(double radius, float amount, float blendRawFactor, FloatProcessor fp) {
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
            doUnsharpMaskClippingPrevention(radius, amount, clippingStrength, clippingRange, blendRawFactor, fpInitial, fpFinal);
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
            double radius, float amount, float deringStrength, float blendRawFactor, FloatProcessor fp, ImageProcessor ipMask) {
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
        return ((pixelValue - amount * pixelValueAfterBlur) / (1f - amount) * (1f - blendRawFactor)) + (pixelValue * blendRawFactor);
    }
}
