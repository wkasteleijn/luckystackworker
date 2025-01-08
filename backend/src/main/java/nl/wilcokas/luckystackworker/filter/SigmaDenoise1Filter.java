package nl.wilcokas.luckystackworker.filter;

import ij.ImagePlus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.filter.sigma.SigmaFilterPlus;
import nl.wilcokas.luckystackworker.model.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class SigmaDenoise1Filter implements LSWFilter  {

    private final SigmaFilterPlus sigmaFilterPlus;

    @Override
    public boolean apply(ImagePlus image, Profile profile, boolean isMono) throws IOException {
        if (Constants.DENOISE_ALGORITHM_SIGMA1.equals(profile.getDenoiseAlgorithm1())) {
            log.info("Applying Sigma denoise mode 1 with value {} to image {}", profile.getDenoise1Amount(), image.getID());
            sigmaFilterPlus.applyDenoise1(image, profile);
            return true;
        }
        return false;
    }

    @Override
    public boolean isSlow() {
        return false;
    }
}
