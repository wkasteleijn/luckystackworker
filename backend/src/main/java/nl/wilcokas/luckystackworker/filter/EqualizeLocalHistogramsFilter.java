package nl.wilcokas.luckystackworker.filter;

import ij.ImagePlus;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.service.GmicService;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class EqualizeLocalHistogramsFilter implements LSWFilter {

  private final GmicService gmicService;

  @Override
  public boolean apply(ImagePlus image, Profile profile, boolean isMono, String... additionalArguments) {
    if (isApplied(profile, image)) {
      log.info(
          "Applying equalize local historgrams with strength {} to image {}",
          profile.getEqualizeLocalHistogramsStrength(),
          image.getID());
      gmicService.callGmicCli(
          image,
          profile.getName(),
          profile.getScale(),
          profile.getRotationAngle(),
          Arrays.asList(
              "fx_equalize_local_histograms",
              "%s,2,4,100,4,1,16,0,50,50".formatted(profile.getEqualizeLocalHistogramsStrength())));
      return true;
    }
    return false;
  }

  @Override
  public boolean isSlow() {
    return true;
  }

  @Override
  public boolean isApplied(Profile profile, ImagePlus image) {
    return profile.getEqualizeLocalHistogramsStrength() > 0;
  }
}
