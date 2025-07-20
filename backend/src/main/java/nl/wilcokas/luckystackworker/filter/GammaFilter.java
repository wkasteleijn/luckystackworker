package nl.wilcokas.luckystackworker.filter;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.model.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GammaFilter implements LSWFilter {
  @Override
  public boolean apply(
      ImagePlus image, Profile profile, boolean isMono, String... additionalArguments) {
    if (isApplied(profile, image)) {
      log.info(
          "Applying gamma correction with value {} to image {}", profile.getGamma(), image.getID());
      for (int slice = 1; slice <= image.getStack().getSize(); slice++) {
        ImageProcessor ip = getImageStackProcessor(image, slice);
        ip.gamma(2d - profile.getGamma().doubleValue());
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
    return profile.getGamma() != null && (profile.getGamma().compareTo(BigDecimal.ONE) != 0);
  }

  private ImageProcessor getImageStackProcessor(final ImagePlus img, final int stackPosition) {
    return img.getStack().getProcessor(stackPosition);
  }
}
