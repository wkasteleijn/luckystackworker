package nl.wilcokas.luckystackworker.filter;

import edu.emory.mathcs.restoretools.iterative.IterativeEnums;
import edu.emory.mathcs.restoretools.iterative.wpl.WPLOptions;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.filter.settings.LSWSharpenMode;
import nl.wilcokas.luckystackworker.filter.settings.WienerDeconvolutionParameters;
import nl.wilcokas.luckystackworker.util.LswFileUtil;
import nl.wilcokas.luckystackworker.util.LswImageProcessingUtil;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WienerDeconvolutionFilter {


    public void apply(ImagePlus image, WienerDeconvolutionParameters parameters) {
        ImagePlus psf = LswFileUtil.getWienerDeconvolutionPSF();
        ImagePlus[] psfPerChannel = getPsfPerChannel(psf);
        ImageStack stack = image.getStack();
        if (parameters.getMode() == LSWSharpenMode.RGB) {
            applyToChannel(stack.getProcessor(1), psfPerChannel[0], parameters.getIterationsRed(), parameters.getDeringStrengthRed(), parameters.getDeringRadiusRed(), parameters.getDeringThresholdRed(), parameters.getBlendRawRed());
            applyToChannel(stack.getProcessor(2), psfPerChannel[1], parameters.getIterationsGreen(), parameters.getDeringStrengthGreen(), parameters.getDeringRadiusGreen(), parameters.getDeringThresholdGreen(), parameters.getBlendRawGreen());
            applyToChannel(stack.getProcessor(3), psfPerChannel[2], parameters.getIterationsBlue(), parameters.getDeringStrengthBlue(), parameters.getDeringRadiusBlue(), parameters.getDeringThresholdBlue(), parameters.getBlendRawBlue());
        } else {
            applyLuminance(image, psf, parameters);
        }
    }

    private ImagePlus[] getPsfPerChannel(ImagePlus psf) {
        ImageStack stack = psf.getStack();
        ImagePlus[] psfPerChannel = new ImagePlus[3];
        for (int i = 0; i < 3; i++) {
            psfPerChannel[i] = new ImagePlus(String.format("PSF channel %d", i), stack.getProcessor(i));
        }
        return psfPerChannel;
    }

    private void applyToChannel(ImageProcessor ipInput, ImagePlus psf, int iterations, float deringStrength, double deringRadius, int deringThreshold, float blendRawFactor) {
        short[] pixels = (short[]) ipInput.getPixels();
        double averagePixelValueIn = getAveragePixelValue(pixels);
        short[] outPixels = getDeconvolvedPixels(ipInput, psf, iterations);
        double averagePixelValueOut = getAveragePixelValue(outPixels);
        final ImageProcessor ipMask = LswImageProcessingUtil.createDeringMaskProcessor(deringStrength, deringRadius, deringThreshold, ipInput);
        short[] maskPixels = (short[]) ipMask.getPixels();
        for (int i = 0; i < pixels.length; i++) {
            float appliedFactor = Math.max(LswImageProcessingUtil.convertToUnsignedInt(maskPixels[i]) / 65535f, 1f - blendRawFactor);
            // correction on output is needed given that is somehow always brighter than the original value
            int newValue = (int) (LswImageProcessingUtil.convertToUnsignedInt(outPixels[i]) * (averagePixelValueIn / averagePixelValueOut));
            int originalValue = LswImageProcessingUtil.convertToUnsignedInt(pixels[i]);

            int assignedValue = (int) (newValue * appliedFactor) + (int) (originalValue * (1 - appliedFactor));
            pixels[i] = LswImageProcessingUtil.convertToShort(assignedValue);
        }
    }

    private void applyLuminance(ImagePlus image, ImagePlus psf, WienerDeconvolutionParameters parameters) {
        ImageStack stack = image.getStack();
        ImageProcessor ipRed = stack.getProcessor(1);
        ImageProcessor ipGreen = stack.getProcessor(2);
        ImageProcessor ipBlue = stack.getProcessor(3);

        // Obtain the luminance channel
        FloatProcessor fpRed = ipRed.toFloat(1, null);
        FloatProcessor fpGreen = ipGreen.toFloat(2, null);
        FloatProcessor fpBlue = ipBlue.toFloat(3, null);
        float[] pixelsRed = (float[]) fpRed.getPixels();
        float[] pixelsGreen = (float[]) fpGreen.getPixels();
        float[] pixelsBlue = (float[]) fpBlue.getPixels();
        float[] pixelsHue = new float[pixelsRed.length];
        float[] pixelsSat = new float[pixelsRed.length];
        float[] pixelsLum = new float[pixelsRed.length];
        for (int i = 0; i < pixelsRed.length; i++) {
            float[] hsl = LswImageProcessingUtil.rgbToHsl(pixelsRed[i],
                    pixelsGreen[i],
                    pixelsBlue[i],
                    parameters.isIncludeRed(),
                    parameters.isIncludeGreen(),
                    parameters.isIncludeBlue(),
                    parameters.isIncludeColor(),
                    parameters.getMode());
            pixelsHue[i] = hsl[0];
            pixelsSat[i] = hsl[1];
            pixelsLum[i] = hsl[2];
        }
        FloatProcessor fpLum = new FloatProcessor(image.getWidth(), image.getHeight(), pixelsLum);

        // Perform Wiener deconvolution
        ShortProcessor ipInput = new ShortProcessor(image.getWidth(), image.getHeight());
        ipInput.setPixels(1, fpLum);
        short[] pixels = (short[]) ipInput.getPixels();
        double averagePixelValueIn = getAveragePixelValue(pixels);
        short[] outPixels = getDeconvolvedPixels(fpLum, psf, parameters.getIterations());
        double averagePixelValueOut = getAveragePixelValue(outPixels);
        final ImageProcessor ipMask = LswImageProcessingUtil.createDeringMaskFloatProcessor(parameters.getDeringRadius(), parameters.getDeringThreshold(), fpLum);
        short[] maskPixels = (short[]) ipMask.getPixels();
        for (int i = 0; i < pixels.length; i++) {
            float appliedFactor = Math.max(LswImageProcessingUtil.convertToUnsignedInt(maskPixels[i]) / 65535f, 1f - parameters.getBlendRaw());
            // correction on output is needed given that is somehow always brighter than the original value
            int newValue = (int) (LswImageProcessingUtil.convertToUnsignedInt(outPixels[i]) * (averagePixelValueIn / averagePixelValueOut));
            int originalValue = LswImageProcessingUtil.convertToUnsignedInt(pixels[i]);

            int assignedValue = (int) (newValue * appliedFactor) + (int) (originalValue * (1 - appliedFactor));
            pixels[i] = LswImageProcessingUtil.convertToShort(assignedValue);
        }

        // Convert back to 16-bit RGB and update the image
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

    private short[] getDeconvolvedPixels(ImageProcessor ipInput, ImagePlus psf, int iterations) {
        WPLOptions options =
                new WPLOptions(0, 1.0, 1.0, false, false, true, 0.01, false, false, false, 0);
        LswWPLFloatIterativeDeconvolver2D deconv = new LswWPLFloatIterativeDeconvolver2D(new ImagePlus(null, ipInput), psf, IterativeEnums.BoundaryType.REFLEXIVE, IterativeEnums.ResizingType.AUTO, iterations, options);
        ShortProcessor ipOutput = (ShortProcessor) deconv.deconvolve().getProcessor();
        return (short[]) ipOutput.getPixels();
    }

    private double getAveragePixelValue(short[] pixels) {
        double sum = 0;
        for (short pixel : pixels) {
            sum += LswImageProcessingUtil.convertToUnsignedInt(pixel);
        }
        return sum / pixels.length;
    }
}
