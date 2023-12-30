package nl.wilcokas.luckystackworker.filter;

import java.io.IOException;
import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import ij.ImagePlus;
import ij.ImageStack;
import ij.io.Opener;
import ij.process.ImageProcessor;
import nl.wilcokas.luckystackworker.exceptions.FilterException;
import nl.wilcokas.luckystackworker.filter.settings.IansNoiseReductionParameters;
import nl.wilcokas.luckystackworker.util.Util;

@Component
public class IansNoiseReductionFilter {

    public void apply(ImagePlus image, final String profileName, final IansNoiseReductionParameters parameters)
            throws IOException, InterruptedException {

        BigDecimal fineValue = parameters.getFine() == null ? BigDecimal.ZERO : parameters.getFine().divide(BigDecimal.valueOf(200));
        BigDecimal mediumValue = parameters.getMedium() == null ? BigDecimal.ZERO : parameters.getMedium().divide(BigDecimal.valueOf(200));
        BigDecimal largeValue = parameters.getLarge() == null ? BigDecimal.ZERO : parameters.getLarge().divide(BigDecimal.valueOf(200));

        String workFolder = Util.getIJFileFormat(System.getProperty("user.home")) + "/AppData/Local/LuckyStackWorker";
        String inputFile = workFolder + "/temp_in.tif";
        Util.saveImage(image, profileName, inputFile, Util.isPngRgbStack(image, inputFile), false, false, false);

        String outputFile = workFolder + "/temp_out.tif";
        int recoveryAlgorithm = parameters.getRecovery() != null && BigDecimal.ZERO.compareTo(parameters.getRecovery()) < 0 ? 1 : 0;
        if (new ProcessBuilder(
                "./gmic/gmic.exe", "-input", inputFile, "-div", "65536",
                "iain_nr_2019",
                "1,0,0,0,0.5,1,1,%s,%s,%s,3,%s,%s,4,0".formatted(fineValue, mediumValue, largeValue, recoveryAlgorithm, parameters.getRecovery()),
                "-mul", "65536", "-output", outputFile + ",int16").start().waitFor() != 0) {
            throw new FilterException("G'mic CLI execution failed");
        }
        ImagePlus outputImage = new Opener().openImage(Util.getIJFileFormat(outputFile));
        ImageStack outputStack = outputImage.getStack();
        ImageStack imageStack = image.getStack();
        for (int layer = 1; layer <= outputStack.getSize(); layer++) {
            ImageProcessor newProcessor = outputStack.getProcessor(layer);
            short[] pixelsNew = (short[]) newProcessor.getPixels();
            short[] pixels = (short[]) imageStack.getProcessor(layer).getPixels();
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = pixelsNew[i];
            }
        }
    }
}
