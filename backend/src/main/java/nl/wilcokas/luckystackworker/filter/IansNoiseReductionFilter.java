package nl.wilcokas.luckystackworker.filter;

import java.io.IOException;

import org.springframework.stereotype.Component;

import ij.ImagePlus;
import ij.ImageStack;
import ij.io.Opener;
import ij.process.ImageProcessor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.exceptions.FilterException;
import nl.wilcokas.luckystackworker.filter.settings.IansNoiseReductionParameters;
import nl.wilcokas.luckystackworker.util.Util;

@Slf4j
@Component
public class IansNoiseReductionFilter {

    public void apply(ImagePlus image, final String profileName, final IansNoiseReductionParameters parameters, boolean fromWorker)
            throws IOException, InterruptedException {

        double fineValue = parameters.getFine() / 200D;
        double mediumValue = parameters.getMedium() / 200D;
        double largeValue = parameters.getLarge() / 200D;

        String workFolder = System.getProperty("user.home") + "\\AppData\\Local\\LuckyStackWorker";
        String inputFile = workFolder + "\\temp_in.tif";
        Util.saveImage(image, profileName, inputFile, Util.isPngRgbStack(image, inputFile), false, false, fromWorker);

        String outputFile = workFolder + "\\temp_out.tif";
        ProcessBuilder processBuilder = new ProcessBuilder(
                "./gmic/gmic.exe", "-input", inputFile, "-div", "65536",
                "iain_nr_2019", "1,0,0,0,0.5,1,1,%s,%s,%s,3,%s,0.5,4,0".formatted(
                        fineValue, mediumValue, largeValue, parameters.isRecover() ? 1 : 0, Integer.toString(parameters.isRecover() ? 1 : 0)),
                "-mul", "65536", "-output", outputFile + ",int16");
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new FilterException("G'mic CLI execution failed with exit code "+exitCode);
        }
        // Open file as new image
        ImagePlus outputImage = new Opener().openImage(Util.getIJFileFormat(outputFile));
        ImageStack outputStack = outputImage.getStack();
        ImageStack imageStack = image.getStack();
        // copy layers over to existing image
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
