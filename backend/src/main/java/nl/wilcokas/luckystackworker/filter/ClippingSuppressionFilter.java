package nl.wilcokas.luckystackworker.filter;

import ij.ImagePlus;
import ij.ImageStack;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.util.LswImageProcessingUtil;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static nl.wilcokas.luckystackworker.constants.Constants.MAX_DOUBLE_VALUE;
import static nl.wilcokas.luckystackworker.constants.Constants.MAX_INT_VALUE;

@Slf4j
@Component
public class ClippingSuppressionFilter implements LSWFilter {

    @Override
    public boolean apply(ImagePlus image, Profile profile, boolean isMono) {
        if (isApplied(profile, image)) {
            log.info("Applying clipping suppression to image {}", image.getID());
            apply(image, profile.getClippingSuppression());
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
        return profile.getClippingSuppression() != 0.0;
    }

    private void apply(ImagePlus image, double clippingSuppression) {
        int width = image.getWidth();
        int height = image.getHeight();

        ImageStack stack = image.getStack();
        short[] redPixels = (short[]) stack.getProcessor(Constants.RED_LAYER_INDEX).getPixels();
        short[] greenPixels = (short[]) stack.getProcessor(Constants.GREEN_LAYER_INDEX).getPixels();
        short[] bluePixels = (short[]) stack.getProcessor(Constants.BLUE_LAYER_INDEX).getPixels();
        int[] red = new int[redPixels.length];
        int[] green = new int[greenPixels.length];
        int[] blue = new int[bluePixels.length];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                red[index] = LswImageProcessingUtil.convertToUnsignedInt(redPixels[index]);
                green[index] = LswImageProcessingUtil.convertToUnsignedInt(greenPixels[index]);
                blue[index] = LswImageProcessingUtil.convertToUnsignedInt(bluePixels[index]);
                long scaledRed = (long) (red[index] * getRelativeGainFactor(red[index], clippingSuppression));
                long scaledGreen = (long) (green[index] * getRelativeGainFactor(green[index], clippingSuppression));
                long scaledBlue = (long) (blue[index] * getRelativeGainFactor(blue[index], clippingSuppression));
                redPixels[index] = LswImageProcessingUtil.convertToShort(scaledRed > MAX_INT_VALUE ? MAX_INT_VALUE : scaledRed);
                greenPixels[index] = LswImageProcessingUtil.convertToShort(scaledGreen > MAX_INT_VALUE ? MAX_INT_VALUE : scaledGreen);
                bluePixels[index] = LswImageProcessingUtil.convertToShort(scaledBlue > MAX_INT_VALUE ? MAX_INT_VALUE : scaledBlue);
            }
        }
    }

    private double getRelativeGainFactor(double pxValue, double clippingSuppressionFactorPercentage) {
        double suppressionFactor = clippingSuppressionFactorPercentage / 100D;
        return (((MAX_DOUBLE_VALUE - pxValue) / MAX_DOUBLE_VALUE) * suppressionFactor) + (1D - suppressionFactor);
    }
}
