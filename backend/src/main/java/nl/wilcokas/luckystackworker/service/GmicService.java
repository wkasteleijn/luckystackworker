package nl.wilcokas.luckystackworker.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import ij.ImagePlus;
import ij.ImageStack;
import ij.io.Opener;
import ij.process.ImageProcessor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.exceptions.FilterException;
import nl.wilcokas.luckystackworker.util.Util;

@Slf4j
@Service
public class GmicService {
    public void callGmicCli(ImagePlus image, final String profileName, final List<String> commands) throws IOException, InterruptedException {
        String workFolder = Util.getIJFileFormat(System.getProperty("user.home")) + "/AppData/Local/LuckyStackWorker";
        String inputFile = workFolder + "/temp_in.tif";
        Util.saveImage(image, profileName, inputFile, true, false, false, false);
        String outputFile = workFolder + "/temp_out.tif";
        List<String> arguments = new ArrayList<>(Arrays.asList("./gmic/gmic.exe", "v", "2", "-input", inputFile, "-div", "65536"));
        arguments.addAll(commands);
        arguments.addAll(Arrays.asList("-mul", "65536", "-output", outputFile + ",int16"));
        ProcessBuilder processBuilder = new ProcessBuilder(arguments);
        processBuilder.redirectErrorStream(true);
        if (logOutput(processBuilder.start()) != 0) {
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

    private int logOutput(final Process process) throws IOException, InterruptedException {
        log.info("=== G'mic CLI output start ===");
        log.info(IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8.name()));
        log.info("==== G'mic CLI output end ====");
        return process.waitFor();
    }
}
