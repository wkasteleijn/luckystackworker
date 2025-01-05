package nl.wilcokas.luckystackworker.filter;

import java.io.IOException;
import java.util.concurrent.Executor;

import nl.wilcokas.luckystackworker.model.Profile;
import org.springframework.stereotype.Component;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.filter.settings.SavitzkyGolayRadius;
import nl.wilcokas.luckystackworker.util.LswImageProcessingUtil;
import nl.wilcokas.luckystackworker.util.LswUtil;

@Slf4j
@Component
public class SavitzkyGolayFilter implements LSWFilter {

    private static final int[] RADIUS_25_FACTORS = { //
            -4, -22, -29, -22, -4, //
            -22, 114, 226, 114, -22, //
            -29, 226, 2129, 226, -29, //
            -22, 114, 226, 114, -22, //
            -4, -22, -29, -22, -4, //
    };

    private static final int[][] RADIUS_25_OFFSETS = { //
            {-2, -2}, {-1, -2}, {0, -2}, {1, -2}, {2, -2}, //
            {-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {2, -1}, //
            {-2, 0}, {-1, 0}, {0, 0}, {1, 0}, {2, 0}, //
            {-2, 1}, {-1, 1}, {0, 1}, {1, 1}, {2, 1}, //
            {-2, 2}, {-1, 2}, {0, 2}, {1, 2}, {2, 2}, //
    };

    private static final int[] RADIUS_49_FACTORS = { //
            0, -14, -33, -39, -33, -14, 0, //
            -14, -22, 59, 86, 59, -22, -14, //
            -33, 59, 233, 346, 233, 59, -33, //
            -39, 86, 346, 2669, 346, 86, -39, //
            -33, 59, 233, 346, 233, 59, -33, //
            -14, -22, 59, 86, 59, -22, -14, //
            0, -14, -33, -39, -33, -14, 0, //
    };

    private static final int[][] RADIUS_49_OFFSETS = { //
            {-3, -3}, {-2, -3}, {-1, -3}, {0, -3}, {1, -3}, {2, -3}, {3, -3}, //
            {-3, -2}, {-2, -2}, {-1, -2}, {0, -2}, {1, -2}, {2, -2}, {3, -2}, //
            {-3, -1}, {-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {2, -1}, {3, -1}, //
            {-3, 0}, {-2, 0}, {-1, 0}, {0, 0}, {1, 0}, {2, 0}, {3, 0}, //
            {-3, 1}, {-2, 1}, {-1, 1}, {0, 1}, {1, 1}, {2, 1}, {3, 1}, //
            {-3, 2}, {-2, 2}, {-1, 2}, {0, 2}, {1, 2}, {2, 2}, {3, 2}, //
            {-3, 3}, {-2, 3}, {-1, 3}, {0, 3}, {1, 3}, {2, 3}, {3, 3},//
    };

    private static final int[] RADIUS_81_FACTORS = { //
            0, -5, -63, -106, -120, -106, -63, -5, 0, //
            -5, -96, -41, 71, 105, 71, -41, -96, -5, //
            -63, -41, 154, 371, 444, 371, 154, -41, -63, //
            -106, 71, 371, 884, 1223, 884, 371, 71, -106, //
            -120, 105, 444, 1223, 8818, 1223, 444, 105, -120, //
            -106, 71, 371, 884, 1223, 884, 371, 71, -106, //
            -63, -41, 154, 371, 444, 371, 154, -41, -63, //
            -5, -96, -41, 71, 105, 71, -41, -96, -5, //
            0, -5, -63, -106, -120, -106, -63, -5, 0

            //            0, -2, -32, -54, -62, -54, -32, -2, 0, //
            //            -2, -49, -22, 35, 54, 35, -22, -49, -2, //
            //            -32, -22, 80, 194, 230, 194, 80, -22, -32, //
            //            -54, 35, 194, 446, 631, 446, 194, 35, -54, //
            //            -62, 54, 230, 631, 2981, 631, 230, 54, -62, //
            //            -54, 35, 194, 446, 631, 446, 194, 35, -54, //
            //            -32, -22, 80, 194, 230, 194, 80, -22, -32, //
            //            -2, -49, -22, 35, 54, 35, -22, -49, -2, //
            //            0, -2, -32, -54, -62, -54, -32, -2, 0 //
    };

    private static final int[][] RADIUS_81_OFFSETS = { //
            {-4, -4}, {-3, -4}, {-2, -4}, {-1, -4}, {0, -4}, {1, -4}, {2, -4}, {3, -4}, {4, -4}, //
            {-4, -3}, {-3, -3}, {-2, -3}, {-1, -3}, {0, -3}, {1, -3}, {2, -3}, {3, -3}, {4, -3}, //
            {-4, -2}, {-3, -2}, {-2, -2}, {-1, -2}, {0, -2}, {1, -2}, {2, -2}, {3, -2}, {4, -2}, //
            {-4, -1}, {-3, -1}, {-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {2, -1}, {3, -1}, {4, -1}, //
            {-4, 0}, {-3, 0}, {-2, 0}, {-1, 0}, {0, 0}, {1, 0}, {2, 0}, {3, 0}, {4, 0}, //
            {-4, 1}, {-3, 1}, {-2, 1}, {-1, 1}, {0, 1}, {1, 1}, {2, 1}, {3, 1}, {4, 1}, //
            {-4, 2}, {-3, 2}, {-2, 2}, {-1, 2}, {0, 2}, {1, 2}, {2, 2}, {3, 2}, {4, 2}, //
            {-4, 3}, {-3, 3}, {-2, 3}, {-1, 3}, {0, 3}, {1, 3}, {2, 3}, {3, 3}, {4, 3}, //
            {-4, 4}, {-3, 4}, {-2, 4}, {-1, 4}, {0, 4}, {1, 4}, {2, 4}, {3, 4}, {4, 4} //
    };

    private static final int[] RADIUS_121_FACTORS = { //
            0, 0, -18, -81, -119, -132, -119, -81, -18, 0, 0, //
            0, -39, -120, -42, 20, 40, 20, -42, -120, -39, 0, //
            -18, -120, 5, 122, 234, 267, 234, 122, 5, -120, -18, //
            -81, -42, 122, 320, 551, 629, 551, 320, 122, -42, -81, //
            -119, 20, 234, 551, 1133, 1522, 1133, 551, 234, 20, -119, //
            -132, 40, 267, 629, 1522, 10633, 1522, 629, 267, 40, -132, //
            -119, 20, 234, 551, 1133, 1522, 1133, 551, 234, 20, -119, //
            -81, -42, 122, 320, 551, 629, 551, 320, 122, -42, -81, //
            -18, -120, 5, 122, 234, 267, 234, 122, 5, -120, -18, //
            0, -39, -120, -42, 20, 40, 20, -42, -120, -39, 0, //
            0, 0, -18, -81, -119, -132, -119, -81, -18, 0, 0,//
    };

    private static final int[][] RADIUS_121_OFFSETS = { //
            {-5, -5}, {-4, -5}, {-3, -5}, {-2, -5}, {-1, -5}, {0, -5}, {1, -5}, {2, -5}, {3, -5}, {4, -5}, {5, -5}, //
            {-5, -4}, {-4, -4}, {-3, -4}, {-2, -4}, {-1, -4}, {0, -4}, {1, -4}, {2, -4}, {3, -4}, {4, -4}, {5, -4}, //
            {-5, -3}, {-4, -3}, {-3, -3}, {-2, -3}, {-1, -3}, {0, -3}, {1, -3}, {2, -3}, {3, -3}, {4, -3}, {5, -3}, //
            {-5, -2}, {-4, -2}, {-3, -2}, {-2, -2}, {-1, -2}, {0, -2}, {1, -2}, {2, -2}, {3, -2}, {4, -2}, {5, -2}, //
            {-5, -1}, {-4, -1}, {-3, -1}, {-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {2, -1}, {3, -1}, {4, -1}, {5, -1}, //
            {-5, 0}, {-4, 0}, {-3, 0}, {-2, 0}, {-1, 0}, {0, 0}, {1, 0}, {2, 0}, {3, 0}, {4, 0}, {5, 0}, //
            {-5, 1}, {-4, 1}, {-3, 1}, {-2, 1}, {-1, 1}, {0, 1}, {1, 1}, {2, 1}, {3, 1}, {4, 1}, {5, 1}, //
            {-5, 2}, {-4, 2}, {-3, 2}, {-2, 2}, {-1, 2}, {0, 2}, {1, 2}, {2, 2}, {3, 2}, {4, 2}, {5, 2}, //
            {-5, 3}, {-4, 3}, {-3, 3}, {-2, 3}, {-1, 3}, {0, 3}, {1, 3}, {2, 3}, {3, 3}, {4, 3}, {5, 3}, //
            {-5, 4}, {-4, 4}, {-3, 4}, {-2, 4}, {-1, 4}, {0, 4}, {1, 4}, {2, 4}, {3, 4}, {4, 4}, {5, 4}, //
            {-5, 5}, {-4, 5}, {-3, 5}, {-2, 5}, {-1, 5}, {0, 5}, {1, 5}, {2, 5}, {3, 5}, {4, 5}, {5, 5}, //
    };

    private static final int[] RADIUS_169_FACTORS = { //
            0, 0, 0, -30, -79, -107, -117, -107, -79, -30, 0, 0, 0, //
            0, -2, -71, -101, -45, -12, -1, -12, -45, -101, -71, -2, 0, //
            0, -71, -83, 1, 74, 126, 143, 126, 74, 1, -83, -71, 0, //
            -30, -101, 1, 114, 213, 309, 338, 309, 213, 114, 1, -101, -30, //
            -79, -45, 74, 213, 386, 595, 665, 595, 386, 213, 74, -45, -79, //
            -107, -12, 126, 309, 595, 1141, 1509, 1141, 595, 309, 126, -12, -107, //
            -117, -1, 143, 338, 665, 1509, 10365, 1509, 665, 338, 143, -1, -117, //
            -107, -12, 126, 309, 595, 1141, 1509, 1141, 595, 309, 126, -12, -107, //
            -79, -45, 74, 213, 386, 595, 665, 595, 386, 213, 74, -45, -79, //
            -30, -101, 1, 114, 213, 309, 338, 309, 213, 114, 1, -101, -30, //
            0, -71, -83, 1, 74, 126, 143, 126, 74, 1, -83, -71, 0, //
            0, -2, -71, -101, -45, -12, -1, -12, -45, -101, -71, -2, 0, //
            0, 0, 0, -30, -79, -107, -117, -107, -79, -30, 0, 0, 0

    };

    private static final int[][] RADIUS_169_OFFSETS = { //
            {-6, -6}, {-5, -6}, {-4, -6}, {-3, -6}, {-2, -6}, {-1, -6}, {0, -6}, {1, -6}, {2, -6}, {3, -6}, {4, -6}, {5, -6}, {6, -6},//
            {-6, -5}, {-5, -5}, {-4, -5}, {-3, -5}, {-2, -5}, {-1, -5}, {0, -5}, {1, -5}, {2, -5}, {3, -5}, {4, -5}, {5, -5}, {6, -5},//
            {-6, -4}, {-5, -4}, {-4, -4}, {-3, -4}, {-2, -4}, {-1, -4}, {0, -4}, {1, -4}, {2, -4}, {3, -4}, {4, -4}, {5, -4}, {6, -4},//
            {-6, -3}, {-5, -3}, {-4, -3}, {-3, -3}, {-2, -3}, {-1, -3}, {0, -3}, {1, -3}, {2, -3}, {3, -3}, {4, -3}, {5, -3}, {6, -3},//
            {-6, -2}, {-5, -2}, {-4, -2}, {-3, -2}, {-2, -2}, {-1, -2}, {0, -2}, {1, -2}, {2, -2}, {3, -2}, {4, -2}, {5, -2}, {6, -2},//
            {-6, -1}, {-5, -1}, {-4, -1}, {-3, -1}, {-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {2, -1}, {3, -1}, {4, -1}, {5, -1}, {6, -1},//
            {-6, 0}, {-5, 0}, {-4, 0}, {-3, 0}, {-2, 0}, {-1, 0}, {0, 0}, {1, 0}, {2, 0}, {3, 0}, {4, 0}, {5, 0}, {6, 0},//
            {-6, 1}, {-5, 1}, {-4, 1}, {-3, 1}, {-2, 1}, {-1, 1}, {0, 1}, {1, 1}, {2, 1}, {3, 1}, {4, 1}, {5, 1}, {6, 1},//
            {-6, 2}, {-5, 2}, {-4, 2}, {-3, 2}, {-2, 2}, {-1, 2}, {0, 2}, {1, 2}, {2, 2}, {3, 2}, {4, 2}, {5, 2}, {6, 2},//
            {-6, 3}, {-5, 3}, {-4, 3}, {-3, 3}, {-2, 3}, {-1, 3}, {0, 3}, {1, 3}, {2, 3}, {3, 3}, {4, 3}, {5, 3}, {6, 3},//
            {-6, 4}, {-5, 4}, {-4, 4}, {-3, 4}, {-2, 4}, {-1, 4}, {0, 4}, {1, 4}, {2, 4}, {3, 4}, {4, 4}, {5, 4}, {6, 4},//
            {-6, 5}, {-5, 5}, {-4, 5}, {-3, 5}, {-2, 5}, {-1, 5}, {0, 5}, {1, 5}, {2, 5}, {3, 5}, {4, 5}, {5, 5}, {6, 5},//
            {-6, 6}, {-5, 6}, {-4, 6}, {-3, 6}, {-2, 6}, {-1, 6}, {0, 6}, {1, 6}, {2, 6}, {3, 6}, {4, 6}, {5, 6}, {6, 6}//
    };
    private static final int RADIUS_25_CENTER = 12;
    private static final int RADIUS_25_DIVISOR = 3181;
    private static final int RADIUS_25_ROWLENGTH = 5;

    private static final int RADIUS_49_CENTER = 24;
    private static final int RADIUS_49_DIVISOR = 5181;
    private static final int RADIUS_49_ROWLENGTH = 7;

    private static final int RADIUS_81_CENTER = 40;
    private static final int RADIUS_81_DIVISOR = 21010;
    private static final int RADIUS_81_ROWLENGTH = 9;

    private static final int RADIUS_121_CENTER = 60;
    private static final int RADIUS_121_DIVISOR = 29989;
    private static final int RADIUS_121_ROWLENGTH = 11;

    private static final int RADIUS_169_CENTER = 84;
    private static final int RADIUS_169_DIVISOR = 33721;
    private static final int RADIUS_169_ROWLENGTH = 13;

    @Override
    public void apply(ImagePlus image, Profile profile, boolean isMono) throws IOException {
        if (Constants.DENOISE_ALGORITHM_SAVGOLAY.equals(profile.getDenoiseAlgorithm2())) {
            log.info("Starting SavitzkyGolayDenoise filter");
            apply(image, profile);
        }
    }

    private void apply(ImagePlus image, Profile profile) {
        ImageStack stack = image.getStack();
        // Run every stack in a seperate thread to increase performance.
        Executor executor = LswUtil.getParallelExecutor();
        // Red
        executor.execute(() -> {
            ImageProcessor layerProcessor = stack.getProcessor(1);
            int iterations = profile.getSavitzkyGolayIterations() == 0 ? 1 : profile.getSavitzkyGolayIterations();
            SavitzkyGolayParameters parameters = getSavitzkyGolayParameters(profile.getSavitzkyGolaySize(),profile.getSavitzkyGolayAmount(), iterations);
            if (parameters.radiusFactors != null) {
                applySavitzkyGolayToLayer(image, layerProcessor, parameters);
            }
        });
        // Green
        executor.execute(() -> {
            ImageProcessor layerProcessor = stack.getProcessor(2);
            int iterations = profile.getSavitzkyGolayIterationsGreen() == 0 ? 1 : profile.getSavitzkyGolayIterationsGreen();
            SavitzkyGolayParameters parameters = getSavitzkyGolayParameters(profile.getSavitzkyGolaySizeGreen(),profile.getSavitzkyGolayAmountGreen(), iterations);
            if (parameters.radiusFactors != null) {
                applySavitzkyGolayToLayer(image, layerProcessor, parameters);
            }
        });
        // Blue
        executor.execute(() -> {
            ImageProcessor layerProcessor = stack.getProcessor(3);
            int iterations = profile.getSavitzkyGolayIterationsBlue() == 0 ? 1 : profile.getSavitzkyGolayIterationsBlue();
            SavitzkyGolayParameters parameters = getSavitzkyGolayParameters(profile.getSavitzkyGolaySizeBlue(),profile.getSavitzkyGolayAmountBlue(), iterations);
            if (parameters.radiusFactors != null) {
                applySavitzkyGolayToLayer(image, layerProcessor, parameters);
            }
        });
        LswUtil.stopAndAwaitParallelExecutor(executor);
    }

    private SavitzkyGolayParameters getSavitzkyGolayParameters(int size, int amount, int iterations) {
        SavitzkyGolayRadius radius = SavitzkyGolayRadius.valueOf(size);
        int[] radiusFactors = null;
        int[][] radiusOffsets = null;
        int radiusDivisor = -1;
        int radiusCenter = -1;
        int radiusRowLength = -1;
        switch (radius) {
            case OFF -> {
                // No filter should be applied
            }
            case RADIUS_49 -> {
                radiusFactors = RADIUS_49_FACTORS;
                radiusOffsets = RADIUS_49_OFFSETS;
                radiusDivisor = RADIUS_49_DIVISOR;
                radiusCenter = RADIUS_49_CENTER;
                radiusRowLength = RADIUS_49_ROWLENGTH;
            }
            case RADIUS_81 -> {
                radiusFactors = RADIUS_81_FACTORS;
                radiusOffsets = RADIUS_81_OFFSETS;
                radiusDivisor = RADIUS_81_DIVISOR;
                radiusCenter = RADIUS_81_CENTER;
                radiusRowLength = RADIUS_81_ROWLENGTH;
            }
            case RADIUS_121 -> {
                radiusFactors = RADIUS_121_FACTORS;
                radiusOffsets = RADIUS_121_OFFSETS;
                radiusDivisor = RADIUS_121_DIVISOR;
                radiusCenter = RADIUS_121_CENTER;
                radiusRowLength = RADIUS_121_ROWLENGTH;
            }
            case RADIUS_169 -> {
                radiusFactors = RADIUS_169_FACTORS;
                radiusOffsets = RADIUS_169_OFFSETS;
                radiusDivisor = RADIUS_169_DIVISOR;
                radiusCenter = RADIUS_169_CENTER;
                radiusRowLength = RADIUS_169_ROWLENGTH;
            }
            default -> { // Also RADIUS_25
                radiusFactors = RADIUS_25_FACTORS;
                radiusOffsets = RADIUS_25_OFFSETS;
                radiusDivisor = RADIUS_25_DIVISOR;
                radiusCenter = RADIUS_25_CENTER;
                radiusRowLength = RADIUS_25_ROWLENGTH;
            }
        }
        return new SavitzkyGolayParameters(iterations, amount, radiusFactors, radiusOffsets, radiusDivisor, radiusCenter, radiusRowLength);
    }

    private void applySavitzkyGolayToLayer(ImagePlus image, ImageProcessor p, SavitzkyGolayParameters parameters) {
        for (int it = 0; it < parameters.iterations; it++) {
            short[] pixels = (short[]) p.getPixels();
            short[] pixelsResult = new short[pixels.length];
            for (int i = 0; i < pixels.length; i++) {
                long newValueUnsignedInt = getPixelValueUnsignedInt(pixels, i, image.getWidth(), parameters.radiusFactors, parameters.radiusOffsets, parameters.radiusDivisor,
                        parameters.radiusCenter, parameters.radiusRowLength, parameters.amount);
                pixelsResult[i] = LswImageProcessingUtil.convertToShort(newValueUnsignedInt > Constants.MAX_INT_VALUE ? Constants.MAX_INT_VALUE
                        : (newValueUnsignedInt < 0 ? 0 : newValueUnsignedInt));
            }
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = pixelsResult[i];
            }
        }
    }

    private long getPixelValueUnsignedInt(short[] pixels, final int position, final int width, int[] radiusFactors, int[][] radiusOffsets,
                                          double radiusDivisor, int radiusCenter, int radiusRowLength, double amount) {
        final int yPosition = position / width;
        final int xPosition = position % width;
        double multipliedTotal = 0;
        int pixelValueUnsignedInt = LswImageProcessingUtil.convertToUnsignedInt(pixels[position]);
        for (int i = 0; i < radiusOffsets.length; i++) {
            int offsetValueUnsignedInt = getOffsetValueUnsignedInt(pixels, xPosition + radiusOffsets[i][0],
                    yPosition + radiusOffsets[i][1], width);
            int factor = radiusFactors[radiusCenter + (radiusOffsets[i][1] * radiusRowLength) + radiusOffsets[i][0]];
            multipliedTotal += offsetValueUnsignedInt * factor;
        }
        return Math.round(((amount / 100) * (multipliedTotal / radiusDivisor)) + (((100 - amount) / 100) * pixelValueUnsignedInt));
    }

    private int getOffsetValueUnsignedInt(short[] pixels, final int xPosition, final int yPosition, final int width) {
        int position = (yPosition * width) + xPosition;
        if (position < 0 || position >= pixels.length) {
            return 0;
        }
        return LswImageProcessingUtil.convertToUnsignedInt(pixels[position]);
    }

    private record SavitzkyGolayParameters(int iterations, int amount, int[] radiusFactors, int[][] radiusOffsets, int radiusDivisor, int radiusCenter, int radiusRowLength) {}

}
