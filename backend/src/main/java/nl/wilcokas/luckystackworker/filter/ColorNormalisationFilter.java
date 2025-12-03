package nl.wilcokas.luckystackworker.filter;

import ij.ImagePlus;
import ij.ImageStack;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.util.LswImageProcessingUtil;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ColorNormalisationFilter implements LSWFilter {

    @Override
    public boolean apply(ImagePlus image, Profile profile, boolean isMono, String... additionalArguments) {
        if (isApplied(profile, image)) {
            log.info("Applying color balance normalization to image {}", image.getID());
            apply(image);
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
        return profile.getNormalizeColorBalance();
    }

    private void apply(ImagePlus image) {
        int width = image.getWidth();
        int height = image.getHeight();

        ImageStack stack = image.getStack();
        short[] redPixels =
                (short[]) stack.getProcessor(Constants.RED_LAYER_INDEX).getPixels();
        short[] greenPixels =
                (short[]) stack.getProcessor(Constants.GREEN_LAYER_INDEX).getPixels();
        short[] bluePixels =
                (short[]) stack.getProcessor(Constants.BLUE_LAYER_INDEX).getPixels();
        int[] redResult = new int[redPixels.length];
        int[] greenResult = new int[greenPixels.length];
        int[] blueResult = new int[bluePixels.length];

        // Calculate average pixel values for each channel
        long redSum = 0, greenSum = 0, blueSum = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                redResult[index] = LswImageProcessingUtil.convertToUnsignedInt(redPixels[index]);
                greenResult[index] = LswImageProcessingUtil.convertToUnsignedInt(greenPixels[index]);
                blueResult[index] = LswImageProcessingUtil.convertToUnsignedInt(bluePixels[index]);
                redSum += redResult[index];
                greenSum += greenResult[index];
                blueSum += blueResult[index];
            }
        }
        double redAvg = (double) redSum / (width * height);
        double greenAvg = (double) greenSum / (width * height);
        double blueAvg = (double) blueSum / (width * height);

        // Calculate scaling factors for each channel
        double redScale = greenAvg / redAvg;
        double blueScale = greenAvg / blueAvg;

        // Apply scaling to each channel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                long scaledRed = (long) (redResult[index] * redScale);
                long scaledBlue = (long) (blueResult[index] * blueScale);
                redPixels[index] = LswImageProcessingUtil.convertToShort(scaledRed > 65535 ? 65535 : scaledRed);
                bluePixels[index] = LswImageProcessingUtil.convertToShort(scaledBlue > 65535 ? 65535 : scaledBlue);
            }
        }
    }
}
