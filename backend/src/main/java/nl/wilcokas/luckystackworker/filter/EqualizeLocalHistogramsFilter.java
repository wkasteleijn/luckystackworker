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
        log.info("Applying equalize local historgrams with strength {} to image {}", profile.getEqualizeLocalHistogramsStrength(), image.getID());
        return apply(image, profile.getName(), profile.getEqualizeLocalHistogramsStrength(), profile.getScale());
    }

    @Override
    public boolean isSlow() {
        return true;
    }

    private boolean apply(final ImagePlus image, final String profileName, final int strength, double scale) {
        if (strength>0) {
            gmicService.callGmicCli(image, profileName, scale,
                    Arrays.asList("fx_equalize_local_histograms", "%s,2,4,100,4,1,16,0,50,50".formatted(strength)));
            return true;
        }
        return false;
    }

}
