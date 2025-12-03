package nl.wilcokas.luckystackworker.filter.sigma;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import java.awt.Rectangle;
import java.math.BigDecimal;
import java.math.RoundingMode;

import nl.wilcokas.luckystackworker.model.Profile;
import org.springframework.stereotype.Component;

/**
 * This plugin-Filter provides a selective mean (averaging) filter. In contrast to the standard mean
 * filter, it preserves edges better and is less sensitive to outliers. Based on Lee's sigma filter
 * algorithm and a plugin by Tony Collins. J.S. Lee, Digital image noise smoothing and the sigma
 * filter, in: Computer Vision, Graphics and Image Processing, vol. 24, 255-269 (1983). The "Outlier
 * Aware" option is a modification of Lee's algorithm introduced by Tony Collins.
 *
 * <p>The filter smoothens an image by taking an average over the neighboring pixels, but only
 * includes those pixels that have a value not deviating from the current pixel by more than a given
 * range. The range is defined by the standard deviation of the pixel values within the neighborhood
 * ("Use pixels within ... sigmas"). If the number of pixels in this range is too low (less than
 * "Minimum pixel fraction"), averaging over all neighboring pixels is performed. With the "Outlier
 * Aware" option, averaging over all neighboring pixels excludes the center pixel. Thus, outliers
 * having a value very different from the surrounding are not included in the average, i.e.,
 * completely eliminated.
 *
 * <p>For preserving the edges, values of "Use pixels within" between 1 and 2 sigmas are
 * recommended. With high values, the filter will behave more like a traditional averaging filter,
 * i.e. smoothen the edges. Typical values of the minimum pixel fraction are around 0.2, with higher
 * values resulting in more noise supression, but smoother edges.
 *
 * <p>If preserving the edges is not desired, "Use pixels within" 2-3 sigmas and a minimum pixel
 * fraction around 0.8-0.9, together with the "Outlier Aware" option will smoothen the image,
 * similar to a traditional filter, but without being influenced by outliers strongly deviating from
 * the surrounding pixels (hot pixels, dead pixels etc.).
 *
 * <p>
 *
 * <p>Code by Michael Schmid, 2007-10-25 Adapted by Wilco Kasteleijn, 2023-02-17
 *
 * <p>Note: this class is based on the original filter from Michael Schmid, but rewritten as an LSW
 * filter.
 */
@Component
public class SigmaFilterPlus {
    // smoothing
    protected int kRadius; // kernel radius. Size is (2*kRadius+1)^2
    protected int kNPoints; // number of points in the kernel
    protected int[] lineRadius; // the length of each kernel line is 2*lineRadius+1

    public void applyDenoise1(ImagePlus image, Profile profile) {
        double sigma = 2D;
        ImageStack stack = image.getStack();
        // red
        ImageProcessor ip = stack.getProcessor(1);
        BigDecimal factor = profile.getDenoise1Amount().compareTo(new BigDecimal("100")) > 0
                ? new BigDecimal(100)
                : profile.getDenoise1Amount();
        BigDecimal minimum = factor.divide(new BigDecimal(100), 2, RoundingMode.HALF_EVEN);
        applySigmaToLayer(
                sigma,
                minimum.doubleValue(),
                profile.getDenoise1Radius().doubleValue(),
                profile.getDenoise1Iterations() == 0 ? 1 : profile.getDenoise1Iterations(),
                ip,
                1);
        // green
        ip = stack.getProcessor(2);
        factor = profile.getDenoise1AmountGreen().compareTo(new BigDecimal("100")) > 0
                ? new BigDecimal(100)
                : profile.getDenoise1AmountGreen();
        minimum = factor.divide(new BigDecimal(100), 2, RoundingMode.HALF_EVEN);
        applySigmaToLayer(
                sigma,
                minimum.doubleValue(),
                profile.getDenoise1RadiusGreen().doubleValue(),
                profile.getDenoise1IterationsGreen() == 0 ? 1 : profile.getDenoise1IterationsGreen(),
                ip,
                2);
        // blue
        ip = stack.getProcessor(3);
        factor = profile.getDenoise1AmountBlue().compareTo(new BigDecimal("100")) > 0
                ? new BigDecimal(100)
                : profile.getDenoise1AmountBlue();
        minimum = factor.divide(new BigDecimal(100), 2, RoundingMode.HALF_EVEN);
        applySigmaToLayer(
                sigma,
                minimum.doubleValue(),
                profile.getDenoise1RadiusBlue().doubleValue(),
                profile.getDenoise1IterationsBlue() == 0 ? 1 : profile.getDenoise1IterationsBlue(),
                ip,
                3);
    }

    public void applyDenoise2(ImagePlus image, Profile profile) {
        double sigma = 5D;
        double minimum = 1D;
        ImageStack stack = image.getStack();
        // red
        ImageProcessor ip = stack.getProcessor(1);
        applySigmaToLayer(
                sigma,
                minimum,
                profile.getDenoise2Radius().doubleValue(),
                profile.getDenoise2Iterations() == 0 ? 1 : profile.getDenoise2Iterations(),
                ip,
                1);
        // green
        ip = stack.getProcessor(2);
        applySigmaToLayer(
                sigma,
                minimum,
                profile.getDenoise2RadiusGreen().doubleValue(),
                profile.getDenoise2IterationsGreen() == 0 ? 1 : profile.getDenoise2IterationsGreen(),
                ip,
                2);
        // blue
        ip = stack.getProcessor(3);
        applySigmaToLayer(
                sigma,
                minimum,
                profile.getDenoise2RadiusBlue().doubleValue(),
                profile.getDenoise2IterationsBlue() == 0 ? 1 : profile.getDenoise2IterationsBlue(),
                ip,
                3);
    }

    private void applySigmaToLayer(
            double sigma, double minimum, double radius, int iterations, ImageProcessor ip, int layer) {
        makeKernel(radius);
        for (int i = 0; i < iterations; i++) {
            // copy class variables to local ones - this is necessary for preview
            int[] lineRadius;
            int kRadius, kNPoints, minPixNumber;
            synchronized (this) { // the two following items must be consistent
                lineRadius = (this.lineRadius.clone()); // cloning also required by doFiltering method
                kRadius = this.kRadius; // kernel radius
                kNPoints = this.kNPoints; // number of pixels in the kernel
                minPixNumber = (int) (kNPoints * minimum + 0.999999); // min pixels in sigma range
            }
            if (Thread.currentThread().isInterrupted()) return;

            FloatProcessor fp = ip.toFloat(layer, null);
            doFiltering(fp, kRadius, lineRadius, sigma, minPixNumber, true);
            ip.setPixels(layer, fp);
        }
    }

    /**
     * Filter a FloatProcessor according to filterType
     *
     * @param ip The image subject to filtering
     * @param kRadius The kernel radius. The kernel has a side length of 2*kRadius+1
     * @param lineRadius The radius of the lines in the kernel. Line length of line i is
     *     2*lineRadius[i]+1. Note that the array <code>lineRadius</code> will be modified, thus call
     *     this method with a clone of the original lineRadius array if the array should be used
     *     again.
     */
    //
    // Data handling: The area needed for processing a line, i.e. a stripe of width
    // (2*kRadius+1)
    // is written into the array 'cache'. This array is padded at the edges of the
    // image so that
    // a surrounding with radius kRadius for each pixel processed is within 'cache'.
    // Out-of-image
    // pixels are set to the value of the neares edge pixel. When adding a new line,
    // the lines in
    // 'cache' are not shifted but rather the smaller array with the line lengths of
    // the kernel is
    // shifted.
    //
    private void doFiltering(
            FloatProcessor ip,
            int kRadius,
            int[] lineRadius,
            double sigmaWidth,
            int minPixNumber,
            boolean outlierAware) {
        float[] pixels = (float[]) ip.getPixels(); // array of the pixel values of the input image
        int width = ip.getWidth();
        int height = ip.getHeight();
        Rectangle roi = ip.getRoi();
        int xmin = roi.x - kRadius;
        int xEnd = roi.x + roi.width;
        int xmax = xEnd + kRadius;
        int kSize = 2 * kRadius + 1;
        int cacheWidth = xmax - xmin;
        int xminInside = xmin > 0 ? xmin : 0;
        int xmaxInside = xmax < width ? xmax : width;
        int widthInside = xmaxInside - xminInside;
        boolean smallKernel = kRadius < 2;
        float[] cache = new float[cacheWidth * kSize]; // a stripe of the image with height=2*kRadius+1
        for (int y = roi.y - kRadius, iCache = 0; y < roi.y + kRadius; y++)
            for (int x = xmin; x < xmax; x++, iCache++) // fill the cache for filtering the first line
            cache[iCache] = pixels[
                    (x < 0 ? 0 : x >= width ? width - 1 : x) + width * (y < 0 ? 0 : y >= height ? height - 1 : y)];
        int nextLineInCache = 2 * kRadius; // where the next line should be written to
        double[] sums = new double[2];
        Thread thread = Thread.currentThread(); // needed to check for interrupted state
        long lastTime = System.currentTimeMillis();
        for (int y = roi.y; y < roi.y + roi.height; y++) {
            long time = System.currentTimeMillis();
            if (time - lastTime > 100) {
                lastTime = time;
                if (thread.isInterrupted()) return;
            }
            int ynext = y + kRadius; // C O P Y N E W L I N E into cache
            if (ynext >= height) ynext = height - 1;
            float leftpxl = pixels[width * ynext]; // edge pixels of the line replace out-of-image pixels
            float rightpxl = pixels[width - 1 + width * ynext];
            int iCache = cacheWidth * nextLineInCache; // where in the cache we have to copy to
            for (int x = xmin; x < 0; x++, iCache++) cache[iCache] = leftpxl;
            System.arraycopy(pixels, xminInside + width * ynext, cache, iCache, widthInside);
            iCache += widthInside;
            for (int x = width; x < xmax; x++, iCache++) cache[iCache] = rightpxl;
            nextLineInCache = (nextLineInCache + 1) % kSize;
            boolean fullCalculation = true; // F I L T E R the line
            for (int x = roi.x, p = x + y * width, xCache0 = kRadius; x < xEnd; x++, p++, xCache0++) {
                double value = pixels[p]; // the current pixel
                if (fullCalculation) {
                    fullCalculation = smallKernel; // for small kernel, always use the full area, not incremental
                    // algorithm
                    getAreaSums(cache, cacheWidth, xCache0, lineRadius, kSize, sums);
                } else addSideSums(cache, cacheWidth, xCache0, lineRadius, kSize, sums);
                double mean = sums[0] / kNPoints; // sum[0] is the sum over the pixels, sum[1] the sum over the squares
                double variance = sums[1] / kNPoints - mean * mean;

                double sigmaRange = sigmaWidth * Math.sqrt(variance);
                double sigmaBottom = value - sigmaRange;
                double sigmaTop = value + sigmaRange;
                double sum = 0;
                int count = 0;
                for (int y1 = 0; y1 < kSize; y1++) { // for y1 within the cache stripe
                    for (int x1 = xCache0 - lineRadius[y1], iCache1 = y1 * cacheWidth + x1;
                            x1 <= xCache0 + lineRadius[y1];
                            x1++, iCache1++) {
                        float v = cache[iCache1]; // a point within the kernel
                        if ((v >= sigmaBottom) && (v <= sigmaTop)) {
                            sum += v;
                            count++;
                        }
                    }
                }
                // if there are too few pixels in the kernel that are within sigma range, the
                // mean of the entire kernel is taken.
                if (count >= minPixNumber) pixels[p] = (float) (sum / count);
                else {
                    if (outlierAware)
                        pixels[p] = (float) ((sums[0] - value) / (kNPoints - 1)); // assumes that the current pixel is
                    // an outlier
                    else pixels[p] = (float) mean;
                }
            } // for x
            int newLineRadius0 = lineRadius[kSize - 1]; // shift kernel lineRadii one line
            System.arraycopy(lineRadius, 0, lineRadius, 1, kSize - 1);
            lineRadius[0] = newLineRadius0;
        } // for y
    }

    /**
     * Get sum of values and values squared within the kernel area. xCache0 points to cache element
     * equivalent to current x coordinate. Output is written to array sums[0] = sum; sums[1] = sum of
     * squares
     */
    private void getAreaSums(float[] cache, int cacheWidth, int xCache0, int[] lineRadius, int kSize, double[] sums) {
        double sum = 0, sum2 = 0;
        for (int y = 0; y < kSize; y++) { // y within the cache stripe
            for (int x = xCache0 - lineRadius[y], iCache = y * cacheWidth + x;
                    x <= xCache0 + lineRadius[y];
                    x++, iCache++) {
                float v = cache[iCache];
                sum += v;
                sum2 += v * v;
            }
        }
        sums[0] = sum;
        sums[1] = sum2;
        return;
    }

    /**
     * Add all values and values squared at the right border inside minus at the left border outside
     * the kernal area. Output is added or subtracted to/from array sums[0] += sum; sums[1] += sum of
     * squares when at the right border, minus when at the left border
     */
    private void addSideSums(float[] cache, int cacheWidth, int xCache0, int[] lineRadius, int kSize, double[] sums) {
        double sum = 0, sum2 = 0;
        for (int y = 0; y < kSize; y++) { // y within the cache stripe
            int iCache0 = y * cacheWidth + xCache0;
            float v = cache[iCache0 + lineRadius[y]];
            sum += v;
            sum2 += v * v;
            v = cache[iCache0 - lineRadius[y] - 1];
            sum -= v;
            sum2 -= v * v;
        }
        sums[0] += sum;
        sums[1] += sum2;
        return;
    }

    /**
     * Create a circular kernel of a given radius. Radius = 0.5 includes the 4 neighbors of the pixel
     * in the center, radius = 1 corresponds to a 3x3 kernel size. The output is written to class
     * variables kNPoints (number of points inside the kernel) and lineRadius, which is an array
     * giving the radius of each line. Line length is 2*lineRadius+1.
     */
    private synchronized void makeKernel(double radius) {
        if (radius >= 1.5 && radius < 1.75) // this code creates the same sizes as the previous RankFilters
        radius = 1.75;
        else if (radius >= 2.5 && radius < 2.85) radius = 2.85;
        int r2 = (int) (radius * radius) + 1;
        kRadius = (int) (Math.sqrt(r2 + 1e-10));
        lineRadius = new int[2 * kRadius + 1];
        lineRadius[kRadius] = kRadius;
        kNPoints = 2 * kRadius + 1;
        for (int y = 1; y <= kRadius; y++) {
            int dx = (int) (Math.sqrt(r2 - y * y + 1e-10));
            lineRadius[kRadius + y] = dx;
            lineRadius[kRadius - y] = dx;
            kNPoints += 4 * dx + 2;
        }
    }
}
