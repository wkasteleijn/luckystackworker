package nl.wilcokas.luckystackworker.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.UnaryOperator;

import org.springframework.stereotype.Component;

import ij.ImagePlus;
import lombok.RequiredArgsConstructor;
import nl.wilcokas.luckystackworker.service.GmicService;

@RequiredArgsConstructor
@Component
public class EqualizeLocalHistogramsFilter {

    private final GmicService gmicService;

    public void apply(final ImagePlus image, final String profileName, final int strength, double scale)
            throws IOException, InterruptedException {
        if (strength>0) {
            gmicService.callGmicCli(image, profileName, scale,
                    Arrays.asList("fx_equalize_local_histograms", "%s,2,4,100,4,1,16,0,50,50".formatted(strength)));
        }
    }
}
