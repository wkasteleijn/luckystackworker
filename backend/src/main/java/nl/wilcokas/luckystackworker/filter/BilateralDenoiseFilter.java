package nl.wilcokas.luckystackworker.filter;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ShortProcessor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.exceptions.FilterException;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.service.dto.OpenImageModeEnum;
import nl.wilcokas.luckystackworker.util.LswFileUtil;
import nl.wilcokas.luckystackworker.util.LswImageProcessingUtil;
import nl.wilcokas.luckystackworker.util.LswUtil;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Filter;

@Slf4j
@Component
public class BilateralDenoiseFilter implements LSWFilter {

    @Override
    public boolean apply(ImagePlus image, Profile profile, boolean isMono) {
        if (isApplied(profile, image)) {
            log.info("Applying bilateral denoise filter to image: {}", image.getTitle());
            for (int i = 1; i <= profile.getBilateralIterations(); i++) {
                doApply(image, profile);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isSlow() {
        return false;
    }

    @Override
    public boolean isApplied(Profile profile, ImagePlus image) {
        return Constants.DENOISE_ALGORITHM_BILATERAL.equals(profile.getDenoiseAlgorithm1());
    }

    private void doApply(ImagePlus image, Profile profile) {
        ImageStack stack = image.getStack();
        try {
            // Run every stack in a seperate thread to increase performance.
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                CompletableFuture<?>[] futures = new CompletableFuture[3];
                futures[0] = CompletableFuture.runAsync(() -> applyToChannel((ShortProcessor) stack.getProcessor(1), profile.getBilateralRadius(), profile.getBilateralSigmaColor() * 10D, 1), executor);
                futures[1] = CompletableFuture.runAsync(() -> applyToChannel((ShortProcessor) stack.getProcessor(2), profile.getBilateralRadiusGreen(), profile.getBilateralSigmaColorGreen() * 10D, 1), executor);
                futures[2] = CompletableFuture.runAsync(() -> applyToChannel((ShortProcessor) stack.getProcessor(3), profile.getBilateralRadiusBlue(), profile.getBilateralSigmaColorBlue() * 10D, 1), executor);
                CompletableFuture.allOf(futures).get();
            }
        } catch (InterruptedException | ExecutionException e) {  // NOSONAR
            throw new FilterException(e.getMessage());
        }
    }

    private void applyToChannel(ShortProcessor ip, int radius, double sigmaColor, double sigmaSpace) {
        short[] newPixels = new short[ip.getPixelCount()];
        int width = ip.getWidth();
        int height = ip.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                newPixels[width * y + x] = LswImageProcessingUtil.convertToShort((long) applyBilateralFilter(ip, x, y, radius, sigmaColor, sigmaSpace));
            }
        }
        System.arraycopy(newPixels, 0, ip.getPixels(), 0, newPixels.length);
    }

    private double applyBilateralFilter(ShortProcessor ip, int x, int y, int radius, double sigmaColor, double sigmaSpace) {
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
        return intensitySum / weightSum;
    }

    public static void main(String[] args) throws Exception {
        ImagePlus image = LswFileUtil
                .openImage("C:\\Users\\wkast\\archive\\Jup\\testsession\\denoise_test2.tif", OpenImageModeEnum.RGB, 1, img -> img).getLeft();

        image.show();
        // Set exposure back to original value
        image.setDefault16bitRange(16);
        image.resetDisplayRange();

        BilateralDenoiseFilter filter = new BilateralDenoiseFilter();
        Profile profile = Profile.builder().bilateralRadius(2).bilateralIterations(1).bilateralSigmaColor(25000).bilateralSigmaSpace(40000).build();
        filter.doApply(image, profile);
        image.updateAndDraw();

        LswFileUtil.saveImage(image, "jup", "C:/Users/wkast/archive/Jup/testsession/jup_denoised2.tif", true, false, false, false);

        LswUtil.waitMilliseconds(5000);

        System.exit(0);

    }
}
