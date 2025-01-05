package nl.wilcokas.luckystackworker.filter;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.model.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;

@Slf4j
@Component
public class GammaFilter implements LSWFilter {
    @Override
    public void apply(ImagePlus image, Profile profile, boolean isMono) throws IOException {
        if (profile.getGamma() != null && (profile.getGamma().compareTo(BigDecimal.ONE) != 0)) {
            log.info("Applying gamma correction with value {} to image {}", profile.getGamma(), image.getID());
            for (int slice = 1; slice <= image.getStack().getSize(); slice++) {
                ImageProcessor ip = getImageStackProcessor(image, slice);
                ip.gamma(2d - profile.getGamma().doubleValue());
            }
        }
    }

    private ImageProcessor getImageStackProcessor(final ImagePlus img, final int stackPosition) {
        return img.getStack().getProcessor(stackPosition);
    }

}
