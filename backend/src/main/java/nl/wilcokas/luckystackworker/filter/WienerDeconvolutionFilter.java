package nl.wilcokas.luckystackworker.filter;

import edu.emory.mathcs.restoretools.Enums;
import edu.emory.mathcs.restoretools.iterative.IterativeEnums;
import edu.emory.mathcs.restoretools.iterative.wpl.WPLFloatIterativeDeconvolver2D;
import edu.emory.mathcs.restoretools.iterative.wpl.WPLOptions;
import ij.ImagePlus;
import ij.LookUpTable;
import ij.process.ShortProcessor;
import nl.wilcokas.luckystackworker.util.LswUtil;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Component
public class WienerDeconvolutionFilter {

    public void apply(ImagePlus image, ImagePlus psf) {
        Executor executor = LswUtil.getParallelExecutor();
        executor.execute(() -> applyToLayer(image,psf,1));
        executor.execute(() -> applyToLayer(image,psf,2));
        executor.execute(() -> applyToLayer(image,psf,3));
        LswUtil.stopAndAwaitParallelExecutor(executor);
    }

    private void applyToLayer(ImagePlus image, ImagePlus psf, int layer) {
        short[] pixels = (short[]) image.getStack().getProcessor(layer).getPixels();
        ShortProcessor ipInput = new ShortProcessor(image.getWidth(), image.getHeight(), pixels, LookUpTable.createGrayscaleColorModel(false));
        WPLFloatIterativeDeconvolver2D deconv = new WPLFloatIterativeDeconvolver2D(new ImagePlus("gray", ipInput), psf, IterativeEnums.BoundaryType.REFLEXIVE, IterativeEnums.ResizingType.AUTO,
                Enums.OutputType.SHORT, 5, false, new WPLOptions());
        ShortProcessor ip = (ShortProcessor) deconv.deconvolve().getProcessor();
        System.arraycopy(ip.getPixels(), 0, image.getStack().getProcessor(layer).getPixels(), 0, ip.getWidth() * ip.getHeight());
    }
}
