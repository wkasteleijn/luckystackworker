package nl.wilcokas.luckystackworker.filter;

import ij.ImagePlus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.filter.sigma.SigmaFilterPlus;
import nl.wilcokas.luckystackworker.model.Profile;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SigmaDenoise2Filter implements LSWFilter {

    private final SigmaFilterPlus sigmaFilterPlus;

    public SigmaDenoise2Filter() {
        this.sigmaFilterPlus = new SigmaFilterPlus();
    }

    @Override
    public boolean apply(ImagePlus image, Profile profile, boolean isMono, String... additionalArguments) {
        if (isApplied(profile, image)) {
            int iterations = profile.getDenoise2Iterations() == 0 ? 1 : profile.getDenoise2Iterations();
            log.info(
                    "Applying Sigma denoise mode 2 with radius {} and {} iterations to image {}",
                    profile.getDenoise2Radius(),
                    iterations,
                    image.getID());
            sigmaFilterPlus.applyDenoise2(image, profile);
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
        return Constants.DENOISE_ALGORITHM_SIGMA2.equals(profile.getDenoiseAlgorithm2());
    }
}
