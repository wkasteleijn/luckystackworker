package nl.wilcokas.luckystackworker.filter;

import java.awt.Rectangle;

import ij.plugin.filter.GaussianBlur;
import ij.process.FloatProcessor;

public class LSWUnsharpMask {
    public void sharpenFloat(FloatProcessor fp, double sigma, float weight) {
        float[] snapshotPixels = ((float[]) fp.getPixels()).clone();
        GaussianBlur gb = new GaussianBlur();
        gb.blurGaussian(fp, sigma, sigma, 0.01);
        if (Thread.currentThread().isInterrupted())
            return;
        float[] pixels = (float[]) fp.getPixels();
        int width = fp.getWidth();
        Rectangle roi = fp.getRoi();
        for (int y = roi.y; y < roi.y + roi.height; y++)
            for (int x = roi.x, p = width * y + x; x < roi.x + roi.width; x++, p++)
                pixels[p] = (snapshotPixels[p] - weight * pixels[p]) / (1f - weight);
    }
}
