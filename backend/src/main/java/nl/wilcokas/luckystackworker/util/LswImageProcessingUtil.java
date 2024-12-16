package nl.wilcokas.luckystackworker.util;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import ij.CompositeImage;
import ij.ImageStack;
import ij.plugin.filter.GaussianBlur;
import nl.wilcokas.luckystackworker.service.dto.LswImageLayersDto;
import nl.wilcokas.luckystackworker.service.dto.OpenImageModeEnum;
import org.apache.commons.lang3.tuple.Pair;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.filter.settings.LSWSharpenMode;
import nl.wilcokas.luckystackworker.model.Profile;

import java.util.Arrays;
import java.util.concurrent.Executor;

import static nl.wilcokas.luckystackworker.constants.Constants.MINIMUK_DARK_TRESHOLD;

@Slf4j
public class LswImageProcessingUtil {

    private LswImageProcessingUtil() {
    }

    public static int getMaxHistogramPercentage(ImagePlus image) {
        Pair<Integer, Integer> maxHistogram = getMaxHistogram(image);
        return (maxHistogram.getLeft() * 100) / maxHistogram.getRight();
    }

    public static Pair<Integer, Integer> getMaxHistogram(ImagePlus image) {
        int[] histogram = image.getProcessor().getHistogram();
        int maxVal = 0;
        for (int i = histogram.length - 1; i >= 0; i--) {
            if (histogram[i] > 0) {
                maxVal = i;
                break;
            }
        }
        return Pair.of(maxVal, histogram.length);
    }

    public static int getMinHistogramPercentage(ImagePlus image) {
        Pair<Integer, Integer> minHistogram = getMinHistogram(image);
        return (minHistogram.getLeft() * 100) / minHistogram.getRight();
    }

    public static Pair<Integer, Integer> getMinHistogram(ImagePlus image) {
        int[] histogram = image.getProcessor().getHistogram();
        int minVal = 0;
        for (int i = 0; i < histogram.length; i++) {
            if (histogram[i] > 0) {
                minVal = i;
                break;
            }
        }
        return Pair.of(minVal, histogram.length);
    }

    public static void copyInto(final ImagePlus origin, final ImagePlus destination, Roi roi, Profile profile,
                                boolean copyMinMax) {
        log.info("Copying image {} into image {}", origin.getID(), destination.getID());
        destination.setImage(origin);
        destination.setTitle("PROCESSING");
        if (copyMinMax && (profile.getBrightness() > 0 || profile.getContrast() > 0) || profile.getBackground() > 0) {
            copyMinMax(origin, destination);
        }

        if (roi != null) {
            destination.setRoi((int) roi.getXBase(), (int) roi.getYBase(), (int) roi.getFloatWidth(),
                    (int) roi.getFloatHeight());
        }
    }

    public static void copyMinMax(final ImagePlus origin, final ImagePlus destination) {
        for (int slice = 1; slice <= origin.getStack().size(); slice++) {
            destination.setSlice(slice);
            origin.setSlice(slice);
            destination.getProcessor().setMinAndMax(origin.getProcessor().getMin(), origin.getProcessor().getMax());
            // destination.updateAndDraw();
        }
        IJ.run(destination, "Apply LUT", null);
    }

    public static float[] rgbToHsl(float red, float green, float blue, boolean includeRed, boolean includeGreen,
                                   boolean includeBlue, boolean includeColor,
                                   LSWSharpenMode mode) {
        float max = Math.max(Math.max(red, green), blue);
        float min = Math.min(Math.min(red, green), blue);
        float c = max - min;

        float hue_ = 0.f;
        if (c == 0) {
            hue_ = 0;
        } else if (max == red) {
            hue_ = (green - blue) / c;
            if (hue_ < 0)
                hue_ += 6.f;
        } else if (max == green) {
            hue_ = (blue - red) / c + 2.f;
        } else if (max == blue) {
            hue_ = (red - green) / c + 4.f;
        }
        float hue = 60.f * hue_;

        float luminance;
        if (mode == LSWSharpenMode.LUMINANCE) {
            float luminanceDivisor = (includeRed ? 1 : 0) + (includeGreen ? 1 : 0) + (includeBlue ? 1 : 0);
            luminance = ((includeRed ? red : 0) + (includeGreen ? green : 0) + (includeBlue ? blue : 0))
                    / luminanceDivisor;
        } else {
            luminance = (max + min) * 0.5f;
        }

        float saturation = 0.f;
        if (includeColor) {
            if (c == 0) {
                saturation = 0.f;
            } else {
                saturation = c / (1 - Math.abs(2.f * luminance - 1.f));
            }
        }

        float[] hsl = new float[3];
        hsl[0] = hue;
        hsl[1] = saturation;
        hsl[2] = luminance;
        return hsl;
    }

    public static float[] hslToRgb(float hue, float saturation, float luminance, float hueCorrectionFactor) {
        float c = (1 - Math.abs(2.f * luminance - 1.f)) * saturation;
        float hue_ = hue / 60.f;
        float h_mod2 = hue_;
        if (h_mod2 >= 4.f)
            h_mod2 -= 4.f;
        else if (h_mod2 >= 2.f)
            h_mod2 -= 2.f;

        float x = c * (1 - Math.abs(h_mod2 - 1));
        float r_, g_, b_;
        if (hue_ < 1) {
            r_ = c;
            g_ = x;
            b_ = 0;
        } else if (hue_ < 2) {
            r_ = x;
            g_ = c;
            b_ = 0;
        } else if (hue_ < 3) {
            r_ = 0;
            g_ = c;
            b_ = x;
        } else if (hue_ < 4) {
            r_ = 0;
            g_ = x;
            b_ = c;
        } else if (hue_ < 5) {
            r_ = x;
            g_ = 0;
            b_ = c;
        } else {
            r_ = c;
            g_ = 0;
            b_ = x;
        }

        float m = luminance - (0.5f * c);
        float red = ((r_ + m) + 0.5f) * (1f + hueCorrectionFactor);
        float green = ((g_ + m) + 0.5f) * (1f - hueCorrectionFactor);
        float blue = ((b_ + m) + 0.5f) * (1f + hueCorrectionFactor);
        return new float[]{red, green, blue};
    }

    public static boolean validateImageFormat(ImagePlus image, JFrame parentFrame, String activeOSProfile) {
        String message = "This file format is not supported. %nYou can only open 16-bit RGB and grayscale PNG and TIFF images.";
        boolean is16Bit = false;
        if (image != null) {
            ImageProcessor processor = image.getProcessor();
            is16Bit = processor instanceof ShortProcessor;
            if (processor instanceof ColorProcessor) {
                message += "%nThe file you selected is in 8-bit color format.";
            } else if (processor instanceof FloatProcessor) {
                message += "%nThe file you selected is in 32-bit grayscale format.";
            }
        }
        if (!is16Bit) {
            log.warn("Attempt to open a non 16-bit image");
            if (parentFrame != null) {
                if (Constants.SYSTEM_PROFILE_MAC.equals(activeOSProfile) || Constants.SYSTEM_PROFILE_LINUX.equals(activeOSProfile)) {
                    // Workaround for issue on macs, somehow needs to wait some milliseconds for the
                    // frame to be initialized.
                    LswUtil.waitMilliseconds(500);
                }
                JOptionPane.showMessageDialog(parentFrame, String.format(message));
            }
            return false;
        }
        return true;
    }

    public static int convertToUnsignedInt(final short value) {
        return value < 0 ? value + Constants.UNSIGNED_INT_SIZE : value;
    }

    public static short convertToShort(long value) {
        return (short) (value >= Constants.SHORT_HALF_SIZE ? value - Constants.UNSIGNED_INT_SIZE : value);
    }

    public static void setNonPersistentSettings(Profile profile) {
        setNonPersistentSettings(profile, profile.getScale(), profile.getOpenImageMode() == null ? OpenImageModeEnum.RGB.name() : profile.getOpenImageMode().name());
    }

    public static void setNonPersistentSettings(Profile profile, double scale,
                                                String openImageMode) {
        profile.setDispersionCorrectionEnabled(false); // dispersion correction is not meant to be persisted.
        profile.setLuminanceIncludeRed(true);
        profile.setLuminanceIncludeGreen(true);
        profile.setLuminanceIncludeBlue(true);
        profile.setLuminanceIncludeColor(true);
        profile.setScale(scale);
        profile.setOpenImageMode(openImageMode == null ? OpenImageModeEnum.RGB : OpenImageModeEnum.valueOf(openImageMode));
    }

    public static void convertLayersToColorImage(short[][] layers, ImagePlus image) {
        convertLayersToColorImage(layers, image, true, true, true);
    }

    public static void convertLayersToColorImage(short[][] layers, ImagePlus image, boolean includeRed, boolean includeGreen, boolean includeBlue) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] rgbPixels = new int[width * height];
        int channelIndex1 = includeRed && includeGreen && includeBlue ? 0 : includeRed ? 0 : includeGreen ? 1 : 2;
        int channelIndex2 = includeRed && includeGreen && includeBlue ? 1 : includeRed ? 0 : includeGreen ? 1 : 2;
        int channelIndex3 = includeRed && includeGreen && includeBlue ? 2 : includeRed ? 0 : includeGreen ? 1 : 2;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int red = convertToUnsignedInt(layers[channelIndex1][y * width + x]) >> 8;
                int green = convertToUnsignedInt(layers[channelIndex2][y * width + x]) >> 8;
                int blue = convertToUnsignedInt(layers[channelIndex3][y * width + x]) >> 8;
                rgbPixels[y * width + x] = red << 16 | green << 8 | blue;
            }
        }
        ImageProcessor processor = image.getProcessor();
        processor.setPixels(rgbPixels);
    }

    public static LswImageLayersDto getImageLayers(ImagePlus image) {
        ImageStack stack = image.getStack();
        short[][] newPixels = new short[3][stack.getProcessor(1).getPixelCount()];
        Executor executor = LswUtil.getParallelExecutor();
        for (int layer = 1; layer <= 3; layer++) {
            int finalLayer = layer;
            executor.execute(() -> {
                ImageProcessor p = stack.getProcessor(Math.min(stack.size(),finalLayer));
                short[] pixels = (short[]) p.getPixels();
                for (int i = 0; i < pixels.length; i++) {
                    newPixels[finalLayer - 1][i] = pixels[i];
                }
            });
        }
        LswUtil.stopAndAwaitParallelExecutor(executor);
        return LswImageLayersDto.builder().layers(newPixels).build();
    }

    public static void copyLayers(LswImageLayersDto layersDto, ImagePlus image, boolean includeRed, boolean includeGreen, boolean includeBlue) {
        short[][] layers = layersDto.getLayers();
        if (image.getProcessor() instanceof ColorProcessor) {
            convertLayersToColorImage(layers, image, includeRed, includeGreen, includeBlue);
        } else {
            ImageStack stack = image.getStack();
            Executor executor = LswUtil.getParallelExecutor();
            if (includeRed) {
                // Red
                executor.execute(() -> {
                    ImageProcessor p = stack.getProcessor(1);
                    short[] pixels = (short[]) p.getPixels();
                    for (int i = 0; i < pixels.length; i++) {
                        pixels[i] = layers[0][i];
                    }
                });
            }

            if (includeGreen) {
                // Green
                executor.execute(() -> {
                    ImageProcessor p = stack.getProcessor(2);
                    short[] pixels = (short[]) p.getPixels();
                    for (int i = 0; i < pixels.length; i++) {
                        pixels[i] = layers[1][i];
                    }
                });
            }

            if (includeBlue) {
                // Blue
                executor.execute(() -> {
                    ImageProcessor p = stack.getProcessor(3);
                    short[] pixels = (short[]) p.getPixels();
                    for (int i = 0; i < pixels.length; i++) {
                        pixels[i] = layers[2][i];
                    }
                });
            }
            LswUtil.stopAndAwaitParallelExecutor(executor);
        }
    }

    public static void copyLayer(LswImageLayersDto layersDto, ImagePlus image, int layer) {
        ImageStack stack = image.getStack();
        Executor executor = LswUtil.getParallelExecutor();
        short[][] layers = layersDto.getLayers();
        int sourceLayer = layer - 1;
        // Red
        executor.execute(() -> {
            ImageProcessor p = stack.getProcessor(layer);
            short[] pixels = (short[]) p.getPixels();
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = layers[sourceLayer][i];
            }
        });
        LswUtil.stopAndAwaitParallelExecutor(executor);
    }

    public static ImagePlus create16BitRGBImage(String filepath, LswImageLayersDto unprocessedImageLayers, int width, int height, boolean includeRed, boolean includeGreen, boolean includeBlue) {
        short[] redPixels;
        short[] emptyPixels = new short[width * height];
        Arrays.fill(emptyPixels, (short) 0);
        if (includeRed) {
            redPixels = unprocessedImageLayers.getLayers()[0];
        } else {
            redPixels = Arrays.copyOf(emptyPixels, emptyPixels.length);
        }
        short[] greenPixels;
        if (includeGreen) {
            greenPixels = unprocessedImageLayers.getLayers()[1];
        } else {
            greenPixels = Arrays.copyOf(emptyPixels, emptyPixels.length);
        }
        short[] bluePixels;
        if (includeBlue) {
            bluePixels = unprocessedImageLayers.getLayers()[2];
        } else {
            bluePixels = Arrays.copyOf(emptyPixels, emptyPixels.length);
        }

        ImageStack stack = new ImageStack(width, height);
        stack.addSlice("Red", redPixels);
        stack.addSlice("Green", greenPixels);
        stack.addSlice("Blue", bluePixels);

        ImagePlus imp = new ImagePlus(filepath, stack);
        imp.setDimensions(3, 1, 1);
        //imp.setFileInfo(fi);
        int mode = IJ.COMPOSITE;
        imp = new CompositeImage(imp, mode);
        for (int c = 1; c <= 3; c++) {
            imp.setPosition(c, 1, 1);
            //imp.setDisplayRange(minValue, maxValue);
        }
        imp.setPosition(1, 1, 1);
        return imp;
    }

    /*
    Excludes the background from RGB balance correction. Works with a gradual decrease in brightness when the background value is reached for the new Value
     */
    public static int preventBackgroundFromLightingUp(double oldValue, double newValue, double lowestValue, boolean preserveDarkBackground) {
        double correctedValue = newValue;
        if (preserveDarkBackground && oldValue < (MINIMUK_DARK_TRESHOLD + lowestValue)) {
            correctedValue = correctedValue * ((oldValue - lowestValue) / MINIMUK_DARK_TRESHOLD);
        }
        return (int) correctedValue;
    }

    public static ImagePlus crop(ImagePlus image, Roi roi, String filepath) {
        int xPos = (int) roi.getXBase();
        int yPos = (int) roi.getYBase();
        int width = (int) roi.getFloatWidth();
        int height = (int) roi.getFloatHeight();

        ImageStack stack = image.getStack();
        int imageWidth = stack.getWidth();
        int imageHeight = stack.getHeight();
        short[][] newPixels = new short[3][width * height];

        Executor executor = LswUtil.getParallelExecutor();
        for (int layer = 1; layer <= 3; layer++) {
            int finalLayer = layer;
            executor.execute(() -> {
                ImageProcessor p = stack.getProcessor(finalLayer);
                short[] shortPixels = (short[]) p.getPixels();
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int srcX = x + xPos;
                        int srcY = y + yPos;
                        if (srcX >= imageWidth || srcY >= imageHeight) {
                            continue; // Skip out-of-bounds pixels
                        }
                        int srcPos = srcY * imageWidth + srcX;
                        int destPos = y * width + x;
                        newPixels[finalLayer - 1][destPos] = shortPixels[srcPos];
                    }
                }
            });
        }
        LswUtil.stopAndAwaitParallelExecutor(executor);
        return create16BitRGBImage(filepath, LswImageLayersDto.builder().layers(newPixels).build(), width, height, true, true, true);
    }

    public static ImageProcessor createDeringMaskProcessor(
            float deringStrength, double deringRadius, int deringThreshold, ImageProcessor ip) {
        if (deringStrength > 0.0f) {
            ImageProcessor maskIp = ip.duplicate();
            FloatProcessor fp = createDeringMaskFloatProcessor(deringRadius, deringThreshold, maskIp);
            maskIp.setPixels(1, fp);
            return maskIp;
        }
        return null;
    }

    public static FloatProcessor createDeringMaskFloatProcessor(double radius, int threshold, ImageProcessor ip) {
        int minValue = 65535;
        int maxValue = 0;
        short[] maskPixels = (short[]) ip.getPixels();
        for (int position = 0; position < maskPixels.length; position++) {
            int value = LswImageProcessingUtil.convertToUnsignedInt(maskPixels[position]);
            if (value > maxValue) {
                maxValue = value;
            }
            if (value < minValue) {
                minValue = value;
            }
        }
        int average = (maxValue - minValue) / threshold; // start cutting of from threshold times the average.
        for (int position = 0; position < maskPixels.length; position++) {
            int value = LswImageProcessingUtil.convertToUnsignedInt(maskPixels[position]);
            if (value < average) {
                maskPixels[position] = 0;
            } else if (value > average) {
                maskPixels[position] = -1;
            }
        }
        FloatProcessor fp = ip.toFloat(1, null);
        GaussianBlur gb = new GaussianBlur();
        gb.blurGaussian(fp, radius, radius, 0.01);
        return fp;
    }

}
