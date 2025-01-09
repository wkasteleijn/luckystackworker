package nl.wilcokas.luckystackworker.filter;

import ij.ImagePlus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.filter.settings.LSWSharpenMode;
import nl.wilcokas.luckystackworker.filter.settings.LSWSharpenParameters;
import nl.wilcokas.luckystackworker.filter.settings.UnsharpMaskParameters;
import nl.wilcokas.luckystackworker.model.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;

@RequiredArgsConstructor
@Slf4j
@Component
public class LocalContrastFilter implements  LSWFilter {

    private final LSWSharpenFilter sharpenFilter;

    @Override
    public boolean apply(ImagePlus image, Profile profile, boolean isMono) throws IOException {
        LSWSharpenMode mode = (profile.getLocalContrastMode() == null) ? LSWSharpenMode.LUMINANCE
                : LSWSharpenMode.valueOf(profile.getLocalContrastMode());
        boolean filterApplied = false;
        if (profile.getLocalContrastFine() != 0) {
            applyLocalContrast(image, profile.getLocalContrastFine(), Constants.LOCAL_CONTRAST_FINE_RADIUS, mode);
            filterApplied = true;
        }
        if (profile.getLocalContrastMedium() != 0) {
            applyLocalContrast(image, profile.getLocalContrastMedium(), Constants.LOCAL_CONTRAST_MEDIUM_RADIUS, mode);
            filterApplied = true;
        }
        if (profile.getLocalContrastLarge() != 0) {
            applyLocalContrast(image, profile.getLocalContrastLarge(), Constants.LOCAL_CONTRAST_LARGE_RADIUS, mode);
            filterApplied = true;
        }
        return filterApplied;
    }

    @Override
    public boolean isSlow() {
        return false;
    }

    private void applyLocalContrast(final ImagePlus image, int amount, BigDecimal radius, LSWSharpenMode localContrastMode) {
        log.info("Applying local contrast with mode {}, radius {} amount {} to image {}", localContrastMode, radius, amount, image.getID());
        float famount = (amount) / 100f;
        UnsharpMaskParameters usParams = UnsharpMaskParameters.builder()
                .radiusRed(radius.doubleValue()).amountRed(famount).iterationsRed(1)
                .radiusGreen(radius.doubleValue()).amountGreen(famount).iterationsGreen(1)
                .radiusBlue(radius.doubleValue()).amountBlue(famount).iterationsBlue(1)
                .build();
        LSWSharpenParameters parameters = LSWSharpenParameters.builder().includeBlue(true).includeGreen(true).includeRed(true).individual(false)
                .saturation(1f).unsharpMaskParameters(usParams).mode(localContrastMode).build();
        sharpenFilter.applyRGBMode(image, parameters.getUnsharpMaskParameters());
    }

}
