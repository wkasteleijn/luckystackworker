package nl.wilcokas.luckystackworker.filter;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import nl.wilcokas.luckystackworker.util.LswUtil;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Component
public class BilateralDenoiseFilter {

        public void apply(ImagePlus image, int radius, double sigmaColor, double sigmaSpace) {
            ImageStack stack = image.getStack();

            // Run every stack in a seperate thread to increase performance.
            Executor executor = LswUtil.getParallelExecutor();
            executor.execute(() -> applyToChannel( (ShortProcessor)stack.getProcessor(1), radius, sigmaColor, sigmaSpace));
            executor.execute(() -> applyToChannel( (ShortProcessor)stack.getProcessor(2), radius, sigmaColor, sigmaSpace));
            executor.execute(() -> applyToChannel( (ShortProcessor)stack.getProcessor(3), radius, sigmaColor, sigmaSpace));

            LswUtil.stopAndAwaitParallelExecutor(executor);
        }

        private void applyToChannel(ShortProcessor ip, int radius, double sigmaColor, double sigmaSpace) {
            //ImageProcessor ip = stack.getProcessor(channel);

            // Get the dimensions of the image
            int width = ip.getWidth();
            int height = ip.getHeight();

            short[] resultPixels = new short[ip.getPixelCount()];

            // Parameters for the bilateral filter
//            int radius = 5; // Filter radius
//            double sigmaColor = 75; // Color variance
//            double sigmaSpace = 75; // Spatial variance

            // Apply the bilateral filter
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    double[] newPixel = applyBilateralFilter(ip, x, y, radius, sigmaColor, sigmaSpace);
                    resultPixels[y * width + x] = (short) Math.round(newPixel[0]);
                }
            }
            short[] pixels = (short[]) ip.getPixels();
            System.arraycopy(resultPixels, 0, pixels, 0, ip.getPixelCount());
        }

        private double[] applyBilateralFilter(ImageProcessor ip, int x, int y, int radius, double sigmaColor, double sigmaSpace) {
            double weightSum = 0;
            double intensitySum = 0;
            int centerPixel = ip.getPixel(x, y);

            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    int nx = x + dx;
                    int ny = y + dy;

                    // Ensure the neighbor pixel is within the image bounds
                    if (nx >= 0 && nx < ip.getWidth() && ny >= 0 && ny < ip.getHeight()) {
                        int neighborPixel = ip.getPixel(nx, ny);

                        // Calculate the Gaussian weights
                        double spatialWeight = Math.exp(-(dx * dx + dy * dy) / (2 * sigmaSpace * sigmaSpace));
                        double colorWeight = Math.exp(-(Math.pow(centerPixel - neighborPixel, 2)) / (2 * sigmaColor * sigmaColor));
                        double weight = spatialWeight * colorWeight;

                        // Accumulate the weighted intensity
                        intensitySum += weight * neighborPixel;
                        weightSum += weight;
                    }
                }
            }

            // Return the normalized intensity
            return new double[] { intensitySum / weightSum };
        }
}
