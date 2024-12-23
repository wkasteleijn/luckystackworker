package nl.wilcokas.luckystackworker.filter;

import ij.ImagePlus;
import ij.ImageStack;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.service.dto.LswImageLayersDto;
import nl.wilcokas.luckystackworker.util.LswImageProcessingUtil;
import nl.wilcokas.luckystackworker.util.LswUtil;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

/**
 * Note: experimental feature. Does not seem to work any better than applying it directly from the LSWSharpenFilter.
 */
@Slf4j
@Component
public class BlendRawFilter {

    public void apply(ImagePlus image, final LswImageLayersDto unprocessedImageLayers, double blendRawRedFactor, double blendRawGreenFactor, double blendRawBlueFactor) {
        ImageStack stack = image.getStack();
        short[] redPixels = (short[]) stack.getProcessor(Constants.RED_LAYER_INDEX).getPixels();
        short[] greenPixels = (short[]) stack.getProcessor(Constants.GREEN_LAYER_INDEX).getPixels();
        short[] bluePixels = (short[]) stack.getProcessor(Constants.BLUE_LAYER_INDEX).getPixels();

        Executor executor = LswUtil.getParallelExecutor();
        executor.execute(() -> {
            for (int i = 0; i < redPixels.length; i++) {
                double value = LswImageProcessingUtil.convertToUnsignedInt(redPixels[i]);
                double rawValue = LswImageProcessingUtil.convertToUnsignedInt(unprocessedImageLayers.getLayers()[1][i]);
                redPixels[i] = LswImageProcessingUtil.convertToShort((long) (value * (1 - blendRawRedFactor) + (rawValue * blendRawRedFactor)));
            }
        });
        executor.execute(() -> {
            for (int i = 0; i < greenPixels.length; i++) {
                double value = LswImageProcessingUtil.convertToUnsignedInt(greenPixels[i]);
                double rawValue = LswImageProcessingUtil.convertToUnsignedInt(unprocessedImageLayers.getLayers()[2][i]);
                greenPixels[i] = LswImageProcessingUtil.convertToShort((long) (value * (1 - blendRawGreenFactor) + (rawValue * blendRawGreenFactor)));
            }
        });
        executor.execute(() -> {
            for (int i = 0; i < bluePixels.length; i++) {
                double value = LswImageProcessingUtil.convertToUnsignedInt(bluePixels[i]);
                double rawValue = LswImageProcessingUtil.convertToUnsignedInt(unprocessedImageLayers.getLayers()[2][i]);
                bluePixels[i] = LswImageProcessingUtil.convertToShort((long) (value * (1 - blendRawBlueFactor) + (rawValue * blendRawBlueFactor)));
            }
        });
        LswUtil.stopAndAwaitParallelExecutor(executor);
    }

}
