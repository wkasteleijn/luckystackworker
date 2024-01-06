package nl.wilcokas.luckystackworker.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import ij.ImagePlus;
import ij.ImageStack;
import ij.io.Opener;
import ij.process.ImageProcessor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.util.Util;

@Slf4j
@Service
public class GmicService {
    public void callGmicCli(ImagePlus image, final String profileName, final List<String> commands) {
        try {
            String activeOSProfile = Util.getActiveOSProfile();
            String workFolder = Util.getDataFolder(activeOSProfile);
            String inputFile = workFolder + "/temp_in.tif";
            Util.saveImage(image, profileName, inputFile, true, false, false, false);
            String outputFile = workFolder + "/temp_out.tif";
            List<String> arguments = new ArrayList<>(Arrays.asList("v", "2", "-input", inputFile, "-div", "65536"));
            arguments.addAll(0, getGmicCommand(activeOSProfile));
            arguments.addAll(commands);
            arguments.addAll(Arrays.asList("-mul", "65536", "-output", outputFile + ",int16"));
            Util.runCliCommand(activeOSProfile, arguments);
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
        } catch (Exception e) {
            log.error("Error calling G'MIC CLI :" + e.getMessage());
        }
    }

    public boolean isGmicAvailable(String activeOSProfile) {
        try {
            Util.runCliCommand(activeOSProfile, getGmicCommand(activeOSProfile));
            log.info("G'MIC is available");
            return true;
        } catch (Exception e) {
            log.error("G'MIC is unavailable, reason: " + e.getMessage());
            return false;
        }
    }

    private List<String> getGmicCommand(String activeOSProfile) {
        if (Constants.SYSTEM_PROFILE_WINDOWS.equals(activeOSProfile)) {
            return Collections.singletonList("./gmic/gmic.exe");
        } else {
            return Arrays.asList("export", "PATH=/usr/local/bin:$PATH", "&&", "gmic");
        }
    }

}
