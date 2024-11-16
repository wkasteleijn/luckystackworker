package nl.wilcokas.luckystackworker.filter;

import ij.ImagePlus;
import ij.ImageStack;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.util.LswImageProcessingUtil;
import org.springframework.stereotype.Component;

import static nl.wilcokas.luckystackworker.constants.Constants.MINIMUK_DARK_TRESHOLD;

@Slf4j
@Component
public class HistogramStretchFilter {
    public void apply(ImagePlus image, double minValue, double maxValue, double lightnessIncreaseValue, double backgroundCutoffFactor) {
        int width = image.getWidth();
        int height = image.getHeight();

        ImageStack stack = image.getStack();
        short[] redPixels = (short[]) stack.getProcessor(Constants.RED_LAYER_INDEX).getPixels();
        short[] greenPixels = (short[]) stack.getProcessor(Constants.GREEN_LAYER_INDEX).getPixels();
        short[] bluePixels = (short[]) stack.getProcessor(Constants.BLUE_LAYER_INDEX).getPixels();
        int[] redResult = new int[redPixels.length];
        int[] greenResult = new int[greenPixels.length];
        int[] blueResult = new int[bluePixels.length];

        double factor = 65535 / (maxValue - minValue);
        long minLong = (long) minValue;

        double lowestValue = 16384 * (backgroundCutoffFactor/100D);

        // Apply scaling to each channel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                redResult[index] = LswImageProcessingUtil.convertToUnsignedInt(redPixels[index]);
                greenResult[index] = LswImageProcessingUtil.convertToUnsignedInt(greenPixels[index]);
                blueResult[index] = LswImageProcessingUtil.convertToUnsignedInt(bluePixels[index]);
                int scaledRed = LswImageProcessingUtil.preventBackgroundFromLightingUp(redResult[index], (lightnessIncreaseValue * 256) + (redResult[index] * factor) - minLong, lowestValue);
                int scaledGreen = LswImageProcessingUtil.preventBackgroundFromLightingUp(greenResult[index], (lightnessIncreaseValue * 256) + (greenResult[index] * factor) - minLong, lowestValue);
                int scaledBlue = LswImageProcessingUtil.preventBackgroundFromLightingUp(blueResult[index], (lightnessIncreaseValue * 256) + (blueResult[index] * factor) - minLong, lowestValue);
                redPixels[index] = LswImageProcessingUtil.convertToShort(scaledRed > 65535 ? 65535 : scaledRed < 0 ? 0 : scaledRed);
                greenPixels[index] = LswImageProcessingUtil.convertToShort(scaledGreen > 65535 ? 65535 : scaledGreen < 0 ? 0 : scaledGreen);
                bluePixels[index] = LswImageProcessingUtil.convertToShort(scaledBlue > 65535 ? 65535 : scaledBlue < 0 ? 0 : scaledBlue);
            }
        }
    }
}
