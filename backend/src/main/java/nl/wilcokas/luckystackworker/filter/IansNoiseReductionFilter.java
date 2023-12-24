package nl.wilcokas.luckystackworker.filter;

import java.io.IOException;

import org.springframework.stereotype.Component;

import ij.ImagePlus;
import ij.ImageStack;
import ij.io.Opener;
import ij.process.ImageProcessor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.filter.settings.IansNoiseReductionParameters;
import nl.wilcokas.luckystackworker.util.Util;

@Slf4j
@Component
public class IansNoiseReductionFilter {

    public void apply(ImagePlus image, final String profileName, final IansNoiseReductionParameters parameters, boolean fromWorker)
            throws IOException {

        String workFolder = System.getProperty("user.dir") + "/AppData/Local/LuckyStackWorker";
        String inputFile = workFolder + "/temp_in.tif";
        Util.saveImage(image, profileName, inputFile, Util.isPngRgbStack(image, inputFile), false, false, fromWorker);

        String outputFile = workFolder + "/temp_out.tif";
        Runtime.getRuntime().exec(".\\gmic\\gmic -input %s -div 65536 iain_nr_2019 1,0,0,0,0.5,1,1,%s,%s,%s,3,%s,0.5,4,0 -output %s"
                .formatted(inputFile, parameters.getFine(), parameters.getMedium(), parameters.getLarge(), parameters.isRecover() ? 1 : 0,
                        outputFile));

        // Open file as new image
        ImagePlus outputImage = new Opener().openImage(Util.getIJFileFormat(outputFile));
        ImageStack outputStack = outputImage.getStack();
        ImageStack imageStack = image.getStack();
        // copy layers over to existing image
        for (int layer = 1; layer <= outputStack.getSize(); layer++) {
            ImageProcessor p = outputStack.getProcessor(layer);
            short[] pixels = (short[]) p.getPixels();
            imageStack.getProcessor(layer).setPixels(pixels);
        }
    }


}
