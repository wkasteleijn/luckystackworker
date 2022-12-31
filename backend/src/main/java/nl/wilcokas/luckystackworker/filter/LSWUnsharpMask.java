package nl.wilcokas.luckystackworker.filter;

import java.awt.Rectangle;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.GaussianBlur;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LSWUnsharpMask {
    public void apply(ImagePlus image, double radius, float amount, int iterations) {
        for (int i = 0; i < iterations; i++) {
            ImageStack stack = image.getStack();
            for (int slice = 1; slice <= stack.getSize(); slice++) {
                ImageProcessor ip = stack.getProcessor(slice);
                FloatProcessor fp = ip.toFloat(slice, null);
                fp.snapshot();
                doUnsharpMask(radius, amount, fp);
                ip.setPixels(slice, fp);
            }
            image.updateAndDraw();
        }
    }

    private void doUnsharpMask(double radius, float amount, FloatProcessor fp) {
        GaussianBlur gb = new GaussianBlur();
        gb.blurGaussian(fp, radius, radius, 0.01);
        float[] pixels = (float[]) fp.getPixels();
        float[] snapshotPixels = (float[]) fp.getSnapshotPixels();
        int width = fp.getWidth();
        Rectangle roi = fp.getRoi();
        for (int y = roi.y; y < roi.y + roi.height; y++) {
            for (int x = roi.x, p = width * y + x; x < roi.x + roi.width; x++, p++) {
                pixels[p] = (snapshotPixels[p] - amount * pixels[p]) / (1f - amount);
            }
        }
    }

    public void applyLuminance(ImagePlus image, double radius, float amount, int iterations, float saturation, boolean includeRed,
            boolean includeGreen, boolean includeBlue) {
        if (!includeRed && !includeGreen && !includeBlue) {
            log.error("Cannot have red, green and blue excluded");
            return;
        }
        boolean saturationApplied = false;
        for (int it = 0; it < iterations; it++) {
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
                float[] hsl = rgbToHsl(pixelsRed[i], pixelsGreen[i], pixelsBlue[i], includeRed, includeGreen, includeBlue);
                pixelsHue[i] = hsl[0];
                pixelsSat[i] = hsl[1] * (saturationApplied ? 1f : saturation);
                pixelsLum[i] = hsl[2];
            }
            saturationApplied = true;
            FloatProcessor fpLum = new FloatProcessor(image.getWidth(), image.getHeight(), pixelsLum);
            fpLum.snapshot();
            doUnsharpMask(radius, amount, fpLum);

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
