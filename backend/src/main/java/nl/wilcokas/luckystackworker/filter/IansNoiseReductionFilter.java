package nl.wilcokas.luckystackworker.filter;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.function.UnaryOperator;

import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.model.Profile;
import org.springframework.stereotype.Component;

import ij.ImagePlus;
import lombok.RequiredArgsConstructor;
import nl.wilcokas.luckystackworker.filter.settings.IansNoiseReductionParameters;
import nl.wilcokas.luckystackworker.service.GmicService;

@RequiredArgsConstructor
@Slf4j
@Component
public class IansNoiseReductionFilter implements LSWFilter {

    private final GmicService gmicService;

    @Override
    public boolean apply(ImagePlus image, Profile profile, boolean isMono) throws IOException {
        if (isApplied(profile, image)) {
            log.info("Applying Ian's noise reduction to image {}", image.getID());
            IansNoiseReductionParameters parameters = IansNoiseReductionParameters.builder().fine(profile.getIansAmount()).medium(profile.getIansAmountMid())
                    .large(BigDecimal.ZERO).recovery(profile.getIansRecovery()).iterations(profile.getIansIterations()).build();
            apply(image, profile.getName(), parameters, profile.getScale());
            return true;
        }
        return false;
    }

    @Override
    public boolean isApplied(Profile profile, ImagePlus image) {
        return Constants.DENOISE_ALGORITHM_IANS.equals(profile.getDenoiseAlgorithm1());
    }

    @Override
    public boolean isSlow() {
        return true;
    }

    private void apply(final ImagePlus image, final String profileName, final IansNoiseReductionParameters parameters, double scale) {
        for (int i = 0; i < parameters.getIterations(); i++) {
            BigDecimal fineValue = parameters.getFine() == null ? BigDecimal.ZERO : parameters.getFine().divide(BigDecimal.valueOf(200));
            BigDecimal mediumValue = parameters.getMedium() == null ? BigDecimal.ZERO : parameters.getMedium().divide(BigDecimal.valueOf(200));
            BigDecimal largeValue = parameters.getLarge() == null ? BigDecimal.ZERO : parameters.getLarge().divide(BigDecimal.valueOf(200));
            BigDecimal recovery = parameters.getRecovery() == null ? BigDecimal.ZERO : parameters.getRecovery();
            int recoveryAlgorithm = parameters.getRecovery() != null && BigDecimal.ZERO.compareTo(parameters.getRecovery()) < 0 ? 1 : 0;
            gmicService.callGmicCli(image, profileName, scale, Arrays.asList("iain_nr_2019",
                    "1,0,0,0,0.5,1,1,%s,%s,%s,3,%s,%s,4,0".formatted(fineValue, mediumValue, largeValue, recoveryAlgorithm, recovery)));
        }
    }
}
