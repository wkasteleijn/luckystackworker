package nl.wilcokas.luckystackworker.util;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

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
        return new float[] { red, green, blue };
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
        setNonPersistentSettings(profile, profile.getScale());
    }

    public static void setNonPersistentSettings(Profile profile, double scale) {
        profile.setDispersionCorrectionEnabled(false); // dispersion correction is not meant to be persisted.
        profile.setLuminanceIncludeRed(true);
        profile.setLuminanceIncludeGreen(true);
        profile.setLuminanceIncludeBlue(true);
        profile.setLuminanceIncludeColor(true);
        profile.setScale(scale);
    }
}
