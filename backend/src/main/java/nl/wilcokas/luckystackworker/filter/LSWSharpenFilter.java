package nl.wilcokas.luckystackworker.filter;

import java.awt.Rectangle;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.GaussianBlur;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.filter.settings.LSWSharpenParameters;
import nl.wilcokas.luckystackworker.filter.settings.UnsharpMaskParameters;

@Slf4j
public class LSWSharpenFilter {

    private static final float FLOAT_MAX_SATURATED_VALUE = 65535f;

    public void applyRGBMode(ImagePlus image, final UnsharpMaskParameters unsharpMaskParameters) {
        for (int i = 0; i < unsharpMaskParameters.getIterations(); i++) {
            ImageStack stack = image.getStack();
            for (int slice = 1; slice <= stack.getSize(); slice++) {
                ImageProcessor ip = stack.getProcessor(slice);
                FloatProcessor fp = ip.toFloat(slice, null);
                fp.snapshot();
                doUnsharpMask(unsharpMaskParameters.getRadius(), unsharpMaskParameters.getAmount(), unsharpMaskParameters.getClippingStrength(),
                        unsharpMaskParameters.getClippingRange(), fp);
                ip.setPixels(slice, fp);
            }
            image.updateAndDraw();
        }
    }

    public void applyLuminanceMode(ImagePlus image, final LSWSharpenParameters parameters) {
        if (!parameters.isIncludeRed() && !parameters.isIncludeGreen() && !parameters.isIncludeBlue()) {
            log.error("Cannot have red, green and blue excluded");
            return;
        }
        UnsharpMaskParameters unsharpMaskParameters = parameters.getUnsharpMaskParameters();
        boolean saturationApplied = false;
        for (int it = 0; it < unsharpMaskParameters.getIterations(); it++) {
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
                float[] hsl = rgbToHsl(pixelsRed[i], pixelsGreen[i], pixelsBlue[i], parameters.isIncludeRed(), parameters.isIncludeGreen(),
                        parameters.isIncludeBlue());
                pixelsHue[i] = hsl[0];
                pixelsSat[i] = hsl[1] * (saturationApplied ? 1f : parameters.getSaturation());
                pixelsLum[i] = hsl[2];
            }
            saturationApplied = true;
            FloatProcessor fpLum = new FloatProcessor(image.getWidth(), image.getHeight(), pixelsLum);
            fpLum.snapshot();
            doUnsharpMask(unsharpMaskParameters.getRadius(), unsharpMaskParameters.getAmount(), unsharpMaskParameters.getClippingStrength(),
                    unsharpMaskParameters.getClippingRange(), fpLum);

            for (int i = 0; i < pixelsRed.length; i++) {
                float[] rgb = hslToRgb(pixelsHue[i], pixelsSat[i], pixelsLum[i], 0f);
                pixelsRed[i] = rgb[0];
                pixelsGreen[i] = rgb[1];
                pixelsBlue[i] = rgb[2];
            }

            ipRed.setPixels(1, fpRed);
            ipGreen.setPixels(2, fpGreen);
            ipBlue.setPixels(3, fpBlue);
            image.updateAndDraw();
        }
    }

    //    public void applyIndividualLuminanceMode(ImagePlus image, final LSWSharpenParameters parameters) {
    //        if (!parameters.isIncludeRed() && !parameters.isIncludeGreen() && !parameters.isIncludeBlue()) {
    //            log.error("Cannot have red, green and blue excluded");
    //            return;
    //        }
    //        UnsharpMaskParameters unsharpMaskParametersRed = parameters.getUnsharpMaskParametersRed();
    //        UnsharpMaskParameters unsharpMaskParametersGreen = parameters.getUnsharpMaskParametersGreen();
    //        UnsharpMaskParameters unsharpMaskParametersBlue = parameters.getUnsharpMaskParametersBlue();
    //        boolean saturationApplied = false;
    //        ImageStack stack = image.getStack();
    //        ImageProcessor ipRed = stack.getProcessor(1);
    //        ImageProcessor ipGreen = stack.getProcessor(2);
    //        ImageProcessor ipBlue = stack.getProcessor(3);
    //
    //        for (int it = 0; it < unsharpMaskParameters.getIterations(); it++) {
    //
    //            FloatProcessor fpRed = ipRed.toFloat(1, null);
    //            FloatProcessor fpGreen = ipGreen.toFloat(2, null);
    //            FloatProcessor fpBlue = ipBlue.toFloat(3, null);
    //            fpRed.snapshot();
    //            fpGreen.snapshot();
    //            fpBlue.snapshot();
    //            float[] pixelsRed = (float[]) fpRed.getPixels();
    //            float[] pixelsGreen = (float[]) fpGreen.getPixels();
    //            float[] pixelsBlue = (float[]) fpBlue.getPixels();
    //
    //            float[] pixelsHue = new float[pixelsRed.length];
    //            float[] pixelsSat = new float[pixelsRed.length];
    //            float[] pixelsLum = new float[pixelsRed.length];
    //            for (int i = 0; i < pixelsRed.length; i++) {
    //                float[] hsl = rgbToHsl(pixelsRed[i], pixelsGreen[i], pixelsBlue[i], parameters.isIncludeRed(), parameters.isIncludeGreen(),
    //                        parameters.isIncludeBlue());
    //                pixelsHue[i] = hsl[0];
    //                pixelsSat[i] = hsl[1] * (saturationApplied ? 1f : parameters.getSaturation());
    //                pixelsLum[i] = hsl[2];
    //            }
    //            saturationApplied = true;
    //            FloatProcessor fpLum = new FloatProcessor(image.getWidth(), image.getHeight(), pixelsLum);
    //            fpLum.snapshot();
    //            doUnsharpMask(unsharpMaskParameters.getRadius(), unsharpMaskParameters.getAmount(), fpLum);
    //
    //            for (int i = 0; i < pixelsRed.length; i++) {
    //                float[] rgb = hslToRgb(pixelsHue[i], pixelsSat[i], pixelsLum[i], 0f);
    //                pixelsRed[i] = rgb[0];
    //                pixelsGreen[i] = rgb[1];
    //                pixelsBlue[i] = rgb[2];
    //            }
    //
    //            ipRed.setPixels(1, fpRed);
    //            ipGreen.setPixels(2, fpGreen);
    //            ipBlue.setPixels(3, fpBlue);
    //            image.updateAndDraw();
    //        }
    //    }

    /*
     * Unsharp mask algorithm that prevents edge clipping effect.
     * @param radius
     * @param amount
     * @param clippingStrength the factor of the clipping suppression
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
                float pixelValue = getUnsharpMaskValue(snapshotPixels[p], pixels[p], amount);
                int cutoffIndex = Math.round(((pixelValue < 0f ? 0f : pixelValue) / FLOAT_MAX_SATURATED_VALUE) * 100f);
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

    private float[] rgbToHsl(float red, float green, float blue, boolean includeRed, boolean includeGreen, boolean includeBlue) {
        float max = Math.max(Math.max(red, green), blue);
        float min = Math.min(Math.min(red, green), blue);
        float c = max - min;

        float hue_ = 0.f;
        if (c == 0) {
            hue_ = 0;
        } else if (max == red) {
            hue_ = (green - blue) / c;
            if (hue_ < 0)
                hue_ += 6.f;
        } else if (max == green) {
            hue_ = (blue - red) / c + 2.f;
        } else if (max == blue) {
            hue_ = (red - green) / c + 4.f;
        }
        float hue = 60.f * hue_;

        float luminanceDivisor = (includeRed ? 1 : 0) + (includeGreen ? 1 : 0) + (includeBlue ? 1 : 0);
        float luminance = ((includeRed ? red : 0) + (includeGreen ? green : 0) + (includeBlue ? blue : 0)) / luminanceDivisor;

        float saturation;
        if (c == 0) {
            saturation = 0.f;
        } else {
            saturation = c / (1 - Math.abs(2.f * luminance - 1.f));
        }

        float[] hsl = new float[3];
        hsl[0] = hue;
        hsl[1] = saturation;
        hsl[2] = luminance;
        return hsl;
    }

    private float[] hslToRgb(float hue, float saturation, float luminance, float hueCorrectionFactor) {
        float c = (1 - Math.abs(2.f * luminance - 1.f)) * saturation;
        float hue_ = hue / 60.f;
        float h_mod2 = hue_;
        if (h_mod2 >= 4.f)
            h_mod2 -= 4.f;
        else if (h_mod2 >= 2.f)
            h_mod2 -= 2.f;

        float x = c * (1 - Math.abs(h_mod2 - 1));
        float r_, g_, b_;
        if (hue_ < 1) {
            r_ = c;
            g_ = x;
            b_ = 0;
        } else if (hue_ < 2) {
            r_ = x;
            g_ = c;
            b_ = 0;
        } else if (hue_ < 3) {
            r_ = 0;
            g_ = c;
            b_ = x;
        } else if (hue_ < 4) {
            r_ = 0;
            g_ = x;
            b_ = c;
        } else if (hue_ < 5) {
            r_ = x;
            g_ = 0;
            b_ = c;
        } else {
            r_ = c;
            g_ = 0;
            b_ = x;
        }

        float m = luminance - (0.5f * c);
        float red = ((r_ + m) + 0.5f) * (1f + hueCorrectionFactor);
        float green = ((g_ + m) + 0.5f) * (1f - hueCorrectionFactor);
        float blue = ((b_ + m) + 0.5f) * (1f + hueCorrectionFactor);
        return new float[] { red, green, blue };
    }

}
