package nl.wilcokas.luckystackworker.filter;

import java.awt.Rectangle;

import org.springframework.stereotype.Component;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.GaussianBlur;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.filter.settings.LSWSharpenParameters;
import nl.wilcokas.luckystackworker.filter.settings.UnsharpMaskParameters;
import nl.wilcokas.luckystackworker.util.Util;

@Slf4j
@Component
public class LSWSharpenFilter {

    private static final float FLOAT_MAX_SATURATED_VALUE = 65535f;

    public void applyRGBMode(ImagePlus image, final UnsharpMaskParameters unsharpMaskParameters) {
        ImageStack finalStack = image.getStack();
        for (int i = 0; i < unsharpMaskParameters.getIterations(); i++) {
            for (int slice = 1; slice <= finalStack.getSize(); slice++) {
                ImageProcessor ipFinal = finalStack.getProcessor(slice);
                FloatProcessor fpFinal = ipFinal.toFloat(slice, null);
                fpFinal.snapshot();
                doUnsharpMask(unsharpMaskParameters.getRadius(), unsharpMaskParameters.getAmount(), unsharpMaskParameters.getClippingStrength(),
                        unsharpMaskParameters.getClippingRange(), fpFinal);
                ipFinal.setPixels(slice, fpFinal);
            }

        }
        // image.updateAndDraw();
    }

    public void applyRGBModeAdaptive(ImagePlus image, final UnsharpMaskParameters unsharpMaskParameters) {
        ImageStack finalStack = image.getStack();
        ImageStack intialStack = finalStack.duplicate();

        // Pass 1, first apply the filter normally to the intialStack.
        for (int i = 0; i < unsharpMaskParameters.getIterations(); i++) {
            for (int slice = 1; slice <= intialStack.getSize(); slice++) {
                ImageProcessor ip = intialStack.getProcessor(slice);
                FloatProcessor fp = ip.toFloat(slice, null);
                fp.snapshot();
                doUnsharpMask(unsharpMaskParameters.getRadius(), unsharpMaskParameters.getAmount(), unsharpMaskParameters.getClippingStrength(),
                        unsharpMaskParameters.getClippingRange(), fp);
                ip.setPixels(slice, fp);
            }

        }

        // Pass 2, apply the adaptive filter based on the result of pass 1.
        for (int i = 0; i < unsharpMaskParameters.getIterations(); i++) {
            for (int slice = 1; slice <= intialStack.getSize(); slice++) {
                ImageProcessor ipInitial = intialStack.getProcessor(slice);
                FloatProcessor fpInitial = ipInitial.toFloat(slice, null);
                ImageProcessor ipFinal = finalStack.getProcessor(slice);
                FloatProcessor fpFinal = ipFinal.toFloat(slice, null);
                fpFinal.snapshot();
                doUnsharpMaskAdaptive(unsharpMaskParameters.getRadius(), unsharpMaskParameters.getAmount(),
                        unsharpMaskParameters.getClippingStrength(),
                        unsharpMaskParameters.getClippingRange(), fpInitial, fpFinal);
                ipFinal.setPixels(slice, fpFinal);
            }

        }

        // image.updateAndDraw();
    }

    public void applyLuminanceMode(ImagePlus image, final LSWSharpenParameters parameters) {
        if (!parameters.isIncludeRed() && !parameters.isIncludeGreen() && !parameters.isIncludeBlue()) {
            log.error("Cannot have red, green and blue excluded");
            return;
        }
        UnsharpMaskParameters unsharpMaskParameters = parameters.getUnsharpMaskParameters();

        ImageStack finalStack = image.getStack();

        for (int it = 0; it < unsharpMaskParameters.getIterations(); it++) {
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
                float[] hsl = Util.rgbToHsl(pixelsRed[i], pixelsGreen[i], pixelsBlue[i], parameters.isIncludeRed(), parameters.isIncludeGreen(),
                        parameters.isIncludeBlue(), parameters.getMode());
                pixelsHue[i] = hsl[0];
                pixelsSat[i] = hsl[1];
                pixelsLum[i] = hsl[2];
            }
            FloatProcessor fpLum = new FloatProcessor(image.getWidth(), image.getHeight(), pixelsLum);
            fpLum.snapshot();
            doUnsharpMask(unsharpMaskParameters.getRadius(), unsharpMaskParameters.getAmount(), unsharpMaskParameters.getClippingStrength(),
                    unsharpMaskParameters.getClippingRange(), fpLum);

            for (int i = 0; i < pixelsRed.length; i++) {
                float[] rgb = Util.hslToRgb(pixelsHue[i], pixelsSat[i], pixelsLum[i], 0f);
                pixelsRed[i] = rgb[0];
                pixelsGreen[i] = rgb[1];
                pixelsBlue[i] = rgb[2];
            }

            ipRed.setPixels(1, fpRed);
            ipGreen.setPixels(2, fpGreen);
            ipBlue.setPixels(3, fpBlue);
        }

        // image.updateAndDraw();
    }

    public void applyLuminanceModeAdaptive(ImagePlus image, final LSWSharpenParameters parameters) {
        if (!parameters.isIncludeRed() && !parameters.isIncludeGreen() && !parameters.isIncludeBlue()) {
            log.error("Cannot have red, green and blue excluded");
            return;
        }
        UnsharpMaskParameters unsharpMaskParameters = parameters.getUnsharpMaskParameters();

        ImageStack finalStack = image.getStack();
        ImageStack intialStack = finalStack.duplicate();

        // Pass 1, first apply the filter normally to the intialStack.
        FloatProcessor fpLumInitial = null;
        for (int it = 0; it < unsharpMaskParameters.getIterations(); it++) {
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
                float[] hsl = Util.rgbToHsl(pixelsRed[i], pixelsGreen[i], pixelsBlue[i], parameters.isIncludeRed(), parameters.isIncludeGreen(),
                        parameters.isIncludeBlue(), parameters.getMode());
                pixelsHue[i] = hsl[0];
                pixelsSat[i] = hsl[1];
                pixelsLum[i] = hsl[2];
            }
            fpLumInitial = new FloatProcessor(image.getWidth(), image.getHeight(), pixelsLum);
            fpLumInitial.snapshot();
            doUnsharpMask(unsharpMaskParameters.getRadius(), unsharpMaskParameters.getAmount(), unsharpMaskParameters.getClippingStrength(),
                    unsharpMaskParameters.getClippingRange(), fpLumInitial);

            for (int i = 0; i < pixelsRed.length; i++) {
                float[] rgb = Util.hslToRgb(pixelsHue[i], pixelsSat[i], pixelsLum[i], 0f);
                pixelsRed[i] = rgb[0];
                pixelsGreen[i] = rgb[1];
                pixelsBlue[i] = rgb[2];
            }

            ipRed.setPixels(1, fpRed);
            ipGreen.setPixels(2, fpGreen);
            ipBlue.setPixels(3, fpBlue);
        }

        // Pass 2, apply the adaptive filter based on the result of pass 1.
        for (int it = 0; it < unsharpMaskParameters.getIterations(); it++) {
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
                float[] hsl = Util.rgbToHsl(pixelsRed[i], pixelsGreen[i], pixelsBlue[i], parameters.isIncludeRed(), parameters.isIncludeGreen(),
                        parameters.isIncludeBlue(), parameters.getMode());
                pixelsHue[i] = hsl[0];
                pixelsSat[i] = hsl[1];
                pixelsLum[i] = hsl[2];
            }
            FloatProcessor fpLum = new FloatProcessor(image.getWidth(), image.getHeight(), pixelsLum);
            fpLum.snapshot();
            doUnsharpMaskAdaptive(unsharpMaskParameters.getRadius(), unsharpMaskParameters.getAmount(), unsharpMaskParameters.getClippingStrength(),
                    unsharpMaskParameters.getClippingRange(), fpLumInitial, fpLum);

            for (int i = 0; i < pixelsRed.length; i++) {
                float[] rgb = Util.hslToRgb(pixelsHue[i], pixelsSat[i], pixelsLum[i], 0f);
                pixelsRed[i] = rgb[0];
                pixelsGreen[i] = rgb[1];
                pixelsBlue[i] = rgb[2];
            }

            ipRed.setPixels(1, fpRed);
            ipGreen.setPixels(2, fpGreen);
            ipBlue.setPixels(3, fpBlue);
        }

        // image.updateAndDraw();
    }

    /*
     * Unsharp mask algorithm that prevents edge clipping effect.
     * @param radius
     * @param amount
     * @param clippingStrength the factor of the clipping suppression, if set to 0 it means no clipping supression is being applied.
     * @param clippingRange represents the histogram % value from where the clipping has to be suppressed by holding off the amount.
     * @param fp
     */
    private void doUnsharpMask(double radius, float amount, float clippingStrength, float clippingRange,
            FloatProcessor fp) {
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
    private void doUnsharpMaskAdaptive(double radius, float amount, float clippingStrength, float clippingRange, FloatProcessor fpInitial,
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

    // public void applyIndividualLuminanceMode(ImagePlus image, final
    // LSWSharpenParameters parameters) {
    // if (!parameters.isIncludeRed() && !parameters.isIncludeGreen() &&
    // !parameters.isIncludeBlue()) {
    // log.error("Cannot have red, green and blue excluded");
    // return;
    // }
    // UnsharpMaskParameters unsharpMaskParametersRed =
    // parameters.getUnsharpMaskParametersRed();
    // UnsharpMaskParameters unsharpMaskParametersGreen =
    // parameters.getUnsharpMaskParametersGreen();
    // UnsharpMaskParameters unsharpMaskParametersBlue =
    // parameters.getUnsharpMaskParametersBlue();
    // boolean saturationApplied = false;
    // ImageStack stack = image.getStack();
    // ImageProcessor ipRed = stack.getProcessor(1);
    // ImageProcessor ipGreen = stack.getProcessor(2);
    // ImageProcessor ipBlue = stack.getProcessor(3);
    //
    // for (int it = 0; it < unsharpMaskParameters.getIterations(); it++) {
    //
    // FloatProcessor fpRed = ipRed.toFloat(1, null);
    // FloatProcessor fpGreen = ipGreen.toFloat(2, null);
    // FloatProcessor fpBlue = ipBlue.toFloat(3, null);
    // fpRed.snapshot();
    // fpGreen.snapshot();
    // fpBlue.snapshot();
    // float[] pixelsRed = (float[]) fpRed.getPixels();
    // float[] pixelsGreen = (float[]) fpGreen.getPixels();
    // float[] pixelsBlue = (float[]) fpBlue.getPixels();
    //
    // float[] pixelsHue = new float[pixelsRed.length];
    // float[] pixelsSat = new float[pixelsRed.length];
    // float[] pixelsLum = new float[pixelsRed.length];
    // for (int i = 0; i < pixelsRed.length; i++) {
    // float[] hsl = rgbToHsl(pixelsRed[i], pixelsGreen[i], pixelsBlue[i],
    // parameters.isIncludeRed(), parameters.isIncludeGreen(),
    // parameters.isIncludeBlue());
    // pixelsHue[i] = hsl[0];
    // pixelsSat[i] = hsl[1] * (saturationApplied ? 1f :
    // parameters.getSaturation());
    // pixelsLum[i] = hsl[2];
    // }
    // saturationApplied = true;
    // FloatProcessor fpLum = new FloatProcessor(image.getWidth(),
    // image.getHeight(), pixelsLum);
    // fpLum.snapshot();
    // doUnsharpMask(unsharpMaskParameters.getRadius(),
    // unsharpMaskParameters.getAmount(), fpLum);
    //
    // for (int i = 0; i < pixelsRed.length; i++) {
    // float[] rgb = hslToRgb(pixelsHue[i], pixelsSat[i], pixelsLum[i], 0f);
    // pixelsRed[i] = rgb[0];
    // pixelsGreen[i] = rgb[1];
    // pixelsBlue[i] = rgb[2];
    // }
    //
    // ipRed.setPixels(1, fpRed);
    // ipGreen.setPixels(2, fpGreen);
    // ipBlue.setPixels(3, fpBlue);
    // image.updateAndDraw();
    // }
    // }

}
