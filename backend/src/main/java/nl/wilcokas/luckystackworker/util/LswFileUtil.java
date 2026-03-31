package nl.wilcokas.luckystackworker.util;

import static nl.wilcokas.luckystackworker.constants.Constants.COMPRESSED_TIF_OUTPUTFORMAT;
import static nl.wilcokas.luckystackworker.constants.Constants.JPG_OUTPUTFORMAT;
import static nl.wilcokas.luckystackworker.constants.Constants.PNG8_OUTPUTFORMAT;
import static nl.wilcokas.luckystackworker.constants.Constants.PNG_OUTPUTFORMAT;
import static nl.wilcokas.luckystackworker.constants.Constants.WEBP_OUTPUTFORMAT;
import static nl.wilcokas.luckystackworker.model.ImageOutputFormatType.CTIF;
import static nl.wilcokas.luckystackworker.model.ImageOutputFormatType.JPG;
import static nl.wilcokas.luckystackworker.model.ImageOutputFormatType.PNG;
import static nl.wilcokas.luckystackworker.model.ImageOutputFormatType.PNG8;
import static nl.wilcokas.luckystackworker.model.ImageOutputFormatType.TIF;
import static nl.wilcokas.luckystackworker.model.ImageOutputFormatType.WEBP;

import com.luciad.imageio.webp.CompressionType;
import com.luciad.imageio.webp.WebPWriteParam;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileInfo;
import ij.io.Opener;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Iterator;
import java.util.function.UnaryOperator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.IIORegistry;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.exceptions.LswNotReadyException;
import nl.wilcokas.luckystackworker.exceptions.NotARawImageException;
import nl.wilcokas.luckystackworker.filter.settings.LSWSharpenMode;
import nl.wilcokas.luckystackworker.ij.LswImageViewer;
import nl.wilcokas.luckystackworker.model.ImageOutputFormatType;
import nl.wilcokas.luckystackworker.model.PSF;
import nl.wilcokas.luckystackworker.model.PSFType;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.service.bean.LswImageLayers;
import nl.wilcokas.luckystackworker.service.bean.OpenImageModeEnum;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.inspector.TagInspector;

@Slf4j
public class LswFileUtil {

    private LswFileUtil() {}

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
        return filename.indexOf(".") > 0
                ? filename.substring(filename.lastIndexOf(".") + 1).toLowerCase()
                : "";
    }

    public static String getFilename(String path) {
        String filename = getFilenameFromPath(path);
        return filename.indexOf(".") > 0 ? filename.substring(0, filename.lastIndexOf(".")) : filename;
    }

    public static String getPathWithoutExtension(String path) {
        return path.indexOf(".") > 0 ? path.substring(0, path.lastIndexOf(".")) : path;
    }

    public static String getFilenameFromPath(String path) {
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
        } else if (name.contains("iss")) {
            return "ISS";
        } else if (name.contains("eur")) {
            return "Europa";
        } else if (name.contains("gan")) {
            return "Ganymede";
        } else if (name.contains("cal")) {
            return "Callisto";
        } else if (name.contains("io")) {
            return "Io";
        } else if (name.contains("tit")) {
            return "Titan";
        }
        return "unspecified";
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

    public static void saveImage(
            ImagePlus image,
            String profileName,
            String path,
            boolean fixRgbStack,
            boolean crop,
            ImageOutputFormatType outputFormatType,
            boolean fromWorker)
            throws IOException {
        if (crop) {
            image = image.crop();
        }
        if (fromWorker) {
            String folder = getFileDirectory(path);
            if (!folder.endsWith(profileName + Constants.WORKER_FOLDER_POSTFIX)) {
                String newFolder = folder + "/" + profileName + Constants.WORKER_FOLDER_POSTFIX;
                Files.createDirectories(Paths.get(newFolder));
                path = newFolder + "/" + getFilenameFromPath(getIJFileFormat(path));
            }
        }
        if (outputFormatType == JPG) {
            LSWFileSaver saver = new LSWFileSaver(createSingleLayerColorImage(image));
            saver.setJpegQuality(100);
            saver.saveAsJpeg(path);
        } else if (outputFormatType == PNG) {
            saveAs48BitPngNative(image, path);
        } else if (outputFormatType == PNG8) {
            LSWFileSaver saver = new LSWFileSaver(createSingleLayerColorImage(image));
            saver.saveAsPng(path);
        } else if (outputFormatType == CTIF) {
            saveAs48BitTiffCompressed(image, path);
        } else if (outputFormatType == WEBP) {
            save8BitLosslessWebp(image, path);
        } else {
            if (fixRgbStack) {
                image.setActiveChannels("111");
                image.setC(1);
                image.setZ(1);
            }
            LSWFileSaver saver = new LSWFileSaver(image);
            if (fixRgbStack) {
                hackIncorrectPngFileInfo(saver);
            }
            saver.saveAsTiff(path);
        }
    }

    private static void saveAs48BitPngNative(ImagePlus imp, String outputPath) throws IOException {
        int width = imp.getWidth();
        int height = imp.getHeight();
        ImageStack stack = imp.getStack();
        short[] r = (short[]) stack.getProcessor(1).getPixels();
        short[] g = (short[]) stack.getProcessor(2).getPixels();
        short[] b = (short[]) stack.getProcessor(3).getPixels();
        DataBufferUShort db = new DataBufferUShort(new short[][]{r, g, b}, r.length);
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        int[] bits = {16, 16, 16};
        ComponentColorModel cm = new ComponentColorModel(cs, bits, false, false,
                Transparency.OPAQUE,
                DataBuffer.TYPE_USHORT);
        BandedSampleModel sm = new BandedSampleModel(DataBuffer.TYPE_USHORT, width, height, 3);
        WritableRaster raster = Raster.createWritableRaster(sm, db, null);
        BufferedImage bi = new BufferedImage(cm, raster, false, null);
        ImageIO.write(bi, "PNG", new File(outputPath));
    }

    public static void saveAs48BitTiffCompressed(ImagePlus imp, String outputPath) throws IOException {
        int width = imp.getWidth();
        int height = imp.getHeight();
        ImageStack stack = imp.getStack();

        // 1. Prepare the 16-bit 3-bank DataBuffer (Red, Green, Blue)
        short[] r = (short[]) stack.getPixels(1);
        short[] g = (short[]) stack.getPixels(2);
        short[] b = (short[]) stack.getPixels(3);
        DataBufferUShort db = new DataBufferUShort(new short[][]{r, g, b}, r.length);

        // 2. Build the ColorModel and SampleModel
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        int[] bits = {16, 16, 16};
        ComponentColorModel cm = new ComponentColorModel(cs, bits, false, false,
                Transparency.OPAQUE,
                DataBuffer.TYPE_USHORT);
        BandedSampleModel sm = new BandedSampleModel(DataBuffer.TYPE_USHORT, width, height, 3);

        WritableRaster raster = Raster.createWritableRaster(sm, db, null);
        BufferedImage bi = new BufferedImage(cm, raster, false, null);

        // 3. Initialize the TIFF Writer
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("TIFF");
        if (!writers.hasNext()) throw new RuntimeException("No TIFF writer found!");
        ImageWriter writer = writers.next();

        File outputFile = new File(outputPath);
        try (var ios = ImageIO.createImageOutputStream(outputFile)) {
            writer.setOutput(ios);

            // 4. SET COMPRESSION PARAMETERS HERE
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                // "LZW" is the most common lossless compression for 16-bit TIFFs
                // Other options include "PackBits" or "ZLib" (Deflate)
                param.setCompressionType("LZW");
            }

            // 5. Setup Metadata (ensures the 16-bit tags are correctly written)
            ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromRenderedImage(bi);
            IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, param);

            // 6. Execute the Write
            writer.write(null, new IIOImage(bi, null, metadata), param);
        } finally {
            writer.dispose();
        }
    }

    public static void save8BitLosslessWebp(ImagePlus image, String outputFilePath) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType("image/webp");
        if (!writers.hasNext()) {
            throw new IllegalStateException("No WebP writer found!");
        }
        ImageWriter writer = writers.next();

        // 2. Configure encoding parameters
        // We cast to WebPWriteParam to access specific WebP features
        WebPWriteParam writeParam = (WebPWriteParam) writer.getDefaultWriteParam();

        // Kotlin's .apply { ... } block converted to standard setters
        writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        writeParam.setCompressionType(CompressionType.Lossless);

        // 3. Configure the output
        try (FileImageOutputStream output = new FileImageOutputStream(new File(outputFilePath))) {
            writer.setOutput(output);

            // 4. Encode and Write
            writer.write(null, new IIOImage(image.getBufferedImage(), null, null), writeParam);
        } finally {
            writer.dispose();
        }
    }

    @NotNull
    private static LswImageViewer createSingleLayerColorImage(ImagePlus image) {
        LswImageViewer singleLayerColorImage =
                new LswImageViewer(StringUtils.EMPTY, new ColorProcessor(image.getWidth(), image.getHeight()));
        LswImageProcessingUtil.convertLayersToColorImage(
                LswImageProcessingUtil.getImageLayers(image).getLayers(), singleLayerColorImage);
        return singleLayerColorImage;
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
                var loaderoptions = new LoaderOptions();
                TagInspector taginspector = tag -> tag.getClassName().equals(Profile.class.getName());
                loaderoptions.setTagInspector(taginspector);
                Profile profile = new Yaml(new Constructor(Profile.class, loaderoptions)).load(profileStr);
                correctProfileForBackwardCompatability(profile);
                return profile;
            }
        } catch (Exception e) {
            log.warn("No profile file found or profile file is corrupt for {}", filePath);
        }
        return null;
    }

    public static void correctProfileForBackwardCompatability(Profile profile) {

        // Added since v1.5.0, so older version written yaml needs to stay compatible.
        if (profile.getDenoise1Radius() == null) {
            profile.setDenoise1Radius(Constants.DEFAULT_DENOISE_RADIUS);
            profile.setDenoiseSigma(Constants.DEFAULT_DENOISE_SIGMA);
            profile.setDenoise1Amount(new BigDecimal(50));
        }
        if (profile.getDenoise1Iterations() == 0) {
            profile.setDenoise1Iterations(Constants.DEFAULT_DENOISE_ITERATIONS);
        }
        if (profile.getSaturation() == null) {
            profile.setSaturation(BigDecimal.valueOf(1));
        }

        // Added since v2.3.0, so older version written yaml needs to stay compatible.
        if (profile.getSavitzkyGolayIterations() == 0) {
            profile.setSavitzkyGolayIterations(1);
        }

        // Added since v3.0.0, so older version written yaml needs to stay compatible.
        if (profile.getSharpenMode() == null) {
            profile.setSharpenMode(LSWSharpenMode.LUMINANCE.toString());
        }
        if (profile.getClippingStrength() == 0) {
            profile.setClippingStrength(0);
            profile.setClippingRange(50);
        }
        BigDecimal oldDenoiseSigma = profile.getDenoiseSigma() == null ? BigDecimal.ZERO : profile.getDenoiseSigma();
        if (profile.getSavitzkyGolaySize() == 0) {
            // if prior to 3.0.0 no denoise was set, prefer the defaults for savitzky-golay
            profile.setSavitzkyGolaySize(3);
            profile.setSavitzkyGolayAmount(100);
            profile.setSavitzkyGolayIterations(1);
        } else if (profile.getSavitzkyGolaySize() > 0 && BigDecimal.ZERO.compareTo(oldDenoiseSigma) < 0) {
            // Prevent both being set, prefer savitzky-golay in that case.
            profile.setDenoiseSigma(BigDecimal.ZERO);
        }

        // Added since v3.2.0, so older version written yaml needs to stay compatible.
        if (profile.getLocalContrastMode() == null) {
            profile.setLocalContrastMode(LSWSharpenMode.LUMINANCE.toString());
        }

        if (profile.getDeringStrength() == 0) {
            profile.setDeringStrength(0);
            profile.setDeringRadius(new BigDecimal(5));
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

        // Added since v5.0.0, so older version written yaml needs to stay compatible.
        if (profile.getIansAmount() == null) {
            profile.setIansAmount(BigDecimal.ZERO);
        }
        if (profile.getIansRecovery() == null) {
            profile.setIansRecovery(BigDecimal.ZERO);
        }
        if (profile.getDenoiseAlgorithm1() == null) {
            if (BigDecimal.valueOf(2).compareTo(oldDenoiseSigma) >= 0) {
                profile.setDenoiseAlgorithm1(Constants.DENOISE_ALGORITHM_SIGMA1);
                profile.setDenoise1Amount(profile.getDenoise() == null ? BigDecimal.ZERO : profile.getDenoise());
                profile.setDenoise1Radius(
                        profile.getDenoiseRadius() == null ? BigDecimal.ONE : profile.getDenoiseRadius());
                profile.setDenoise1Iterations(profile.getDenoiseIterations());
            } else {
                profile.setDenoiseAlgorithm1(Constants.DEFAULT_DENOISEALGORITHM);
            }
        }
        if (profile.getDenoiseAlgorithm2() == null) {
            if (profile.getSavitzkyGolaySize() > 0) {
                profile.setDenoiseAlgorithm2(Constants.DENOISE_ALGORITHM_SAVGOLAY);
            } else {
                profile.setDenoiseAlgorithm2(Constants.DEFAULT_DENOISEALGORITHM);
            }
        }
        if (profile.getDenoise2Radius() == null) {
            profile.setDenoise2Radius(BigDecimal.ONE);
        }

        // Renamed since v5.0.0
        if (BigDecimal.valueOf(2).compareTo(oldDenoiseSigma) < 0) {
            profile.setDenoiseAlgorithm2(Constants.DENOISE_ALGORITHM_SIGMA2);
            profile.setDenoise2Radius(profile.getDenoiseRadius());
            profile.setDenoise2Iterations(profile.getDenoiseIterations());
        }

        // Added since v5.2.0, so older version written yaml needs to stay compatible.
        if (profile.getOpenImageMode() == null) {
            profile.setOpenImageMode(OpenImageModeEnum.RGB);
        }

        // Added since v6.0.0, so older version written yaml needs to stay compatible.
        if (profile.getDenoise1AmountGreen() == null) {
            profile.setDenoise1AmountGreen(profile.getDenoise1Amount());
            profile.setDenoise1RadiusGreen(profile.getDenoise1Radius());
            profile.setDenoise1IterationsGreen(profile.getDenoise1Iterations());
            profile.setDenoise2RadiusGreen(profile.getDenoise2Radius());
            profile.setDenoise2IterationsGreen(profile.getDenoise2Iterations());
            profile.setSavitzkyGolaySizeGreen(profile.getSavitzkyGolaySize());
            profile.setSavitzkyGolayAmountGreen(profile.getSavitzkyGolayAmount());
            profile.setSavitzkyGolayIterationsGreen(profile.getSavitzkyGolayIterations());
            profile.setDenoise1AmountBlue(profile.getDenoise1Amount());
            profile.setDenoise1RadiusBlue(profile.getDenoise1Radius());
            profile.setDenoise1IterationsBlue(profile.getDenoise1Iterations());
            profile.setDenoise2RadiusBlue(profile.getDenoise2Radius());
            profile.setDenoise2IterationsBlue(profile.getDenoise2Iterations());
            profile.setSavitzkyGolaySizeBlue(profile.getSavitzkyGolaySize());
            profile.setSavitzkyGolayAmountBlue(profile.getSavitzkyGolayAmount());
            profile.setSavitzkyGolayIterationsBlue(profile.getSavitzkyGolayIterations());
        }
        if (profile.getRadiusGreen() == null) {
            profile.setRadiusGreen(profile.getRadius());
            profile.setAmountGreen(profile.getAmount());
            profile.setIterationsGreen(profile.getIterations());
            profile.setLevelGreen(profile.getLevel());
            profile.setClippingStrengthGreen(profile.getClippingStrength());
            profile.setClippingRangeGreen(profile.getClippingRange());
            profile.setDeringRadiusGreen(profile.getDeringRadius());
            profile.setDeringStrengthGreen(profile.getDeringStrength());
            profile.setDeringThresholdGreen(profile.getDeringThreshold());
            profile.setRadiusBlue(profile.getRadius());
            profile.setAmountBlue(profile.getAmount());
            profile.setIterationsBlue(profile.getIterations());
            profile.setLevelBlue(profile.getLevel());
            profile.setClippingStrengthBlue(profile.getClippingStrength());
            profile.setClippingRangeBlue(profile.getClippingRange());
            profile.setDeringRadiusBlue(profile.getDeringRadius());
            profile.setDeringStrengthBlue(profile.getDeringStrength());
            profile.setDeringThresholdBlue(profile.getDeringThreshold());
        }

        // Added since 6.6.0, so older version written yaml needs to stay compatible.
        if (profile.getIansAmountMid() == null) {
            profile.setIansAmountMid(BigDecimal.ZERO);
        }
        if (profile.getIansIterations() == 0) {
            profile.setIansIterations(1);
        }

        // Added since 6.12.0, so older version written yaml needs to stay compatible.
        if (profile.getRofIterations() == 0) {
            profile.setRofIterations(5);
            profile.setRofIterationsGreen(5);
            profile.setRofIterationsBlue(5);
        }
        if (profile.getRofTheta() == 0) {
            profile.setRofTheta(50);
            profile.setRofThetaGreen(50);
            profile.setRofThetaBlue(50);
        }
        if (profile.getApplyUnsharpMask() == null) {
            profile.setApplyUnsharpMask(true);
            profile.setApplyWienerDeconvolution(false);
        }
        if (profile.getWienerIterations() == 0) {
            profile.setWienerIterations(5);
            profile.setWienerIterationsGreen(5);
            profile.setWienerIterationsBlue(5);
        }
        if (profile.getBilateralSigmaSpace() == 0) {
            profile.setBilateralSigmaSpace(50);
            profile.setBilateralSigmaSpaceGreen(50);
            profile.setBilateralSigmaSpaceBlue(50);
        }
        if (profile.getBilateralSigmaColor() == 0) {
            profile.setBilateralSigmaColor(150);
            profile.setBilateralSigmaColorGreen(150);
            profile.setBilateralSigmaColorBlue(150);
        }
        if (profile.getBilateralIterations() == 0) {
            profile.setBilateralIterations(1);
            profile.setBilateralIterationsGreen(1);
            profile.setBilateralIterationsBlue(1);
        }
        if (profile.getBilateralRadius() == 0) {
            profile.setBilateralRadius(1);
            profile.setBilateralRadiusGreen(1);
            profile.setBilateralRadiusBlue(1);
        }
        if (profile.getPsf() == null) {
            PSF psf = new PSF();
            psf.setAiryDiskRadius(20);
            psf.setDiffractionIntensity(60);
            psf.setSeeingIndex(4);
            psf.setType(PSFType.SYNTHETIC);
            profile.setPsf(psf);
        }
        if (profile.getApplyUnsharpMask() == null) {
            if (profile.getRadius() != null && profile.getAmount() != null) {
                profile.setApplyUnsharpMask(true);
            } else {
                profile.setApplyUnsharpMask(false);
            }
        }

        // Added since 7.0.0, so older version written yaml needs to stay compatible.
        if (profile.getWienerRepetitions() == 0) {
            profile.setWienerRepetitions(1);
        }
        if (profile.getSaveScale() == 0D) {
            profile.setScale(100D);
        }
    }

    public static boolean validateImageFormat(ImagePlus image, JFrame parentFrame) {
        String message =
                "This file format is not supported. %nYou can only open 16-bit RGB and grayscale PNG and TIFF images.";
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
                LswUtil.delayMacOS();
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

    public static Pair<ImagePlus, Boolean> openImage(
            String filepath, OpenImageModeEnum openImageMode, double scale, UnaryOperator<ImagePlus> scaler) {
        return openImage(filepath, openImageMode, null, null, scale, scaler, null);
    }

    public static Pair<ImagePlus, Boolean> openImage(
            String filepath,
            OpenImageModeEnum openImageMode,
            ImagePlus currentImage,
            LswImageLayers currentUnprocessedImageLayers,
            double scale,
            UnaryOperator<ImagePlus> scaler,
            JFrame parentFrame) {
        ImagePlus newImage = new Opener().openImage(LswFileUtil.getIJFileFormat(filepath));
        if (parentFrame != null && !validateImageFormat(newImage, parentFrame)) {
            throw new NotARawImageException("Invalid image format");
        }

        if (scale > 1.0) {
            newImage = scaler.apply(newImage);
        }
        LswImageLayers unprocessedNewImageLayers = LswImageProcessingUtil.getImageLayers(newImage);
        boolean includeRed = openImageMode == OpenImageModeEnum.RED || openImageMode == OpenImageModeEnum.RGB;
        boolean includeGreen = openImageMode == OpenImageModeEnum.GREEN || openImageMode == OpenImageModeEnum.RGB;
        boolean includeBlue = openImageMode == OpenImageModeEnum.BLUE || openImageMode == OpenImageModeEnum.RGB;
        if (newImage.getStackSize() == 3 && includeRed && includeGreen && includeBlue) {
            return Pair.of(newImage, false);
        }

        if (currentImage == null
                || currentImage.getWidth() != newImage.getWidth()
                || currentImage.getHeight() != newImage.getHeight()) {
            return Pair.of(
                    LswImageProcessingUtil.create16BitRGBImage(filepath, unprocessedNewImageLayers),
                    newImage.getStackSize() != 3);
        } else {
            if (currentUnprocessedImageLayers != null) {
                LswImageProcessingUtil.copyLayers(currentUnprocessedImageLayers, currentImage, true, true, true);
            }
            if (includeRed) {
                LswImageProcessingUtil.copyLayer(unprocessedNewImageLayers, currentImage, 1);
            }
            if (includeGreen) {
                LswImageProcessingUtil.copyLayer(unprocessedNewImageLayers, currentImage, 2);
            }
            if (includeBlue) {
                LswImageProcessingUtil.copyLayer(unprocessedNewImageLayers, currentImage, 3);
            }
            return Pair.of(currentImage, false);
        }
    }

    public static LocalDateTime getObjectDateTime(final String filePath) {
        // Attempt to derive from filename hopefully winjupos formatted (e.g. 2024-04-16-1857_6_...)
        String filename =
                LswFileUtil.getFilenameFromPath(LswFileUtil.getImageName(LswFileUtil.getIJFileFormat(filePath)));
        String[] parts = filename.split("-");
        if (parts[0].length() == 4
                && NumberUtils.isCreatable(parts[0])
                && parts[3].length() >= 4
                && NumberUtils.isCreatable(parts[3].substring(0, 4))) {
            try {
                return LocalDateTime.of(
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]),
                        Integer.parseInt(parts[3].substring(0, 2)),
                        Integer.parseInt(parts[3].substring(2, 4)),
                        0,
                        0);
            } catch (Exception e) {
                log.warn("Unable to parse date/time from filename {}", filename);
            }
        }
        // Use file date as a fallback
        try {
            return LocalDateTime.ofInstant(
                    ((FileTime) Files.getAttribute(Paths.get(filePath), "creationTime")).toInstant(), ZoneOffset.UTC);
        } catch (IOException _) {
            // Or the current date time if nothing else is available
            return LocalDateTime.now();
        }
    }

    public static ImagePlus getWienerDeconvolutionPSF(String profileName) {
        return new Opener()
                .openImage(getDataFolder(LswUtil.getActiveOSProfile()) + "/psf_%s.tif".formatted(profileName));
    }

    public static byte[] getWienerDeconvolutionPSFImage(String profileName) {
        String path = getDataFolder(LswUtil.getActiveOSProfile()) + "/psf_%s.jpg".formatted(profileName);
        // Read the image from file
        try (InputStream in = new FileInputStream(path)) {
            return IOUtils.toByteArray(in);
        } catch (IOException e) {
            log.warn("PSF image not found for profile {}", profileName);
            return null;
        }
    }

    public static void savePSF(ImagePlus image, String profileName) throws IOException {
        String dataFolder = LswFileUtil.getDataFolder(LswUtil.getActiveOSProfile());
        saveImage(image, null, dataFolder + "/psf_%s.tif".formatted(profileName), false, false, TIF, false);
        saveImage(image, null, dataFolder + "/psf_%s.jpg".formatted(profileName), false, false, JPG, false);
    }

    public static void createCleanDirectory(String path) throws IOException {
        try {
            Files.createDirectory(Paths.get(path));
        } catch (FileAlreadyExistsException e) {
            log.info("Directory {} already existed, removing all files from it", path);
            FileUtils.cleanDirectory(new File(path));
        }
    }

    public static ImageOutputFormatType getImageOutputFormat(String extension, FileFilter selectedFormat) {
        if (!StringUtils.isEmpty(extension)) {
            return switch (extension.toLowerCase()) {
                case "jpg", "jpeg" -> JPG;
                case "png" -> PNG;
                case "webp" -> WEBP;
                default -> TIF;
            };
        }
        return switch (selectedFormat.getDescription()) {
            case PNG_OUTPUTFORMAT -> PNG;
            case JPG_OUTPUTFORMAT -> JPG;
            case COMPRESSED_TIF_OUTPUTFORMAT -> CTIF;
            case WEBP_OUTPUTFORMAT -> WEBP;
            case PNG8_OUTPUTFORMAT -> PNG8;
            default -> TIF;
        };
    }

    public static String getImageOutputExtensionForFormat(ImageOutputFormatType type) {
        return switch (type) {
            case PNG,PNG8 -> "png";
            case JPG -> "jpg";
            case WEBP -> "webp";
            default -> "tif";
        };
    }

    private static void hackIncorrectPngFileInfo(LSWFileSaver saver) {
        FileInfo fileInfo = (FileInfo) LswUtil.getPrivateField(saver, LSWFileSaver.class, "fi");
        LswUtil.setPrivateField(fileInfo, FileInfo.class, "fileType", FileInfo.RGB48);
    }
}
