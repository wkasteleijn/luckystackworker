package nl.wilcokas.luckystackworker.util;

import java.awt.image.ColorModel;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.ReflectionUtils;
import org.yaml.snakeyaml.Yaml;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.io.FileSaver;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.filter.settings.LSWSharpenMode;
import nl.wilcokas.luckystackworker.model.Profile;

@Slf4j
public class Util {

    private Util() {
    }

    public static boolean fileExists(String path) {
        return Files.exists(Paths.get(path));
    }

    public static String getFilename(File file) {
        return getFilename(file.getAbsolutePath());
    }

    public static String getFilenameExtension(File file) {
        return getFilenameExtension(file.getAbsolutePath());
    }

    public static String getFilenameExtension(String path) {
        String filename = getFilenameFromPath(path);
        return filename.indexOf(".") > 0 ? filename.substring(filename.lastIndexOf(".")+1).toLowerCase() : "";
    }

    public static String getFilename(String path) {
        String filename = getFilenameFromPath(path);
        return filename.indexOf(".") > 0 ? filename.substring(0, filename.lastIndexOf(".")).toLowerCase() : filename;
    }

    private static String getFilenameFromPath(String path) {
        return path.indexOf("/") >= 0 ? path.substring(path.lastIndexOf("/") + 1) : path;
    }

    @SuppressWarnings("static-access")
    public static void pause(long waitDelay) throws InterruptedException {
        Thread.currentThread().sleep(waitDelay);
    }

    public static String getIJFileFormat(String path) {
        return path.replaceAll("\\\\", "/");
    }

    public static String deriveProfileFromImageName(String path) {
        String name = getImageName(getIJFileFormat(path)).toLowerCase();
        if (name.contains("mer")) {
            return "mer";
        } else if (name.contains("ven")) {
            return "ven";
        } else if (name.contains("moon")) {
            return "moon";
        } else if (name.contains("mars")) {
            return "mars";
        } else if (name.contains("jup")) {
            return "jup";
        } else if (name.contains("sat")) {
            return "sat";
        } else if (name.contains("uranus")) {
            return "uranus";
        } else if (name.contains("neptune")) {
            return "neptune";
        } else if (name.contains("sun")) {
            return "sun";
        }
        return null;
    }

    public static String getFileDirectory(String path) {
        String ijFormatPath = getIJFileFormat(path);
        return ijFormatPath.substring(0, ijFormatPath.lastIndexOf("/"));
    }

    public static String getImageName(String path) {
        return path.substring(path.lastIndexOf("/") + 1);
    }

    public static Profile toProfile(Map<String, String> props, final String profileName) {
        return Profile.builder().amount(new BigDecimal(getSetting(props, "amount", profileName))) //
                .radius(new BigDecimal(getSetting(props, "radius", profileName))) //
                .iterations(Integer.valueOf(getSetting(props, "iterations", profileName))) //
                .denoise(new BigDecimal(getSetting(props, "denoise", profileName))) //
                .denoiseSigma(new BigDecimal(getSetting(props, "denoiseSigma", profileName))) //
                .denoiseRadius(new BigDecimal(getSetting(props, "denoiseRadius", profileName))) //
                .denoiseIterations(Integer.valueOf(getSetting(props, "denoiseIterations", profileName))) //
                .savitzkyGolayAmount(Integer.valueOf(getSetting(props, "savitzkyGolayAmount", profileName))) //
                .savitzkyGolayIterations(Integer.valueOf(getSetting(props, "savitzkyGolayIterations", profileName))) //
                .savitzkyGolaySize(Integer.valueOf(getSetting(props, "savitzkyGolaySize", profileName))) //
                .clippingStrength(Integer.valueOf(getSetting(props, "clippingStrength", profileName))) //
                .clippingRange(Integer.valueOf(getSetting(props, "clippingRange", profileName))) //
                .sharpenMode(getSetting(props, "sharpenMode", profileName)) //
                .gamma(new BigDecimal(getSetting(props, "gamma", profileName))) //
                .contrast(Integer.valueOf(getSetting(props, "contrast", profileName))) //
                .brightness(Integer.valueOf(getSetting(props, "brightness", profileName))) //
                .background(Integer.valueOf(getSetting(props, "background", profileName))) //
                .red(new BigDecimal(getSetting(props, "red", profileName))) //
                .green(new BigDecimal(getSetting(props, "green", profileName))) //
                .blue(new BigDecimal(getSetting(props, "blue", profileName))) //
                .saturation(new BigDecimal(getSetting(props, "saturation", profileName))) //
                .name(profileName).build();
    }

    public static String readFromInputStream(InputStream inputStream) {
        try {
            StringBuilder resultStringBuilder = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = br.readLine()) != null) {
                    resultStringBuilder.append(line).append("\n");
                }
            }
            return resultStringBuilder.toString();
        } catch (IOException e) {
            log.error("Error reading from inputStream: ", e);
        }
        return null;
    }

    public static void deleteFile(String path) throws IOException {
        Files.delete(Paths.get(path));
    }

    public static void saveImage(ImagePlus image, String path, boolean isPngRgbStack, boolean crop) throws IOException {
        if (crop) {
            image = image.crop();
        }
        if (isPngRgbStack) {
            image.setActiveChannels("111");
            image.setC(1);
            image.setZ(1);
        }
        FileSaver saver = new FileSaver(image);
        if (isPngRgbStack) {
            hackIncorrectPngFileInfo(saver);
        }
        saver.saveAsTiff(path);
    }

    public static ImagePlus fixNonTiffOpeningSettings(ImagePlus image) {
        log.info("Applying workaround for correctly opening PNG RGB stack");
        ImagePlus result = new CompositeImage(image, IJ.COMPOSITE);
        result.getStack().setSliceLabel("red", 1);
        result.getStack().setSliceLabel("green", 2);
        result.getStack().setSliceLabel("blue", 3);
        result.getStack().setColorModel(ColorModel.getRGBdefault());
        result.setActiveChannels("111");
        result.setC(1);
        result.setZ(1);
        result.setDisplayMode(IJ.COMPOSITE);
        result.setOpenAsHyperStack(true);
        result.getFileInfo().fileType = FileInfo.RGB48;
        return result;
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

    public static boolean isPngRgbStack(ImagePlus image, String filePath) {
        return filePath.toLowerCase().endsWith(".png") && image.isStack() && image.getStack().getSize() > 1;
    }

    public static void writeProfile(Profile profile, String path) throws IOException {
        Files.writeString(Paths.get(path + ".yaml"), new Yaml().dump(profile));
    }

    public static Profile readProfile(String filePath) {
        String profileStr = null;
        try {
            profileStr = Files.readString(Paths.get(filePath + ".yaml"));
            if (profileStr != null) {
                Profile profile = new Yaml().load(profileStr);

                // Added since v3.0.0, so older version written yaml needs to stay compatible.
                if (profile.getSharpenMode() == null) {
                    profile.setSharpenMode(LSWSharpenMode.LUMINANCE.toString());
                }
                if (profile.getClippingStrength() == 0) {
                    profile.setClippingStrength(0);
                    profile.setClippingRange(50);
                }
                if (profile.getSavitzkyGolaySize() == 0
                        && (profile.getDenoiseSigma() == null || BigDecimal.ZERO.equals(profile.getDenoiseSigma()))) {
                    // if prior to 3.0.0 no denoise was set, prefer the defaults for savitzky-golay
                    profile.setSavitzkyGolaySize(3);
                    profile.setSavitzkyGolayAmount(75);
                    profile.setSavitzkyGolayIterations(1);
                } else if (profile.getSavitzkyGolaySize() > 0
                        && (profile.getDenoiseSigma() != null
                                || BigDecimal.ZERO.compareTo(profile.getDenoiseSigma()) < 0)) {
                    // Prevent both being set, prefer savitzky-golay in that case.
                    profile.setDenoiseSigma(BigDecimal.ZERO);
                }

                // Added since v2.3.0, so older version written yaml needs to stay compatible.
                if (profile.getSavitzkyGolayIterations() == 0) {
                    profile.setSavitzkyGolayIterations(1);
                }

                // Added since v1.5.0, so older version written yaml needs to stay compatible.
                if (profile.getDenoiseRadius() == null) {
                    profile.setDenoiseRadius(Constants.DEFAULT_DENOISE_RADIUS);
                    profile.setDenoiseSigma(Constants.DEFAULT_DENOISE_SIGMA);
                }
                if (profile.getDenoiseIterations() == 0) {
                    profile.setDenoiseIterations(Constants.DEFAULT_DENOISE_ITERATIONS);
                }
                if (profile.getSaturation() == null) {
                    profile.setSaturation(BigDecimal.valueOf(1));
                }

                return profile;
            }
        } catch (Exception e) {
            log.warn("No profile file found or profile file is corrupt for {}", filePath);
        }
        return null;
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
            boolean includeBlue,
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

        float saturation;
        if (c == 0) {
            saturation = 0.f;
        } else {
            saturation = c / (1 - Math.abs(2.f * luminance - 1.f));
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

    public static boolean validateImageFormat(ImagePlus image, JFrame parentFrame) {
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
                JOptionPane.showMessageDialog(parentFrame, String.format(message));
            }
            return false;
        }
        return true;
    }

    private static String getSetting(Map<String, String> props, String setting, String name) {
        return props.get(name + "." + setting);
    }

    private static void hackIncorrectPngFileInfo(FileSaver saver) {
        Field field = ReflectionUtils.findField(FileSaver.class, "fi");
        ReflectionUtils.makeAccessible(field);
        FileInfo fileInfo = (FileInfo) ReflectionUtils.getField(field, saver);
        Field fileType = ReflectionUtils.findField(FileInfo.class, "fileType");
        ReflectionUtils.makeAccessible(fileType);
        ReflectionUtils.setField(fileType, fileInfo, FileInfo.RGB48);
    }

}
