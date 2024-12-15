package nl.wilcokas.luckystackworker.filter;

import edu.emory.mathcs.restoretools.Enums;
import edu.emory.mathcs.restoretools.iterative.IterativeEnums;
import edu.emory.mathcs.restoretools.iterative.wpl.WPLFloatIterativeDeconvolver2D;
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

    public void apply(ImagePlus image, ImagePlus psf, int iterations) {
        ImageStack stack = image.getStack();
        for (int channel = 1; channel <= 3; channel++) {
            applyToLayer(stack.getProcessor(channel), psf, iterations);
        }
    }

    private void applyToLayer(ImageProcessor ipInput, ImagePlus psf, int iterations) {
        short[] pixels = (short[]) ipInput.getPixels();
        double averagePixelValueIn = getAveragePixelValue(pixels);
        short[] outPixels = getDeconvolvedPixels(ipInput, psf, iterations);
        double averagePixelValueOut = getAveragePixelValue(outPixels);
        for (int i = 0; i < pixels.length; i++) {
            int newValue = LswImageProcessingUtil.convertToUnsignedInt(outPixels[i]);
            pixels[i] = LswImageProcessingUtil.convertToShort((int) (newValue * (averagePixelValueIn / averagePixelValueOut)));
        }
    }

    private short[] getDeconvolvedPixels(ImageProcessor ipInput, ImagePlus psf, int iterations) {
        WPLOptions options =
                new WPLOptions(0, 1.0, 1.0, false, false, true, 0.01, false, false, false, 0);
        WPLFloatIterativeDeconvolver2D deconv = new WPLFloatIterativeDeconvolver2D(new ImagePlus(null, ipInput), psf, IterativeEnums.BoundaryType.REFLEXIVE, IterativeEnums.ResizingType.AUTO,
                Enums.OutputType.SHORT, iterations, false, options);
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
