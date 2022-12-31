package nl.wilcokas.luckystackworker.filter;

import java.awt.Rectangle;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.GaussianBlur;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

public class LSWUnsharpMask {
    public void apply(ImagePlus image, double sigma, float weight, int iterations) {
        ImageStack stack = image.getStack();
        for (int slice = 1; slice <= stack.getSize(); slice++) {
            ImageProcessor ip = stack.getProcessor(slice);
            ip.setPixels(stack.getPixels(slice));
            ip.setSliceNumber(slice);
            ip.setSnapshotPixels(null);
            FloatProcessor fp = ip.toFloat(slice, null);
            fp.snapshot();
            GaussianBlur gb = new GaussianBlur();
            gb.blurGaussian(fp, sigma, sigma, 0.01);
            float[] pixels = (float[]) fp.getPixels();
            float[] snapshotPixels = (float[]) fp.getSnapshotPixels();
            int width = fp.getWidth();
            Rectangle roi = fp.getRoi();
            for (int i = 0; i < iterations; i++) {
                for (int y = roi.y; y < roi.y + roi.height; y++) {
                    for (int x = roi.x, p = width * y + x; x < roi.x + roi.width; x++, p++) {
                        pixels[p] = (snapshotPixels[p] - weight * pixels[p]) / (1f - weight);
                    }
                }
            }
            ip.setPixels(slice, fp);
        }
        image.updateAndDraw();
    }

    public void applyLuminance(ImagePlus image, double sigma, float weight, int iterations, float saturation) {

        // TODO: convert RGB to HSL (zie
        // https://biginteger.blogspot.com/2012/01/convert-rgb-to-hsl-and-vice-versa-in.html)
        // Zie functies onder
        // Neem L voor verscherpen en S om kleur aan te passen, super simpel.
        ImageStack stack = image.getStack();
        ImageProcessor ipRed = stack.getProcessor(1);
        ImageProcessor ipGreen = stack.getProcessor(2);
        ImageProcessor ipBlue = stack.getProcessor(3);
        ipRed.setSnapshotPixels(null);
        ipGreen.setSnapshotPixels(null);
        ipBlue.setSnapshotPixels(null);

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
            float[] hsl = rgbToHsl(pixelsRed[i], pixelsGreen[i], pixelsBlue[i]);
            pixelsHue[i] = hsl[0];
            pixelsSat[i] = hsl[1] * saturation;
            pixelsLum[i] = hsl[2];
        }
        FloatProcessor fpLum = new FloatProcessor(image.getWidth(), image.getHeight(), pixelsLum);
        fpLum.snapshot();
        GaussianBlur gb = new GaussianBlur();
        gb.blurGaussian(fpLum, sigma, sigma, 0.01);
        float[] snapshotPixels = (float[]) fpLum.getSnapshotPixels();
        int width = fpLum.getWidth();
        Rectangle roi = fpLum.getRoi();
        for (int i = 0; i < iterations; i++) {
            for (int y = roi.y; y < roi.y + roi.height; y++) {
                for (int x = roi.x, p = width * y + x; x < roi.x + roi.width; x++, p++) {
                    pixelsLum[p] = (snapshotPixels[p] - weight * pixelsLum[p]) / (1f - weight);
                }
            }
        }

        for (int i = 0; i < pixelsRed.length; i++) {
            float[] rgb = hslToRgb(pixelsHue[i], pixelsSat[i], pixelsLum[i], 0.05f);
            pixelsRed[i] = rgb[0];
            pixelsGreen[i] = rgb[1];
            pixelsBlue[i] = rgb[2];
        }

        ipRed.setPixels(1, fpRed);
        ipGreen.setPixels(2, fpGreen);
        ipBlue.setPixels(3, fpBlue);
        image.updateAndDraw();


    }

    public static float[] rgbToHsl(float r, float g, float b) {
        float max = Math.max(Math.max(r, g), b);
        float min = Math.min(Math.min(r, g), b);
        float c = max - min;

        float h_ = 0.f;
        if (c == 0) {
            h_ = 0;
        } else if (max == r) {
            h_ = (g - b) / c;
            if (h_ < 0)
                h_ += 6.f;
        } else if (max == g) {
            h_ = (b - r) / c + 2.f;
        } else if (max == b) {
            h_ = (r - g) / c + 4.f;
        }
        float h = 60.f * h_;

        float l = (r + g + b) / 3;

        float s;
        if (c == 0) {
            s = 0.f;
        } else {
            s = c / (1 - Math.abs(2.f * l - 1.f));
        }

        float[] hsl = new float[3];
        hsl[0] = h;
        hsl[1] = s;
        hsl[2] = l;
        return hsl;
    }

    public static float[] hslToRgb(float h, float s, float l, float hueCorrectionFactor) {
        float c = (1 - Math.abs(2.f * l - 1.f)) * s;
        float h_ = h / 60.f;
        float h_mod2 = h_;
        if (h_mod2 >= 4.f)
            h_mod2 -= 4.f;
        else if (h_mod2 >= 2.f)
            h_mod2 -= 2.f;

        float x = c * (1 - Math.abs(h_mod2 - 1));
        float r_, g_, b_;
        if (h_ < 1) {
            r_ = c;
            g_ = x;
            b_ = 0;
        } else if (h_ < 2) {
            r_ = x;
            g_ = c;
            b_ = 0;
        } else if (h_ < 3) {
            r_ = 0;
            g_ = c;
            b_ = x;
        } else if (h_ < 4) {
            r_ = 0;
            g_ = x;
            b_ = c;
        } else if (h_ < 5) {
            r_ = x;
            g_ = 0;
            b_ = c;
        } else {
            r_ = c;
            g_ = 0;
            b_ = x;
        }

        float m = l - (0.5f * c);
        float r = ((r_ + m) + 0.5f) * (1f + hueCorrectionFactor);
        float g = ((g_ + m) + 0.5f) * (1f - hueCorrectionFactor);
        float b = ((b_ + m) + 0.5f) * (1f + hueCorrectionFactor);
        return new float[] { r, g, b };
    }

}
