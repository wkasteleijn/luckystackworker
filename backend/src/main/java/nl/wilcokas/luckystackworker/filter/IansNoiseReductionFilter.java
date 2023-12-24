package nl.wilcokas.luckystackworker.filter;

import java.io.IOException;

import org.springframework.stereotype.Component;

import ij.ImagePlus;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.filter.settings.IansNoiseReductionParameters;

@Slf4j
@Component
public class IansNoiseReductionFilter {


    public void apply(ImagePlus image, final IansNoiseReductionParameters parameters) throws IOException {

        // Save image to temp file in the appdata folder

        // Apply filter, result in same appdata folder
        // (${user.home}/AppData/Local/LuckyStackWorker)
        Runtime.getRuntime().exec(".\\gmic\\gmic -input %s -div 65536 iain_nr_2019 1,0,0,0,0.5,1,1,%s,%s,%s,3,%s,0.5,4,0 -output %s");

        // Open file as new image
        // copy layers over to existing image
    }


}
