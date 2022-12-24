package nl.wilcokas.luckystackworker.filter;

import java.util.ArrayList;
import java.util.List;

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

    private static final int RADIUS_25_DIVISOR = 2447;

    private static final int SHORT_HALF_SIZE = 32768;
    private static final int MAX_INT_VALUE = 65535;

    public void apply(ImagePlus image, final SavitzkyGolayRadius radius) {

        int[] radiusFactors;
        int[][] radiusOffsets;
        int radiusDivisor;
        switch (radius) {
        case RADIUS_25 -> {
            radiusFactors = RADIUS_25_FACTORS;
            radiusOffsets = RADIUS_25_OFFSETS;
            radiusDivisor = RADIUS_25_DIVISOR;
        }
        default -> {
            radiusFactors = RADIUS_25_FACTORS;
            radiusOffsets = RADIUS_25_OFFSETS;
            radiusDivisor = RADIUS_25_DIVISOR;
        }
        }

        for (int layer = 1; layer <= 3; layer++) {
            ImageProcessor p = image.getStack().getProcessor(layer);
            short[] pixels = (short[]) p.getPixels();
            for (int i = 0; i < pixels.length; i++) {
                int newValueUnsignedInt = getPixelValueUnsignedInt(pixels, i, image.getWidth(), image.getHeight(), radiusFactors, radiusOffsets,
                        radiusDivisor);
                pixels[i] = convertToShort(newValueUnsignedInt > MAX_INT_VALUE ? MAX_INT_VALUE : (newValueUnsignedInt < 0 ? 0 : newValueUnsignedInt));
            }
        }
        image.updateAndDraw();
    }

    private int getPixelValueUnsignedInt(short[] pixels, final int position, final int width, final int height, int[] radiusFactors,
            int[][] radiusOffsets, int radiusDivisor) {
        final int yPosition = position / width;
        final int xPosition = position % height;
        List<Integer> multipliedValues = new ArrayList<>();
        for (int i = 0; i < radiusOffsets.length; i++) {
            int xOffset = radiusOffsets[i][0];
            int yOffset = radiusOffsets[i][1];
            int offsetValueUnsignedInt = getOffsetValueUnsignedInt(pixels, xPosition + xOffset, yPosition + yOffset, width);
            int factor = radiusFactors[12 + (yOffset * 5) + xOffset];
            multipliedValues.add(offsetValueUnsignedInt * factor);
        }
        return multipliedValues.stream().reduce(Integer::sum).orElse(0) / radiusDivisor;
    }

    private int getOffsetValueUnsignedInt(short[] pixels, final int xPosition, final int yPosition, final int width) {
        int position = yPosition * width + xPosition;
        if (position < 0 || position >= pixels.length) {
            return 0;
        }
        return convertToUnsignedInt(pixels[position]);
    }

    private int convertToUnsignedInt(final short value) {
        return SHORT_HALF_SIZE + value;
    }

    private short convertToShort(final int value) {
        return (short) (value - SHORT_HALF_SIZE);
    }
}
