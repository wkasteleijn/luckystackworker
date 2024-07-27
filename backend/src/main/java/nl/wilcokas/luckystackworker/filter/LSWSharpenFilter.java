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

        final ImageStack maskStack = createDeringMaskStack(unsharpMaskParameters, stack);

        // TODO: finish


        for (int i = 0; i < unsharpMaskParameters.getIterationsLuminance(); i++) {

            // Run every stack in a seperate thread to increase performance.
            Executor executor = LswUtil.getParallelExecutor();
            for (int slice = 1; slice <= stack.getSize(); slice++) {
                int finalLayer = slice;
                executor.execute(() -> {
                    ImageProcessor ip = stack.getProcessor(finalLayer);
                    FloatProcessor fp = ip.toFloat(finalLayer, null);
                    fp.snapshot();
                    if (maskStack == null) {
                        doUnsharpMask(unsharpMaskParameters.getRadiusLuminance(), unsharpMaskParameters.getAmountLuminance(), fp);
                    } else {
                        ImageProcessor ipMask = maskStack.getProcessor(finalLayer);
                        doUnsharpMaskDeringing(unsharpMaskParameters.getRadiusLuminance(), unsharpMaskParameters.getAmountLuminance(),
                                unsharpMaskParameters.getDeringStrengthLuminance(), fp, ipMask);
                    }
                    ip.setPixels(finalLayer, fp);
                });
            }
            LswUtil.stopAndAwaitParallelExecutor(executor);
        }
    }

    public void applyRGBModeClippingPrevention(ImagePlus image, final UnsharpMaskParameters unsharpMaskParameters) {
        ImageStack finalStack = image.getStack();
        ImageStack intialStack = finalStack.duplicate();

        // Pass 1, first apply the filter normally to the intialStack.
        // Run every stack in a seperate thread to increase performance.
        Executor executor = LswUtil.getParallelExecutor();

        executor.execute(() -> {
            for (int i = 0; i < unsharpMaskParameters.getIterationsRed(); i++) {
                ImageProcessor ip = intialStack.getProcessor(1);
                FloatProcessor fp = ip.toFloat(1, null);
                fp.snapshot();
                doUnsharpMask(unsharpMaskParameters.getRadiusRed(), unsharpMaskParameters.getAmountRed(), fp);
                ip.setPixels(1, fp);
            }
        });

        executor.execute(() -> {
            for (int i = 0; i < unsharpMaskParameters.getIterationsGreen(); i++) {
                ImageProcessor ip = intialStack.getProcessor(2);
                FloatProcessor fp = ip.toFloat(2, null);
                fp.snapshot();
                doUnsharpMask(unsharpMaskParameters.getRadiusGreen(), unsharpMaskParameters.getAmountGreen(), fp);
                ip.setPixels(2, fp);
            }
        });

        executor.execute(() -> {
            for (int i = 0; i < unsharpMaskParameters.getIterationsBlue(); i++) {
                ImageProcessor ip = intialStack.getProcessor(3);
                FloatProcessor fp = ip.toFloat(3, null);
                fp.snapshot();
                doUnsharpMask(unsharpMaskParameters.getRadiusBlue(), unsharpMaskParameters.getAmountBlue(), fp);
                ip.setPixels(3, fp);
            }
        });

        LswUtil.stopAndAwaitParallelExecutor(executor);

        // Pass 2, apply the adaptive filter based on the result of pass 1.
        // Run every stack in a seperate thread to increase performance.
        executor = LswUtil.getParallelExecutor();

        executor.execute(() -> {
            for (int i = 0; i < unsharpMaskParameters.getIterationsRed(); i++) {
                ImageProcessor ipInitial = intialStack.getProcessor(1);
                FloatProcessor fpInitial = ipInitial.toFloat(1, null);
                ImageProcessor ipFinal = finalStack.getProcessor(1);
                FloatProcessor fpFinal = ipFinal.toFloat(1, null);
                fpFinal.snapshot();
                doUnsharpMaskClippingPrevention(unsharpMaskParameters.getRadiusRed(), unsharpMaskParameters.getAmountRed(),
                        unsharpMaskParameters.getClippingStrengthRed(),
                        unsharpMaskParameters.getClippingRangeRed(), fpInitial, fpFinal);
                ipFinal.setPixels(1, fpFinal);
            }
        });

        executor.execute(() -> {
            for (int i = 0; i < unsharpMaskParameters.getIterationsGreen(); i++) {
                ImageProcessor ipInitial = intialStack.getProcessor(2);
                FloatProcessor fpInitial = ipInitial.toFloat(2, null);
                ImageProcessor ipFinal = finalStack.getProcessor(2);
                FloatProcessor fpFinal = ipFinal.toFloat(2, null);
                fpFinal.snapshot();
                doUnsharpMaskClippingPrevention(unsharpMaskParameters.getRadiusGreen(), unsharpMaskParameters.getAmountGreen(),
                        unsharpMaskParameters.getClippingStrengthGreen(),
                        unsharpMaskParameters.getClippingRangeGreen(), fpInitial, fpFinal);
                ipFinal.setPixels(2, fpFinal);
            }
        });

        executor.execute(() -> {
            for (int i = 0; i < unsharpMaskParameters.getIterationsBlue(); i++) {
                ImageProcessor ipInitial = intialStack.getProcessor(3);
                FloatProcessor fpInitial = ipInitial.toFloat(3, null);
                ImageProcessor ipFinal = finalStack.getProcessor(3);
                FloatProcessor fpFinal = ipFinal.toFloat(3, null);
                fpFinal.snapshot();
                doUnsharpMaskClippingPrevention(unsharpMaskParameters.getRadiusBlue(), unsharpMaskParameters.getAmountBlue(),
                        unsharpMaskParameters.getClippingStrengthBlue(),
                        unsharpMaskParameters.getClippingRangeBlue(), fpInitial, fpFinal);
                ipFinal.setPixels(3, fpFinal);
            }
        });

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
                float[] hsl = LswImageProcessingUtil.rgbToHsl(pixelsRed[i], pixelsGreen[i], pixelsBlue[i], parameters.isIncludeRed(),
                        parameters.isIncludeGreen(),
                        parameters.isIncludeBlue(), parameters.isIncludeColor(), parameters.getMode());
                pixelsHue[i] = hsl[0];
                pixelsSat[i] = hsl[1];
                pixelsLum[i] = hsl[2];
            }
            FloatProcessor fpLum = new FloatProcessor(image.getWidth(), image.getHeight(), pixelsLum);
            fpLum.snapshot();

            if (unsharpMaskParameters.getDeringStrengthLuminance() > 0.0f) {
                ImageProcessor ipLum = new ShortProcessor(image.getWidth(), image.getHeight());
                ipLum.setPixels(1, fpLum);
                final FloatProcessor fpMask = createDeringMaskStack(unsharpMaskParameters.getDeringRadiusLuminance(),
                        unsharpMaskParameters.getDeringThresholdLuminance(), ipLum);
                ImageProcessor ipLumMask = new ShortProcessor(image.getWidth(), image.getHeight());
                ipLumMask.setPixels(1, fpMask);
                doUnsharpMaskDeringing(unsharpMaskParameters.getRadiusLuminance(), unsharpMaskParameters.getAmountLuminance(),
                        unsharpMaskParameters.getDeringStrengthLuminance(), fpLum, ipLumMask);
            } else {
                doUnsharpMask(unsharpMaskParameters.getRadiusLuminance(), unsharpMaskParameters.getAmountLuminance(), fpLum);
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
                float[] hsl = LswImageProcessingUtil.rgbToHsl(pixelsRed[i], pixelsGreen[i], pixelsBlue[i], parameters.isIncludeRed(),
                        parameters.isIncludeGreen(),
                        parameters.isIncludeBlue(), parameters.isIncludeColor(), parameters.getMode());
                pixelsHue[i] = hsl[0];
                pixelsSat[i] = hsl[1];
                pixelsLum[i] = hsl[2];
            }
            fpLumInitial = new FloatProcessor(image.getWidth(), image.getHeight(), pixelsLum);
            fpLumInitial.snapshot();
            doUnsharpMask(unsharpMaskParameters.getRadiusLuminance(), unsharpMaskParameters.getAmountLuminance(), fpLumInitial);

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
                float[] hsl = LswImageProcessingUtil.rgbToHsl(pixelsRed[i], pixelsGreen[i], pixelsBlue[i], parameters.isIncludeRed(),
                        parameters.isIncludeGreen(),
                        parameters.isIncludeBlue(), parameters.isIncludeColor(), parameters.getMode());
                pixelsHue[i] = hsl[0];
                pixelsSat[i] = hsl[1];
                pixelsLum[i] = hsl[2];
            }
            FloatProcessor fpLum = new FloatProcessor(image.getWidth(), image.getHeight(), pixelsLum);
            fpLum.snapshot();
            doUnsharpMaskClippingPrevention(unsharpMaskParameters.getRadiusLuminance(), unsharpMaskParameters.getAmountLuminance(), unsharpMaskParameters.getClippingStrengthLuminance(),
                    unsharpMaskParameters.getClippingRangeLuminance(), fpLumInitial, fpLum);

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

    private ImageStack createDeringMaskStack(final UnsharpMaskParameters unsharpMaskParameters, ImageStack originalStack) {
        if (unsharpMaskParameters.getDeringStrengthLuminance() > 0.0f) {
            ImageStack maskStack = originalStack.duplicate();
            double radius = unsharpMaskParameters.getDeringRadiusLuminance();

            Executor executor = LswUtil.getParallelExecutor();
            for (int layer = 1; layer <= maskStack.size(); layer++) {
                int finalLayer = layer;
                executor.execute(() -> {
                    ImageProcessor ip = maskStack.getProcessor(finalLayer);
                    FloatProcessor fp = createDeringMaskStack(radius, unsharpMaskParameters.getDeringThresholdLuminance(), ip);
                    ip.setPixels(1, fp);
                });
            }
            LswUtil.stopAndAwaitParallelExecutor(executor);
            return maskStack;
        }
        return null;
    }

    private FloatProcessor createDeringMaskStack(double radius, int threshold, ImageProcessor ip) {
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
    private void doUnsharpMask(double radius, float amount, FloatProcessor fp) {
        GaussianBlur gb = new GaussianBlur();
        gb.blurGaussian(fp, radius, radius, 0.01);
        float[] pixels = (float[]) fp.getPixels();
        float[] snapshotPixels = (float[]) fp.getSnapshotPixels();
        int width = fp.getWidth();
        Rectangle roi = fp.getRoi();
        for (int y = roi.y; y < roi.y + roi.height; y++) {
            for (int x = roi.x, p = width * y + x; x < roi.x + roi.width; x++, p++) {
                pixels[p] = getUnsharpMaskValue(snapshotPixels[p], pixels[p], amount);
            }
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
    private void doUnsharpMaskClippingPrevention(double radius, float amount, float clippingStrength, float clippingRange, FloatProcessor fpInitial,
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
                int cutoffIndex = Math.round(((initialPixels[p] < 0f ? 0f : initialPixels[p]) / FLOAT_MAX_SATURATED_VALUE) * 100f);
                float cutoffFactor = getCutoffFactor(cutoffIndex, clippingRange) * clippingStrength;
                float amountNew = amount - (amount * (cutoffFactor / 100));
                float pixelValueNew = getUnsharpMaskValue(snapshotPixels[p], pixels[p], amountNew);
                pixels[p] = pixelValueNew;
            }
        }
    }

    /*
     * Unsharp mask algorithm that prevents ring effects at sharp planet edges (Jupiter, Mars, Venus).
     * @param radius
     * @param amount
     * @param deringStrength the factor of the deringing, if set to 0 it means no deringing is being applied.
     * @param fp
     * @param fpMask the processor layer that serves as a mask from where the sharpen amount cutoff factor is determined
     */
    private void doUnsharpMaskDeringing(double radius, float amount, float deringStrength, FloatProcessor fp,
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
                float pixelValueNew = getUnsharpMaskValue(snapshotPixels[p], pixels[p], amountNew);
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

    private float getUnsharpMaskValue(float pixelValue, float pixelValueAfterBlur, float amount) {
        return (pixelValue - amount * pixelValueAfterBlur) / (1f - amount);
    }
}
