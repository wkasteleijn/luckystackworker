package nl.wilcokas.luckystackworker.filter;

import edu.emory.mathcs.restoretools.iterative.IterativeEnums;
import edu.emory.mathcs.restoretools.iterative.wpl.WPLOptions;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.util.LswImageProcessingUtil;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WienerDeconvolutionFilter {

    public void apply(ImagePlus image, ImagePlus psf, int iterations, float deringStrength, double deringRadius, int deringThreshold, float blendRawFactor) {
        ImageStack stack = image.getStack();
        for (int channel = 1; channel <= 3; channel++) {
            applyToLayer(stack.getProcessor(channel), psf, iterations, deringStrength, deringRadius, deringThreshold, blendRawFactor);
        }
    }

    private void applyToLayer(ImageProcessor ipInput, ImagePlus psf, int iterations, float deringStrength, double deringRadius, int deringThreshold, float blendRawFactor) {
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
