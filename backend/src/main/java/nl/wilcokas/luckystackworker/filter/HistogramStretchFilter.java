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
public class HistogramStretchFilter implements LSWFilter {

  @Override
  public boolean apply(
      ImagePlus image, Profile profile, boolean isMono, String... additionalArguments) {
    if (isApplied(profile, image)) {
      log.info(
          "Applying contrast increase with factor {} to image {}",
          profile.getContrast(),
          image.getID());

      // Contrast
      double newMin = Math.round((profile.getContrast()) * (16384.0 / 100.0));
      double newMax = 65536 - newMin;

      // Brightness
      newMax = Math.round(newMax - (profile.getBrightness()) * (49152.0 / 100.0));

      apply(
          image,
          newMin,
          newMax,
          profile.getLightness(),
          profile.getBackground(),
          profile.isPreserveDarkBackground());
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
    return profile.getContrast() != 0
        || profile.getBrightness() != 0
        || profile.getLightness() != 0
        || profile.getBackground() != 0;
  }

  private void apply(
      ImagePlus image,
      double minValue,
      double maxValue,
      double lightnessIncreaseValue,
      double backgroundCutoffFactor,
      boolean preserveDarkBackground) {
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

    double lowestValue = 16384 * (backgroundCutoffFactor / 100D);

    // Apply scaling to each channel
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int index = y * width + x;
        redResult[index] = LswImageProcessingUtil.convertToUnsignedInt(redPixels[index]);
        greenResult[index] = LswImageProcessingUtil.convertToUnsignedInt(greenPixels[index]);
        blueResult[index] = LswImageProcessingUtil.convertToUnsignedInt(bluePixels[index]);
        int scaledRed =
            LswImageProcessingUtil.preventBackgroundFromLightingUp(
                redResult[index],
                (lightnessIncreaseValue * 256) + (redResult[index] * factor) - minLong,
                lowestValue,
                preserveDarkBackground);
        int scaledGreen =
            LswImageProcessingUtil.preventBackgroundFromLightingUp(
                greenResult[index],
                (lightnessIncreaseValue * 256) + (greenResult[index] * factor) - minLong,
                lowestValue,
                preserveDarkBackground);
        int scaledBlue =
            LswImageProcessingUtil.preventBackgroundFromLightingUp(
                blueResult[index],
                (lightnessIncreaseValue * 256) + (blueResult[index] * factor) - minLong,
                lowestValue,
                preserveDarkBackground);
        redPixels[index] =
            LswImageProcessingUtil.convertToShort(
                scaledRed > 65535 ? 65535 : scaledRed < 0 ? 0 : scaledRed);
        greenPixels[index] =
            LswImageProcessingUtil.convertToShort(
                scaledGreen > 65535 ? 65535 : scaledGreen < 0 ? 0 : scaledGreen);
        bluePixels[index] =
            LswImageProcessingUtil.convertToShort(
                scaledBlue > 65535 ? 65535 : scaledBlue < 0 ? 0 : scaledBlue);
      }
    }
  }
}
