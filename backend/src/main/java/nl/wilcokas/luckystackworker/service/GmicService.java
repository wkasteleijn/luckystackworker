package nl.wilcokas.luckystackworker.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;

import nl.wilcokas.luckystackworker.service.dto.OpenImageModeEnum;
import org.springframework.stereotype.Service;

import ij.ImagePlus;
import ij.ImageStack;
import ij.io.Opener;
import ij.process.ImageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.util.LswFileUtil;
import nl.wilcokas.luckystackworker.util.LswUtil;

@Slf4j
@RequiredArgsConstructor
@Service
public class GmicService {

    private Boolean gmicAvailable;

    public void callGmicCli(ImagePlus image, final String profileName, final double scale, final List<String> commands) {
        try {
            String activeOSProfile = LswUtil.getActiveOSProfile();
            if (!isGmicAvailable(activeOSProfile)) {
                log.warn("Attempt to call G'MIC while it in't available");
                return;
            }
            String workFolder = LswFileUtil.getDataFolder(activeOSProfile);
            String inputFile = workFolder + "/temp_in.tif";
            LswFileUtil.saveImage(image, profileName, inputFile, image.getStack().size() > 1, false, false, false);
            String outputFile = workFolder + "/temp_out.tif";
            List<String> arguments = new ArrayList<>(Arrays.asList("v", "2", "-input", inputFile, "-div", "65536"));
            arguments.addAll(0, getGmicCommand(activeOSProfile));
            arguments.addAll(commands);
            arguments.addAll(Arrays.asList("-mul", "65536"));
            if (image.getStack().size() == 1) {
                // Convert mono to RGB in G'MIC, else the resulting output is messed up.
                arguments.add("-to_rgb");
            }
            arguments.addAll(Arrays.asList("-output", outputFile + ",int16"));
            LswUtil.runCliCommand(activeOSProfile, arguments, true);
            ImagePlus outputImage = LswFileUtil.openImage(LswFileUtil.getIJFileFormat(outputFile), OpenImageModeEnum.RGB, scale, img -> img).getLeft();
            ImageStack outputStack = outputImage.getStack();
            ImageStack imageStack = image.getStack();
            for (int layer = 1; layer <= imageStack.getSize(); layer++) {
                ImageProcessor newProcessor = outputStack.getProcessor(layer);
                short[] pixelsNew = (short[]) newProcessor.getPixels();
                short[] pixels = (short[]) imageStack.getProcessor(layer).getPixels();
                for (int i = 0; i < pixels.length; i++) {
                    pixels[i] = pixelsNew[i];
                }
            }
        } catch (Exception e) {
            log.error("Error calling G'MIC CLI :", e);
        }
    }

    public boolean isGmicAvailable(String activeOSProfile) {
        if (gmicAvailable == null) {
            try {
                LswUtil.runCliCommand(activeOSProfile, getGmicCommand(activeOSProfile), true);
                log.warn("G'MIC is available");
                gmicAvailable = true;
            } catch (Exception e) {
                log.error("G'MIC is unavailable, reason: " + e.getMessage());
                gmicAvailable = false;
            }
        }
        return gmicAvailable;
    }

    private List<String> getGmicCommand(String activeOSProfile) {
        if (Constants.SYSTEM_PROFILE_WINDOWS.equals(activeOSProfile)) {
            return Collections.singletonList("./gmic/gmic.exe");
        } else if (Constants.SYSTEM_PROFILE_MAC.equals(activeOSProfile)) {
            return Arrays.asList("export", "PATH=/usr/local/bin:$PATH", "&&", "gmic");
        } else {
            return Arrays.asList("export", "PATH=/usr/bin:$PATH", "&&", "gmic");
        }
    }

}
