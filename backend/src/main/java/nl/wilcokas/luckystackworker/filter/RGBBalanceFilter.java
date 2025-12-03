package nl.wilcokas.luckystackworker.filter;

import ij.ImagePlus;
import ij.ImageStack;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.exceptions.FilterException;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.util.LswImageProcessingUtil;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RGBBalanceFilter implements LSWFilter {

    private static final int STEP_SIZE = 64;

    @Override
    public boolean apply(ImagePlus image, Profile profile, boolean isMono, String... additionalArguments) {
        if (isApplied(profile, image)) {
            log.info(
                    "Applying RGB balance correction to image {} with values R {}, G {}, B {}",
                    image.getID(),
                    profile.getRed(),
                    profile.getGreen(),
                    profile.getBlue());
            apply(
                    image,
                    profile.getRed().intValue(),
                    profile.getGreen().intValue(),
                    profile.getBlue().intValue(),
                    profile.getPurple().intValue() / 255D,
                    profile.getPreserveDarkBackground());
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
        return ((profile.getRed() != null && (!profile.getRed().equals(BigDecimal.ZERO)))
                || (profile.getGreen() != null && (!profile.getGreen().equals(BigDecimal.ZERO)))
                || (profile.getBlue() != null && (!profile.getBlue().equals(BigDecimal.ZERO))));
    }

    public void apply(
            ImagePlus image,
            int amountRed,
            int amountGreen,
            int amountBlue,
            double purpleReductionAmount,
            boolean preserveDarkBackground) {
        ImageStack stack = image.getStack();
        short[] redPixels =
                (short[]) stack.getProcessor(Constants.RED_LAYER_INDEX).getPixels();
        short[] greenPixels =
                (short[]) stack.getProcessor(Constants.GREEN_LAYER_INDEX).getPixels();
        short[] bluePixels =
                (short[]) stack.getProcessor(Constants.BLUE_LAYER_INDEX).getPixels();
        short[] redPixelsResult = new short[redPixels.length];
        short[] greenPixelsResult = new short[greenPixels.length];
        short[] bluePixelsResult = new short[bluePixels.length];

        for (int i = 0; i < redPixels.length; i++) {
            int currentRedValue = LswImageProcessingUtil.convertToUnsignedInt(redPixels[i]);
            int currentGreenValue = LswImageProcessingUtil.convertToUnsignedInt(greenPixels[i]);
            int currentBlueValue = LswImageProcessingUtil.convertToUnsignedInt(bluePixels[i]);

            int newRedValue = LswImageProcessingUtil.preventBackgroundFromLightingUp(
                    currentRedValue, currentRedValue - (amountRed * STEP_SIZE), 0, preserveDarkBackground);
            int newGreenValue = LswImageProcessingUtil.preventBackgroundFromLightingUp(
                    currentGreenValue, currentGreenValue - (amountGreen * STEP_SIZE), 0, preserveDarkBackground);
            int newBlueValue = LswImageProcessingUtil.preventBackgroundFromLightingUp(
                    currentBlueValue, currentBlueValue - (amountBlue * STEP_SIZE), 0, preserveDarkBackground);

            int desaturatedValue = (newRedValue + newGreenValue + newBlueValue) / 3;
            double purpleCorrectionFactor = purpleReductionAmount > 0D
                    ? getPurpleCorrectionFactor(newRedValue, newGreenValue, newBlueValue)
                    : 0D;
            redPixelsResult[i] =
                    getPixelResult(newRedValue, desaturatedValue, purpleCorrectionFactor, purpleReductionAmount);
            greenPixelsResult[i] =
                    getPixelResult(newGreenValue, desaturatedValue, purpleCorrectionFactor, purpleReductionAmount);
            bluePixelsResult[i] =
                    getPixelResult(newBlueValue, desaturatedValue, purpleCorrectionFactor, purpleReductionAmount);
        }

        try {
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                CompletableFuture<?>[] futures = new CompletableFuture[3];
                futures[0] = CompletableFuture.runAsync(
                        () -> {
                            for (int i = 0; i < redPixels.length; i++) {
                                redPixels[i] = redPixelsResult[i];
                            }
                        },
                        executor);
                futures[1] = CompletableFuture.runAsync(
                        () -> {
                            for (int i = 0; i < greenPixels.length; i++) {
                                greenPixels[i] = greenPixelsResult[i];
                            }
                        },
                        executor);
                futures[2] = CompletableFuture.runAsync(
                        () -> {
                            for (int i = 0; i < bluePixels.length; i++) {
                                bluePixels[i] = bluePixelsResult[i];
                            }
                        },
                        executor);
                CompletableFuture.allOf(futures).get();
            }
        } catch (InterruptedException | ExecutionException e) { // NOSONAR
            throw new FilterException(e.getMessage());
        }
    }

    private short getPixelResult(
            int newValueUnsignedInt,
            int desaturatedValue,
            double purpleCorrectionFactor,
            double purpleReductionAmount) {
        int purpleCorrectedValue = (int)
                ((desaturatedValue * purpleCorrectionFactor) + (newValueUnsignedInt * (1 - purpleCorrectionFactor)));
        int finalNewValue = (int)
                ((purpleCorrectedValue * purpleReductionAmount) + (newValueUnsignedInt * (1 - purpleReductionAmount)));
        return LswImageProcessingUtil.convertToShort(
                finalNewValue > Constants.MAX_INT_VALUE
                        ? Constants.MAX_INT_VALUE
                        : (finalNewValue < 0 ? 0 : finalNewValue));
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
