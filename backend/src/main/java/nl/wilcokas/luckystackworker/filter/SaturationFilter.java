package nl.wilcokas.luckystackworker.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import nl.wilcokas.luckystackworker.filter.settings.LSWSharpenMode;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.util.LswImageProcessingUtil;

import java.io.IOException;

@Slf4j
@Component
public class SaturationFilter implements LSWFilter {

    @Override
    public boolean apply(ImagePlus image, Profile profile, boolean isMono) throws IOException {
        if (isApplied(profile,image)) {
            log.info("Applying saturation increase with factor {} to image {}", profile.getSaturation(),
                    image.getID());
            apply(image, profile);
            return true;
        } else {
            log.debug("Attemping to apply saturation increase to a non RGB image {}", image.getFileInfo());
        }
        return false;
    }

    @Override
    public boolean isSlow() {
        return false;
    }

    @Override
    public boolean isApplied(Profile profile, ImagePlus image) {
        return profile.getSaturation() != null && (LswImageProcessingUtil.validateRGBStack(image));
    }

    private void apply(ImagePlus image, Profile profile) {
        ImageStack stack = image.getStack();
        ImageProcessor ipRed = stack.getProcessor(1);
        ImageProcessor ipGreen = stack.getProcessor(2);
        ImageProcessor ipBlue = stack.getProcessor(3);

        FloatProcessor fpRed = ipRed.toFloat(1, null);
        FloatProcessor fpGreen = ipGreen.toFloat(2, null);
        FloatProcessor fpBlue = ipBlue.toFloat(3, null);
        fpRed.snapshot();
        fpGreen.snapshot();
        fpBlue.snapshot();
        float[] pixelsRed = (float[]) fpRed.getPixels();
        float[] pixelsGreen = (float[]) fpGreen.getPixels();
        float[] pixelsBlue = (float[]) fpBlue.getPixels();

        float[] pixelsHue = new float[pixelsRed.length];
        float[] pixelsSat = new float[pixelsRed.length];
        float[] pixelsLum = new float[pixelsRed.length];
        for (int i = 0; i < pixelsRed.length; i++) {
            LSWSharpenMode mode = LSWSharpenMode.valueOf(profile.getSharpenMode());
            float[] hsl = LswImageProcessingUtil.rgbToHsl(pixelsRed[i], pixelsGreen[i], pixelsBlue[i], true, true, true,
                    mode == LSWSharpenMode.RGB || profile.isLuminanceIncludeColor(), mode);
            pixelsHue[i] = hsl[0];
            pixelsSat[i] = hsl[1] * profile.getSaturation().floatValue();
            pixelsLum[i] = hsl[2];
        }

        for (int i = 0; i < pixelsRed.length; i++) {
            float[] rgb = LswImageProcessingUtil.hslToRgb(pixelsHue[i], pixelsSat[i], pixelsLum[i], 0f);
            pixelsRed[i] = rgb[0];
            pixelsGreen[i] = rgb[1];
            pixelsBlue[i] = rgb[2];
        }

        ipRed.setPixels(1, fpRed);
        ipGreen.setPixels(2, fpGreen);
        ipBlue.setPixels(3, fpBlue);

    }
}
