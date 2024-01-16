package nl.wilcokas.luckystackworker.util;

import java.awt.image.ColorModel;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.yaml.snakeyaml.Yaml;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileInfo;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.filter.settings.LSWSharpenMode;
import nl.wilcokas.luckystackworker.model.Profile;

@Slf4j
public class LswFileUtil {

    private LswFileUtil() {
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
        return filename.indexOf(".") > 0 ? filename.substring(filename.lastIndexOf(".") + 1).toLowerCase() : "";
    }

    public static String getFilename(String path) {
        String filename = getFilenameFromPath(path);
        return filename.indexOf(".") > 0 ? filename.substring(0, filename.lastIndexOf(".")) : filename;
    }

    public static String getPathWithoutExtension(String path) {
        return path.indexOf(".") > 0 ? path.substring(0, path.lastIndexOf(".")) : path;
    }

    private static String getFilenameFromPath(String path) {
        return path.indexOf("/") >= 0 ? path.substring(path.lastIndexOf("/") + 1) : path;
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

    public static void saveImage(ImagePlus image, String profileName, String path, boolean fixRgbStack, boolean crop, boolean asJpg, boolean fromWorker) throws IOException {
        if (crop) {
            image = image.crop();
        }
        if (fixRgbStack) {
            image.setActiveChannels("111");
            image.setC(1);
            image.setZ(1);
        }
        LSWFileSaver saver = new LSWFileSaver(image);
        if (fixRgbStack) {
            hackIncorrectPngFileInfo(saver);
        }
        if (fromWorker) {
            String folder = getFileDirectory(path);
            if (!folder.endsWith(profileName + Constants.WORKER_FOLDER_POSTFIX)) {
                String newFolder = folder + "/" + profileName + Constants.WORKER_FOLDER_POSTFIX;
                Files.createDirectories(Paths.get(newFolder));
                path = newFolder + "/" + getFilenameFromPath(getIJFileFormat(path));
            }
        }
        if (asJpg) {
            saver.setJpegQuality(100);
            saver.saveAsJpeg(path);
        } else {
            saver.saveAsTiff(path);
        }
    }

    public static ImagePlus fixNonTiffOpeningSettings(ImagePlus image) {
        if (isStack(image)) {
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
        } else {
            image.setDisplayMode(IJ.GRAYSCALE);
            return image;
        }
    }

    public static boolean isPngRgbStack(ImagePlus image, String filePath) {
        return isPng(image, filePath) && isStack(image);
    }
    public static boolean isPng(ImagePlus image, String filePath) {
        return filePath.toLowerCase().endsWith(".png");
    }

    public static boolean isStack(ImagePlus image) {
        return image.hasImageStack() && image.getStack().getSize() > 1;
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

                // Added since v5.0.0, so older version written yaml needs to stay compatible.
                if (profile.getIansAmount() == null) {
                    profile.setIansAmount(BigDecimal.ZERO);
                }
                if (profile.getIansRecovery() == null) {
                    profile.setIansRecovery(BigDecimal.ZERO);
                }
                if (profile.getDenoiseAlgorithm1() == null) {
                    profile.setDenoiseAlgorithm1(Constants.DEFAULT_DENOISEALGORITHM);
                }
                if (profile.getDenoiseAlgorithm2() == null) {
                    profile.setDenoiseAlgorithm2(Constants.DEFAULT_DENOISEALGORITHM);
                }
                if (profile.getDenoise2Radius() == null) {
                    profile.setDenoise2Radius(BigDecimal.ONE);
                }

                // Renamed since v5.0.0
                if (BigDecimal.valueOf(2).compareTo(profile.getDenoiseSigma()) < 0) {
                    profile.setDenoiseAlgorithm2(Constants.DENOISE_ALGORITHM_SIGMA2);
                    profile.setDenoise2Radius(profile.getDenoiseRadius());
                    profile.setDenoise2Iterations(profile.getDenoiseIterations());
                } else if (profile.getSavitzkyGolaySize() > 0) {
                    profile.setDenoiseAlgorithm2(Constants.DENOISE_ALGORITHM_SAVGOLAY);
                } else {
                    profile.setDenoiseAlgorithm1(Constants.DENOISE_ALGORITHM_SIGMA1);
                    profile.setDenoise1Amount(profile.getDenoise() == null ? BigDecimal.ZERO : profile.getDenoise());
                    profile.setDenoise1Radius(profile.getDenoiseRadius() == null ? BigDecimal.ONE : profile.getDenoiseRadius());
                    profile.setDenoise1Iterations(profile.getDenoiseIterations());
                }

                // Added since v4.8.0, so older version written yaml needs to stay compatible.
                if (profile.getThreshold() == 0) {
                    profile.setThreshold(Constants.DEFAULT_THRESHOLD);
                }
                if (profile.getScale() == 0D) {
                    profile.setScale(1D);
                }
                if (profile.getPurple() == null) {
                    profile.setPurple(BigDecimal.ZERO);
                }

                // Added since v3.2.0, so older version written yaml needs to stay compatible.
                if (profile.getLocalContrastMode() == null) {
                    profile.setLocalContrastMode(LSWSharpenMode.LUMINANCE.toString());
                }

                if (profile.getDeringStrength() == 0) {
                    profile.setDeringStrength(0);
                    profile.setDeringRadius(new BigDecimal(5));
                }

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
                if (profile.getDenoise1Radius() == null) {
                    profile.setDenoise1Radius(Constants.DEFAULT_DENOISE_RADIUS);
                    profile.setDenoiseSigma(Constants.DEFAULT_DENOISE_SIGMA);
                }
                if (profile.getDenoise1Iterations() == 0) {
                    profile.setDenoise1Iterations(Constants.DEFAULT_DENOISE_ITERATIONS);
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

    public static String getDataFolder(String osProfile) {
        String dataFolder = System.getProperty("user.home");
        if (Constants.SYSTEM_PROFILE_MAC.equals(osProfile) || Constants.SYSTEM_PROFILE_LINUX.equals(osProfile)) {
            dataFolder += "/.lsw";
        } else if (Constants.SYSTEM_PROFILE_WINDOWS.equals(osProfile)) {
            dataFolder += "/AppData/Local/LuckyStackWorker";
        }
        return dataFolder;
    }

    private static void hackIncorrectPngFileInfo(LSWFileSaver saver) {
        FileInfo fileInfo = (FileInfo) LswUtil.getPrivateField(saver, LSWFileSaver.class, "fi");
        LswUtil.setPrivateField(fileInfo, FileInfo.class, "fileType", FileInfo.RGB48);
    }

}
