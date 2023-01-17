package nl.wilcokas.luckystackworker.filter;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.filter.settings.SavitzkyGolayRadius;

@Slf4j
@Component
public class SavitzkyGolayFilter {

    private static final int[] RADIUS_25_FACTORS = { //
            //            -4, -23, -29, -23, -4, //
            //            -23, 108, 229, 108, -23, //
            //            -29, 229, 1415, 229, -29, //
            //            -23, 108, 229, 108, -23, //
            //            -4, -23, -29, -23, -4 //
            -4, -22, -29, -22, -4, //
            -22, 114, 226, 114, -22, //
            -29, 226, 2129, 226, -29, //
            -22, 114, 226, 114, -22, //
            -4, -22, -29, -22, -4, //
    };

    private static final int[][] RADIUS_25_OFFSETS = { //
            { -2, -2 }, { -1, -2 }, { 0, -2 }, { 1, -2 }, { 2, -2 }, //
            { -2, -1 }, { -1, -1 }, { 0, -1 }, { 1, -1 }, { 2, -1 }, //
            { -2, 0 }, { -1, 0 }, { 0, 0 }, { 1, 0 }, { 2, 0 }, //
            { -2, 1 }, { -1, 1 }, { 0, 1 }, { 1, 1 }, { 2, 1 }, //
            { -2, 2 }, { -1, 2 }, { 0, 2 }, { 1, 2 }, { 2, 2 }, //
    };

    private static final int[] RADIUS_49_FACTORS = { //
            0, -14, -33, -39, -39, -14, 0, //
            -14, -22, 61, 87, 61, -22, -14, //
            -33, 61, 228, 349, 228, 61, -33, //
            -39, 87, 349, 1763, 349, 87, -39, //
            -33, 61, 228, 349, 228, 61, -33, //
            -14, -22, 61, 87, 61, -22, -14, //
            0, -14, -33, -39, -39, -14, 0, //
    };

    private static final int[][] RADIUS_49_OFFSETS = { //
            { -3, -3 }, { -2, -3 }, { -1, -3 }, { 0, -3 }, { 1, -3 }, { 2, -3 }, { 3, -3 }, //
            { -3, -2 }, { -2, -2 }, { -1, -2 }, { 0, -2 }, { 1, -2 }, { 2, -2 }, { 3, -2 }, //
            { -3, -1 }, { -2, -1 }, { -1, -1 }, { 0, -1 }, { 1, -1 }, { 2, -1 }, { 3, -1 }, //
            { -3, 0 }, { -2, 0 }, { -1, 0 }, { 0, 0 }, { 1, 0 }, { 2, 0 }, { 3, 0 }, //
            { -3, 1 }, { -2, 1 }, { -1, 1 }, { 0, 1 }, { 1, 1 }, { 2, 1 }, { 3, 1 }, //
            { -3, 2 }, { -2, 2 }, { -1, 2 }, { 0, 2 }, { 1, 2 }, { 2, 2 }, { 3, 2 }, //
            { -3, 3 }, { -2, 3 }, { -1, 3 }, { 0, 3 }, { 1, 3 }, { 2, 3 }, { 3, 3 },//
    };

    private static final int[] RADIUS_81_FACTORS = { //
            0, -2, -32, -54, -62, -54, -32, -2, 0, //
            -2, -49, -22, 35, 54, 35, -22, -49, -2, //
            -32, -22, 80, 194, 230, 194, 80, -22, -32, //
            -54, 35, 194, 446, 631, 446, 194, 35, -54, //
            -62, 54, 230, 631, 2981, 631, 230, 54, -62, //
            -54, 35, 194, 446, 631, 446, 194, 35, -54, //
            -32, -22, 80, 194, 230, 194, 80, -22, -32, //
            -2, -49, -22, 35, 54, 35, -22, -49, -2, //
            0, -2, -32, -54, -62, -54, -32, -2, 0 //
    };

    private static final int[][] RADIUS_81_OFFSETS = { //
            { -4, -4 }, { -3, -4 }, { -2, -4 }, { -1, -4 }, { 0, -4 }, { 1, -4 }, { 2, -4 }, { 3, -4 }, { 4, -4 }, //
            { -4, -3 }, { -3, -3 }, { -2, -3 }, { -1, -3 }, { 0, -3 }, { 1, -3 }, { 2, -3 }, { 3, -3 }, { 4, -3 }, //
            { -4, -3 }, { -3, -2 }, { -2, -2 }, { -1, -2 }, { 0, -2 }, { 1, -2 }, { 2, -2 }, { 3, -2 }, { 4, -2 }, //
            { -4, -3 }, { -3, -1 }, { -2, -1 }, { -1, -1 }, { 0, -1 }, { 1, -1 }, { 2, -1 }, { 3, -1 }, { 4, -1 }, //
            { -4, -3 }, { -3, 0 }, { -2, 0 }, { -1, 0 }, { 0, 0 }, { 1, 0 }, { 2, 0 }, { 3, 0 }, { 4, 0 }, //
            { -4, 1 }, { -3, 1 }, { -2, 1 }, { -1, 1 }, { 0, 1 }, { 1, 1 }, { 2, 1 }, { 3, 1 }, { 4, 1 }, //
            { -4, 2 }, { -3, 2 }, { -2, 2 }, { -1, 2 }, { 0, 2 }, { 1, 2 }, { 2, 2 }, { 3, 2 }, { 4, 2 }, //
            { -4, 3 }, { -3, 3 }, { -2, 3 }, { -1, 3 }, { 0, 3 }, { 1, 3 }, { 2, 3 }, { 3, 3 }, { 4, 3 }, //
            { -4, 4 }, { -3, 3 }, { -2, 3 }, { -1, 3 }, { 0, 3 }, { 1, 3 }, { 2, 3 }, { 3, 3 }, { 4, 4 } //
    };

    private static final int RADIUS_25_CENTER = 12;
    private static final int RADIUS_25_DIVISOR = 3181;
    private static final int RADIUS_25_ROWLENGTH = 5;

    private static final int RADIUS_49_CENTER = 24;
    private static final int RADIUS_49_DIVISOR = 4287;
    private static final int RADIUS_49_ROWLENGTH = 7;

    private static final int RADIUS_81_CENTER = 40;
    private static final int RADIUS_81_DIVISOR = 9253;
    private static final int RADIUS_81_ROWLENGTH = 9;

    public void apply(ImagePlus image, final SavitzkyGolayRadius radius, int amount) {

        int[] radiusFactors;
        int[][] radiusOffsets;
        int radiusDivisor;
        int radiusCenter;
        int radiusRowLength;
        switch (radius) {
        case OFF -> {
            return;
        }
        case RADIUS_25 -> {
            radiusFactors = RADIUS_25_FACTORS;
            radiusOffsets = RADIUS_25_OFFSETS;
            radiusDivisor = RADIUS_25_DIVISOR;
            radiusCenter = RADIUS_25_CENTER;
            radiusRowLength = RADIUS_25_ROWLENGTH;
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
        default -> {
            radiusFactors = RADIUS_25_FACTORS;
            radiusOffsets = RADIUS_25_OFFSETS;
            radiusDivisor = RADIUS_25_DIVISOR;
            radiusCenter = RADIUS_25_CENTER;
            radiusRowLength = RADIUS_25_ROWLENGTH;
        }
        }

        ImageStack stack = image.getStack();
        // Run every stack in a seperate thread to increase performance.
        int numThreads = Runtime.getRuntime().availableProcessors();
        Executor executor = Executors.newFixedThreadPool(numThreads);
        for (int layer = 1; layer <= stack.size(); layer++) {
            int finalLayer = layer;
            executor.execute(() -> {
                ImageProcessor p = stack.getProcessor(finalLayer);
                short[] pixels = (short[]) p.getPixels();

                short[] pixelsResult = new short[pixels.length];
                for (int i = 0; i < pixels.length; i++) {
                    long newValueUnsignedInt = getPixelValueUnsignedInt(pixels, i, image.getWidth(), radiusFactors, radiusOffsets, radiusDivisor,
                            radiusCenter, radiusRowLength, amount);
                    pixelsResult[i] = convertToShort(newValueUnsignedInt > Constants.MAX_INT_VALUE ? Constants.MAX_INT_VALUE
                            : (newValueUnsignedInt < 0 ? 0 : newValueUnsignedInt));
                }
                for (int i = 0; i < pixels.length; i++) {
                    pixels[i] = pixelsResult[i];
                }
            });
        }
        ((ExecutorService) executor).shutdown();
        try {
            ((ExecutorService) executor).awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            log.warn("Savitzky-golay thread execution was stopped: ", e);
        }
    }

    private long getPixelValueUnsignedInt(short[] pixels, final int position, final int width, int[] radiusFactors, int[][] radiusOffsets,
            double radiusDivisor, int radiusCenter, int radiusRowLength, double amount) {
        final int yPosition = position / width;
        final int xPosition = position % width;
        double multipliedTotal = 0;
        int pixelValueUnsignedInt = convertToUnsignedInt(pixels[position]);
        for (int i = 0; i < radiusOffsets.length; i++) {
            int offsetValueUnsignedInt = getOffsetValueUnsignedInt(pixels,  xPosition + radiusOffsets[i][0],
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
        return convertToUnsignedInt(pixels[position]);
    }

    private int convertToUnsignedInt(final short value) {
        return value < 0 ? value + Constants.UNSIGNED_INT_SIZE : value;
    }

    private short convertToShort(long value) {
        return (short) (value >= Constants.SHORT_HALF_SIZE ? value - Constants.UNSIGNED_INT_SIZE : value);
    }

}
