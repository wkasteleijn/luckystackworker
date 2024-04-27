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
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import ij.ImageStack;
import ij.gui.NewImage;
import ij.io.Opener;
import nl.wilcokas.luckystackworker.service.dto.LswImageLayersDto;
import nl.wilcokas.luckystackworker.service.dto.OpenImageModeEnum;
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

                // Added since v5.2.0, so older version written yaml needs to stay compatible.
                if (profile.getOpenImageMode() == null) {
                    profile.setOpenImageMode(OpenImageModeEnum.RGB);
                }

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
                BigDecimal oldDenoiseSigma = profile.getDenoiseSigma() == null ? BigDecimal.ZERO : profile.getDenoiseSigma();
                if (BigDecimal.valueOf(2).compareTo(oldDenoiseSigma) < 0) {
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
                if (profile.getDeringThreshold() == 0) {
                    profile.setDeringThreshold(Constants.DEFAULT_THRESHOLD);
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
                if (profile.getSavitzkyGolaySize() == 0 && BigDecimal.ZERO.equals(oldDenoiseSigma)) {
                    // if prior to 3.0.0 no denoise was set, prefer the defaults for savitzky-golay
                    profile.setSavitzkyGolaySize(3);
                    profile.setSavitzkyGolayAmount(75);
                    profile.setSavitzkyGolayIterations(1);
                } else if (profile.getSavitzkyGolaySize() > 0 && BigDecimal.ZERO.compareTo(oldDenoiseSigma) < 0) {
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
            log.error("Error:",e);
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

    public static ImagePlus openImage(String filepath, OpenImageModeEnum openImageMode, double scale, UnaryOperator<ImagePlus> scaler) {
        return openImage(filepath, openImageMode, null, null, scale, scaler);
    }

    public static ImagePlus openImage(String filepath, OpenImageModeEnum openImageMode, ImagePlus currentImage, LswImageLayersDto currentUnprocessedImageLayers, double scale, UnaryOperator<ImagePlus> scaler) {
        ImagePlus newImage = new Opener().openImage(LswFileUtil.getIJFileFormat(filepath));
        if (scale > 1.0) {
            newImage = scaler.apply(newImage);
        }
        LswImageLayersDto unprocessedNewImageLayers = getImageLayers(newImage);
        boolean includeRed = openImageMode == OpenImageModeEnum.RED || openImageMode == OpenImageModeEnum.RGB;
        boolean includeGreen = openImageMode == OpenImageModeEnum.GREEN || openImageMode == OpenImageModeEnum.RGB;
        boolean includeBlue = openImageMode == OpenImageModeEnum.BLUE || openImageMode == OpenImageModeEnum.RGB;
        if (unprocessedNewImageLayers.getCount() == 3 && includeRed && includeGreen && includeBlue) {
            return newImage;
        }

        if (currentImage == null || currentImage.getWidth() != newImage.getWidth() || currentImage.getHeight() != newImage.getHeight()) {
            return create16BitRGBImage(filepath, unprocessedNewImageLayers, newImage.getWidth(), newImage.getHeight(), includeRed, includeGreen, includeBlue);
        } else {
            if (currentUnprocessedImageLayers != null) {
                copyLayers(currentUnprocessedImageLayers, currentImage, true, true, true);
            }
            if (includeRed) {
                copyLayer(unprocessedNewImageLayers, currentImage, 1);
            }
            if (includeGreen) {
                copyLayer(unprocessedNewImageLayers, currentImage, 2);
            }
            if (includeBlue) {
                copyLayer(unprocessedNewImageLayers, currentImage, 3);
            }
            return currentImage;
        }
    }

    public static LswImageLayersDto getImageLayers(ImagePlus image) {
        ImageStack stack = image.getStack();
        short[][] newPixels = new short[3][stack.getProcessor(1).getPixelCount()];
        Executor executor = LswUtil.getParallelExecutor();
        for (int layer = 1; layer <= stack.size(); layer++) {
            int finalLayer = layer;
            executor.execute(() -> {
                ImageProcessor p = stack.getProcessor(finalLayer);
                short[] pixels = (short[]) p.getPixels();
                for (int i = 0; i < pixels.length; i++) {
                    newPixels[finalLayer - 1][i] = pixels[i];
                }
            });
        }
        LswUtil.stopAndAwaitParallelExecutor(executor);
        return LswImageLayersDto.builder().layers(newPixels).count(stack.size()).build();
    }

    public static void copyLayers(LswImageLayersDto layersDto, ImagePlus image, boolean includeRed, boolean includeGreen, boolean includeBlue) {
        ImageStack stack = image.getStack();
        Executor executor = LswUtil.getParallelExecutor();
        short[][] layers = layersDto.getLayers();

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

    public static void copyLayer(LswImageLayersDto layersDto, ImagePlus image, int layer) {
        ImageStack stack = image.getStack();
        Executor executor = LswUtil.getParallelExecutor();
        short[][] layers = layersDto.getLayers();
        int sourceLayer = layersDto.getCount() == 1 ? 0 : layer-1;
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

    private static void hackIncorrectPngFileInfo(LSWFileSaver saver) {
        FileInfo fileInfo = (FileInfo) LswUtil.getPrivateField(saver, LSWFileSaver.class, "fi");
        LswUtil.setPrivateField(fileInfo, FileInfo.class, "fileType", FileInfo.RGB48);
    }

    private static ImagePlus create16BitRGBImage(String filepath, LswImageLayersDto unprocessedImageLayers, int width, int height, boolean includeRed, boolean includeGreen, boolean includeBlue) {
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
            greenPixels = unprocessedImageLayers.getLayers()[unprocessedImageLayers.getCount() == 1 ? 0 : 1];
        } else {
            greenPixels = Arrays.copyOf(emptyPixels, emptyPixels.length);
        }
        short[] bluePixels;
        if (includeBlue) {
            bluePixels = unprocessedImageLayers.getLayers()[unprocessedImageLayers.getCount() == 1 ? 0 : 2];
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

}
