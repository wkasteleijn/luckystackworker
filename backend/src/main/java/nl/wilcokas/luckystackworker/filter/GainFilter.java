package nl.wilcokas.luckystackworker.filter;

import ij.ImagePlus;
import ij.ImageStack;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.util.LswImageProcessingUtil;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class GainFilter implements LSWFilter {

    @Override
    public boolean apply(ImagePlus image, Profile profile, boolean isMono) throws IOException {
        if (isApplied(profile, image)) {
            log.info("Applying gain correction to image {}", image.getID());
            apply(image, profile.getGain());
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
        return profile.getGain() != 1.0;
    }

    private void apply(ImagePlus image, double gain) {
        int width = image.getWidth();
        int height = image.getHeight();

        ImageStack stack = image.getStack();
        short[] redPixels = (short[]) stack.getProcessor(Constants.RED_LAYER_INDEX).getPixels();
        short[] greenPixels = (short[]) stack.getProcessor(Constants.GREEN_LAYER_INDEX).getPixels();
        short[] bluePixels = (short[]) stack.getProcessor(Constants.BLUE_LAYER_INDEX).getPixels();
        int[] redResult = new int[redPixels.length];
        int[] greenResult = new int[greenPixels.length];
        int[] blueResult = new int[bluePixels.length];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                redResult[index] = LswImageProcessingUtil.convertToUnsignedInt(redPixels[index]);
                greenResult[index] = LswImageProcessingUtil.convertToUnsignedInt(greenPixels[index]);
                blueResult[index] = LswImageProcessingUtil.convertToUnsignedInt(bluePixels[index]);
                long scaledRed = (long) (redResult[index] * gain);
                long scaledGreen = (long) (greenResult[index] * gain);
                long scaledBlue = (long) (blueResult[index] * gain);
                redPixels[index] = LswImageProcessingUtil.convertToShort(scaledRed > 65535 ? 65535 : scaledRed);
                greenPixels[index] = LswImageProcessingUtil.convertToShort(scaledRed > 65535 ? 65535 : scaledGreen);
                bluePixels[index] = LswImageProcessingUtil.convertToShort(scaledBlue > 65535 ? 65535 : scaledBlue);
            }
        }
    }
}
