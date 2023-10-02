package nl.wilcokas.luckystackworker.filter;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import ij.ImagePlus;
import ij.ImageStack;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.util.Util;

@Slf4j
@Component
public class RGBBalanceFilter {

    private static final int STEP_SIZE = 64;

    public void apply(ImagePlus image, int amountRed, int amountGreen, int amountBlue, double purpleReductionAmount) {
        ImageStack stack = image.getStack();
        short[] redPixels = (short[]) stack.getProcessor(Constants.RED_LAYER_INDEX).getPixels();
        short[] greenPixels = (short[]) stack.getProcessor(Constants.GREEN_LAYER_INDEX).getPixels();
        short[] bluePixels = (short[]) stack.getProcessor(Constants.BLUE_LAYER_INDEX).getPixels();
        short[] redPixelsResult = new short[redPixels.length];
        short[] greenPixelsResult = new short[greenPixels.length];
        short[] bluePixelsResult = new short[bluePixels.length];

        for (int i = 0; i < redPixels.length; i++) {
            int newRedValue = Util.convertToUnsignedInt(redPixels[i]) - (amountRed * STEP_SIZE);
            int newGreenValue = Util.convertToUnsignedInt(greenPixels[i]) - (amountGreen * STEP_SIZE);
            int newBlueValue = Util.convertToUnsignedInt(bluePixels[i]) - (amountBlue * STEP_SIZE);
            int desaturatedValue = (newRedValue + newGreenValue + newBlueValue) / 3;
            double purpleCorrectionFactor = purpleReductionAmount > 0D ? getPurpleCorrectionFactor(newRedValue, newGreenValue, newBlueValue) : 0D;
            redPixelsResult[i] = getPixelResult(newRedValue, desaturatedValue, amountRed, purpleCorrectionFactor, purpleReductionAmount);
            greenPixelsResult[i] = getPixelResult(newGreenValue, desaturatedValue, amountGreen, purpleCorrectionFactor, purpleReductionAmount);
            bluePixelsResult[i] = getPixelResult(newBlueValue, desaturatedValue, amountBlue, purpleCorrectionFactor, purpleReductionAmount);
        }
        int numThreads = Runtime.getRuntime().availableProcessors();
        Executor executor = Executors.newFixedThreadPool(numThreads);
        executor.execute(() -> {
            for (int i = 0; i < redPixels.length; i++) {
                redPixels[i] = redPixelsResult[i];
            }
        });
        executor.execute(() -> {
            for (int i = 0; i < greenPixels.length; i++) {
                greenPixels[i] = greenPixelsResult[i];
            }
        });
        executor.execute(() -> {
            for (int i = 0; i < bluePixels.length; i++) {
                bluePixels[i] = bluePixelsResult[i];
            }
        });
        ((ExecutorService) executor).shutdown();
        try {
            ((ExecutorService) executor).awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            log.warn("Parallel thread execution was stopped: ", e);
        }

    }

    private short getPixelResult(int newValueUnsignedInt, int desaturatedValue, int correctionAmount, double purpleCorrectionFactor,
            double purpleReductionAmount) {
        int purpleCorrectedValue = (int) ((desaturatedValue * purpleCorrectionFactor) + (newValueUnsignedInt * (1 - purpleCorrectionFactor)));
        int finalNewValue = (int) ((purpleCorrectedValue * purpleReductionAmount) + (newValueUnsignedInt * (1 - purpleReductionAmount)));
        return Util.convertToShort(finalNewValue > Constants.MAX_INT_VALUE ? Constants.MAX_INT_VALUE : (finalNewValue < 0 ? 0 : finalNewValue));
    }

    private double getPurpleCorrectionFactor(int redValue, int greenValue, int blueValue) {
        double differenceWithGreen = 0D;
        double differenceRedBlue = 0D;
        if (greenValue < redValue && greenValue < blueValue) {
            if (blueValue < redValue) {
                differenceWithGreen = redValue - (greenValue * 0.5);
                differenceRedBlue = redValue - blueValue;
            } else {
                differenceWithGreen = blueValue - (greenValue * 0.5);
                differenceRedBlue = blueValue - redValue;
            }
            return 1D - (differenceRedBlue / differenceWithGreen);
        }
        return 0D;
    }
}
