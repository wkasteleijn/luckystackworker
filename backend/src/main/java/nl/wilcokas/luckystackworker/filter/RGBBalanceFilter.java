package nl.wilcokas.luckystackworker.filter;

import org.springframework.stereotype.Component;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.util.Util;

@Component
public class RGBBalanceFilter {

    private static final int STEP_SIZE = 64;

    public void apply(ImagePlus image, int amountRed, int amountGreen, int amountBlue) {
        ImageStack stack = image.getStack();
        reduceLayerBrightness(stack, 1, amountRed);
        reduceLayerBrightness(stack, 2, amountGreen);
        reduceLayerBrightness(stack, 3, amountBlue);
    }

    private void reduceLayerBrightness(ImageStack stack, int layer, int value) {
        ImageProcessor p = stack.getProcessor(layer);
        short[] pixels = (short[]) p.getPixels();
        short[] pixelsResult = new short[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            int newValueUnsignedInt = Util.convertToUnsignedInt(pixels[i]) - (value * STEP_SIZE);
            pixelsResult[i] = Util.convertToShort(
                    newValueUnsignedInt > Constants.MAX_INT_VALUE ? Constants.MAX_INT_VALUE : (newValueUnsignedInt < 0 ? 0 : newValueUnsignedInt));
        }
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = pixelsResult[i];
        }
    }

}
