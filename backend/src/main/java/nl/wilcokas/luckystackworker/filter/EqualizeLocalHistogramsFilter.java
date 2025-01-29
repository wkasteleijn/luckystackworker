package nl.wilcokas.luckystackworker.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.UnaryOperator;

import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.model.Profile;
import org.springframework.stereotype.Component;

import ij.ImagePlus;
import lombok.RequiredArgsConstructor;
import nl.wilcokas.luckystackworker.service.GmicService;

@RequiredArgsConstructor
@Slf4j
@Component
public class EqualizeLocalHistogramsFilter implements LSWFilter {

    private final GmicService gmicService;

    @Override
    public boolean apply(ImagePlus image, Profile profile, boolean isMono) {
        return apply(image, profile.getName(), profile.getEqualizeLocalHistogramsStrength(), profile.getScale(), profile.getRotationAngle());
    }

    @Override
    public boolean isSlow() {
        return true;
    }

    @Override
    public boolean isApplied(Profile profile, ImagePlus image) {
        return false;
    }

    private boolean apply(final ImagePlus image, final String profileName, final int strength, double scale, double rotationAngle) {
        if (strength > 0) {
            log.info("Applying equalize local historgrams with strength {} to image {}", strength, image.getID());
            gmicService.callGmicCli(image, profileName, scale, rotationAngle,
                    Arrays.asList("fx_equalize_local_histograms", "%s,2,4,100,4,1,16,0,50,50".formatted(strength)));
            return true;
        }
        return false;
    }

}
