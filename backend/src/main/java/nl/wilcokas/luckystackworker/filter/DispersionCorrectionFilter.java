package nl.wilcokas.luckystackworker.filter;

import java.awt.Rectangle;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.util.LswImageProcessingUtil;
import org.springframework.stereotype.Component;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import nl.wilcokas.luckystackworker.model.Profile;

@Slf4j
@Component
public class DispersionCorrectionFilter implements LSWFilter {

    @Override
    public void apply(ImagePlus image, Profile profile, boolean isMono) throws IOException {
        if (profile.isDispersionCorrectionEnabled() && LswImageProcessingUtil.validateRGBStack(image)) {
            log.info("Applying dispersion correction");
            apply(image, profile);
        }
    }

    @Override
    public boolean isSlow() {
        return false;
    }

    public void apply(ImagePlus image, Profile profile) {
        ImageStack stack = image.getStack();
        ImageProcessor ipRed = stack.getProcessor(1);
        ImageProcessor ipBlue = stack.getProcessor(3);
        correctLayer(ipRed, profile.getDispersionCorrectionRedX(), profile.getDispersionCorrectionRedY());
        correctLayer(ipBlue, profile.getDispersionCorrectionBlueX(), profile.getDispersionCorrectionBlueY());
    }

    private void correctLayer(ImageProcessor ip, int dx, int dy) {
        short[] pixels = (short[]) ip.getPixels();
        short[] pixelsNew = new short[pixels.length];
        int width = ip.getWidth();
        int height = ip.getHeight();
        Rectangle roi = ip.getRoi();
        for (int y = roi.y; y < roi.y + roi.height; y++) {
            for (int x = roi.x, p = width * y + x; x < roi.x + roi.width; x++, p++) {
                int xOrg = x - dx;
                int yOrg = y - dy;
                if (xOrg < 0 || yOrg < 0 || xOrg >= width || yOrg >= height) {
                    pixelsNew[p] = 0;
                } else {
                    int pOrg = width * yOrg + xOrg;
                    pixelsNew[p] = pixels[pOrg];
                }
            }
        }
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = pixelsNew[i];
        }
    }
}
