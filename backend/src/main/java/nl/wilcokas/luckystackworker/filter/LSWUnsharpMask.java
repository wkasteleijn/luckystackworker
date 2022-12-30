package nl.wilcokas.luckystackworker.filter;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.GaussianBlur;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

public class LSWUnsharpMask {
    public void apply(ImagePlus image, double sigma, float weight) {
        ImageStack stack = image.getStack();
        for (int slice = 1; slice <= stack.getSize(); slice++) {
            ImageProcessor ip = stack.getProcessor(slice);
            ip.setPixels(stack.getPixels(slice));
            ip.setSliceNumber(slice);
            ip.setSnapshotPixels(null);
            FloatProcessor fp = null;
            fp = ip.toFloat(slice, fp);
            fp.snapshot();
            GaussianBlur gb = new GaussianBlur();
            gb.blurGaussian(fp, sigma, sigma, 0.01);
            float[] pixels = (float[]) fp.getPixels();
            float[] snapshotPixels = (float[]) fp.getSnapshotPixels();
            int width = fp.getWidth();
            Rectangle roi = fp.getRoi();
            for (int y = roi.y; y < roi.y + roi.height; y++) {
                for (int x = roi.x, p = width * y + x; x < roi.x + roi.width; x++, p++) {
                    pixels[p] = (snapshotPixels[p] - weight * pixels[p]) / (1f - weight);
                }
            }
            ip.setPixels(slice, fp);
        }
        image.updateAndDraw();
    }

    public void applyLuminance(ImagePlus image, double sigma, float weight) {
        ImageStack stack = image.getStack();
        List<FloatProcessor> floatProcessors = new ArrayList<>();
        for (int slice = 1; slice <= stack.getSize(); slice++) {
            ImageProcessor ip = stack.getProcessor(slice);
            ip.setPixels(stack.getPixels(slice));
            ip.setSliceNumber(slice);
            ip.setSnapshotPixels(null);
            FloatProcessor fp = null;
            fp = ip.toFloat(slice, fp);
            fp.snapshot();
            GaussianBlur gb = new GaussianBlur();
            gb.blurGaussian(fp, sigma, sigma, 0.01);
            float[] pixels = (float[]) fp.getPixels();
            float[] snapshotPixels = (float[]) fp.getSnapshotPixels();
            int width = fp.getWidth();
            Rectangle roi = fp.getRoi();
            for (int y = roi.y; y < roi.y + roi.height; y++) {
                for (int x = roi.x, p = width * y + x; x < roi.x + roi.width; x++, p++) {
                    pixels[p] = (snapshotPixels[p] - weight * pixels[p]) / (1f - weight);
                }
            }
            floatProcessors.add(fp);
        }
        float[] luminancePixels = new float[image.getWidth() * image.getHeight()];
        for (int slice = 1; slice <= stack.getSize(); slice++) {
            FloatProcessor fp = floatProcessors.get(slice);
            float[] pixels = (float[]) fp.getPixels();
            for (int i = 0; i < pixels.length; i++) {
                luminancePixels[i] += pixels[i];
            }
        }
        for (int i = 0; i < luminancePixels.length; i++) {
            luminancePixels[i] += luminancePixels[i] / 3;
        }
        for (int slice = 1; slice <= stack.getSize(); slice++) {
            ImageProcessor ip = stack.getProcessor(slice);
            // ip.setPixels(slice, fp);
        }

        image.updateAndDraw();
    }

}
