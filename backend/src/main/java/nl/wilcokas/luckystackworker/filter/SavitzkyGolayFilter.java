package nl.wilcokas.luckystackworker.filter;

import org.springframework.stereotype.Component;

import ij.ImagePlus;
import ij.process.ImageProcessor;

@Component
public class SavitzkyGolayFilter {

    private static final int[] RADIUS_25_FACTORS = { //
            -4, -23, -29, -23, -4, //
            -23, 108, 229, 108, -23, //
            -29, 229, 1415, 229, -29, //
            -23, 108, 229, 108, -23, //
            -4, -23, -29, -23, -4 //
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
    private static final int RADIUS_25_DIVISOR = 2447;
    private static final int RADIUS_25_ROWLENGTH = 5;

    private static final int RADIUS_49_CENTER = 24;
    private static final int RADIUS_49_DIVISOR = 4287;
    private static final int RADIUS_49_ROWLENGTH = 7;

    private static final int RADIUS_81_CENTER = 40;
    private static final int RADIUS_81_DIVISOR = 9253;
    private static final int RADIUS_81_ROWLENGTH = 9;

    private static final int SHORT_HALF_SIZE = 32768;
    private static final int UNSIGNED_INT_SIZE = 65536;
    private static final int MAX_INT_VALUE = 65535;

    public void apply(ImagePlus image, final SavitzkyGolayRadius radius) {

        int[] radiusFactors;
        int[][] radiusOffsets;
        int radiusDivisor;
        int radiusCenter;
        int radiusRowLength;
        switch (radius) {
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

        for (int layer = 1; layer <= 3; layer++) {
            ImageProcessor p = image.getStack().getProcessor(layer);
            short[] pixels = (short[]) p.getPixels();
            short[] pixelsResult = new short[pixels.length];
            for (int i = 0; i < pixels.length; i++) {
                int newValueUnsignedInt = getPixelValueUnsignedInt(pixels, i, image.getWidth(), radiusFactors, radiusOffsets,
                        radiusDivisor, radiusCenter, radiusRowLength);
                pixelsResult[i] = convertToShort(
                        newValueUnsignedInt > MAX_INT_VALUE ? MAX_INT_VALUE : (newValueUnsignedInt < 0 ? 0 : newValueUnsignedInt));
            }
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = pixelsResult[i];
            }
        }
        image.updateAndDraw();
    }

    private int getPixelValueUnsignedInt(short[] pixels, final int position, final int width, int[] radiusFactors,
            int[][] radiusOffsets, int radiusDivisor, int radiusCenter, int radiusRowLength) {
        final int yPosition = position / width;
        final int xPosition = position % width;
        int multipliedTotal = 0;
        for (int i = 0; i < radiusOffsets.length; i++) {
            int xOffset = radiusOffsets[i][0];
            int yOffset = radiusOffsets[i][1];
            int offsetValueUnsignedInt = getOffsetValueUnsignedInt(pixels, xPosition + xOffset, yPosition + yOffset, width);
            int factor = radiusFactors[radiusCenter + (yOffset * radiusRowLength) + xOffset];
            multipliedTotal += offsetValueUnsignedInt * factor;
        }
        return multipliedTotal / radiusDivisor;
    }

    private int getOffsetValueUnsignedInt(short[] pixels, final int xPosition, final int yPosition, final int width) {
        int position = (yPosition * width) + xPosition;
        if (position < 0 || position >= pixels.length) {
            return 0;
        }
        return convertToUnsignedInt(pixels[position]);
    }

    private int convertToUnsignedInt(final short value) {
        return value < 0 ? value + UNSIGNED_INT_SIZE : value;
    }

    private short convertToShort(final int value) {
        return (short) (value >= SHORT_HALF_SIZE ? value - UNSIGNED_INT_SIZE : value);
    }
}
