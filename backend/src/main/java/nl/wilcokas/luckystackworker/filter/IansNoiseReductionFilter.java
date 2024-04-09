package nl.wilcokas.luckystackworker.filter;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;

import org.springframework.stereotype.Component;

import ij.ImagePlus;
import lombok.RequiredArgsConstructor;
import nl.wilcokas.luckystackworker.filter.settings.IansNoiseReductionParameters;
import nl.wilcokas.luckystackworker.service.GmicService;

@RequiredArgsConstructor
@Component
public class IansNoiseReductionFilter {

    private final GmicService gmicService;

    public void apply(final ImagePlus image, final String profileName, final IansNoiseReductionParameters parameters, double scale)
            throws IOException, InterruptedException {
        BigDecimal fineValue = parameters.getFine() == null ? BigDecimal.ZERO : parameters.getFine().divide(BigDecimal.valueOf(200));
        BigDecimal mediumValue = parameters.getMedium() == null ? BigDecimal.ZERO : parameters.getMedium().divide(BigDecimal.valueOf(200));
        BigDecimal largeValue = parameters.getLarge() == null ? BigDecimal.ZERO : parameters.getLarge().divide(BigDecimal.valueOf(200));
        BigDecimal recovery = parameters.getRecovery() == null ? BigDecimal.ZERO : parameters.getRecovery();
        int recoveryAlgorithm = parameters.getRecovery() != null && BigDecimal.ZERO.compareTo(parameters.getRecovery()) < 0 ? 1 : 0;
        gmicService.callGmicCli(image, profileName, scale, Arrays.asList("iain_nr_2019",
                "1,0,0,0,0.5,1,1,%s,%s,%s,3,%s,%s,4,0".formatted(fineValue, mediumValue, largeValue, recoveryAlgorithm, recovery)));
    }

}
