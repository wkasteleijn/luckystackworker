package nl.wilcokas.luckystackworker.service;

import bunwarpj.bUnwarpJ_;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.Opener;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.LuckyStackWorkerContext;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.exceptions.DeRotationStoppedException;
import nl.wilcokas.luckystackworker.filter.LSWSharpenFilter;
import nl.wilcokas.luckystackworker.filter.SavitzkyGolayFilter;
import nl.wilcokas.luckystackworker.filter.settings.LSWSharpenMode;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.service.bean.LswImageLayers;
import nl.wilcokas.luckystackworker.util.LswFileUtil;
import nl.wilcokas.luckystackworker.util.LswImageProcessingUtil;
import nl.wilcokas.luckystackworker.util.LswUtil;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class DeRotationService {

    private final LSWSharpenFilter lswSharpenFilter;
    private final SavitzkyGolayFilter savitzkyGolayFilter;
    private final LuckyStackWorkerContext luckyStackWorkerContext;

    @Getter
    private List<String> allImagesFilenames;

    @Getter
    private String referenceImageFilename;

    @Getter
    private int anchorStrength;

    @Getter
    private int noiseRobustness;

    @Getter
    private int accurateness;

    public Optional<String> derotate(
            final String rootFolder,
            final String referenceImageFilename,
            final List<String> allImagesFilenames,
            final int anchorStrength,
            final int noiseRobustness,
            final int accurateness)
            throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {

        this.allImagesFilenames = allImagesFilenames;
        this.referenceImageFilename = referenceImageFilename;
        this.anchorStrength = anchorStrength;
        this.noiseRobustness = noiseRobustness;
        this.accurateness = accurateness;

        luckyStackWorkerContext.setTotalFilesCount(allImagesFilenames.size()
                * 4); // 4 steps * nr of files (pre-sharpening, create transformation files, warp
        // images, stack images)
        luckyStackWorkerContext.setFilesProcessedCount(0);

        String referenceImagePath = rootFolder + "/" + referenceImageFilename;
        ImagePlus referenceImage = new Opener().openImage(referenceImagePath);
        String derotationWorkFolder = LswFileUtil.getDataFolder(LswUtil.getActiveOSProfile()) + "/derotation";
        try {
            Files.createDirectory(Paths.get(derotationWorkFolder));
        } catch (FileAlreadyExistsException e) {
            log.warn("Derotation work folder already existed!");
        }

        List<String> sharpenedImagePaths = new ArrayList<>();

        try {
            createPreSharpenedLuminanceCopies(rootFolder, derotationWorkFolder, sharpenedImagePaths);

            final Map<String, String> imagesWithTransformation = createTransformationFiles(sharpenedImagePaths, derotationWorkFolder, accurateness);

            warpImages(referenceImage, derotationWorkFolder, rootFolder, imagesWithTransformation);

            stackImages(rootFolder, derotationWorkFolder, referenceImage.getWidth(), referenceImage.getHeight());

            return Optional.of(derotationWorkFolder + "/STACK_" + referenceImageFilename);

        } catch (DeRotationStoppedException e) {
            log.info("DeRotation was stopped: " + e.getMessage());
            return Optional.empty();
        } finally {
            luckyStackWorkerContext.setStatus(Constants.STATUS_IDLE);
            luckyStackWorkerContext.setFilesProcessedCount(0);
            luckyStackWorkerContext.setTotalFilesCount(0);
            luckyStackWorkerContext.setProfileBeingApplied(false);
        }
    }

    public void removeDerotationWorkFolder() throws IOException {
        String derotationWorkFolder = LswFileUtil.getDataFolder(LswUtil.getActiveOSProfile()) + "/derotation";
        FileUtils.deleteDirectory(new File(derotationWorkFolder));
    }

    private void warpImages(final ImagePlus referenceImage, final String derotationWorkFolder, final String rootFolder,
                            final Map<String, String> imagesWithTransformation) throws IOException {
        log.info("Create warped images based on the transformation files");
        boolean sourceReachedReference = false;
        for (int i = 0; i < allImagesFilenames.size(); i++) {
            String sourceImageFilename = allImagesFilenames.get(i);
            ImagePlus sourceImage = new Opener().openImage(rootFolder + "/" + sourceImageFilename);
            if (referenceImageFilename.equals(sourceImageFilename)) {
                sourceReachedReference = true;
            } else {
                boolean targetAtReference = false;
                int offset = 1;
                String transformationReferenceFile = sourceImageFilename;
                // Iteratively warp source image towards the reference image by applying the transformation
                // file of each consecutive image until the reference image is reached.
                while (!targetAtReference) {
                    String targetImageFilename = sourceReachedReference
                            ? allImagesFilenames.get(i - offset)
                            : allImagesFilenames.get(i + offset);
                    if (referenceImageFilename.equals(targetImageFilename)) {
                        targetAtReference = true;
                    }
                    String transformationFile = imagesWithTransformation.get(transformationReferenceFile);
                    ImagePlus targetImage = targetImageFilename.equals(referenceImageFilename)
                            ? referenceImage
                            : new Opener().openImage(rootFolder + "/" + targetImageFilename);
                    applyTransformation(sourceImage, targetImage, transformationFile);
                    sourceImage.updateAndDraw();
                    LswFileUtil.saveImage(sourceImage, null, derotationWorkFolder + "/D_" + sourceImageFilename, true, false, false, false);
                    offset++;
                    transformationReferenceFile = targetImageFilename;
                }
            }
            LswFileUtil.saveImage(sourceImage, null, derotationWorkFolder + "/D_" + sourceImageFilename, true, false, false, false);
            increaseProgressCounter("Warping image %s".formatted(sourceImageFilename));
        }
        log.info("Done");
    }

    private void applyTransformation(ImagePlus sourceImage, ImagePlus targetImage, String transformationFile) {
        for (int layer = 1; layer <= 3; layer++) {
            FloatProcessor sourceProcessor =
                    sourceImage.getStack().getProcessor(layer).toFloat(1, null);
            FloatProcessor targetProcessor =
                    targetImage.getStack().getProcessor(layer).toFloat(1, null);
            final ImagePlus sourceLayerImage = new ImagePlus("Layer " + layer, sourceProcessor);
            bUnwarpJ_.applyTransformToSource(
                    transformationFile, new ImagePlus("Layer " + layer, targetProcessor), sourceLayerImage);
            copyPixelsFromTo(sourceLayerImage, sourceImage, layer);
        }
    }

    private void stackImages(final String rootFolder, final String derotationWorkFolder, int width, int height) throws IOException {
        log.info("Stacking warped images");
        long[] redPixels = new long[width * height];
        long[] greenPixels = new long[width * height];
        long[] bluePixels = new long[width * height];
        for (int i = 0; i < allImagesFilenames.size(); i++) {
            String imageFilename = allImagesFilenames.get(i);
            ImagePlus image;
            if (imageFilename.equals(referenceImageFilename)) {
                image = new Opener().openImage(rootFolder + "/" + referenceImageFilename);
            } else {
                image = new Opener().openImage(derotationWorkFolder + "/D_" + imageFilename);
            }
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int index = y * width + x;
                    redPixels[index] += image.getStack().getProcessor(1).getPixel(x, y);
                    greenPixels[index] += image.getStack().getProcessor(2).getPixel(x, y);
                    bluePixels[index] += image.getStack().getProcessor(3).getPixel(x, y);
                }
            }
            increaseProgressCounter("Stacking image %s".formatted(allImagesFilenames.get(i)));
        }
        short[] redPixelsAverages = new short[redPixels.length];
        short[] greenPixelsAverages = new short[greenPixels.length];
        short[] bluePixelsAverages = new short[bluePixels.length];
        for (int i = 0; i < redPixels.length; i++) {
            redPixelsAverages[i] = (short) (redPixels[i] / allImagesFilenames.size());
            greenPixelsAverages[i] = (short) (greenPixels[i] / allImagesFilenames.size());
            bluePixelsAverages[i] = (short) (bluePixels[i] / allImagesFilenames.size());
        }
        short[][] layers = new short[][] {redPixelsAverages, greenPixelsAverages, bluePixelsAverages};
        LswImageLayers lswImageLayers = LswImageLayers.builder()
                .width(width)
                .height(height)
                .layers(layers)
                .build();
        ImagePlus stackedImage = LswImageProcessingUtil.create16BitRGBImage(derotationWorkFolder + "/STACK_" + referenceImageFilename, lswImageLayers, true, true, true);
        LswFileUtil.saveImage(stackedImage, null, derotationWorkFolder + "/STACK_" + referenceImageFilename, true, false, false, false);
        log.info("Done");
    }

    private void copyPixelsFromTo(final ImagePlus fromImage, ImagePlus toImage, int layer) {
        FloatProcessor fromProcessor = (FloatProcessor) fromImage.getProcessor();
        ShortProcessor toProcessor = (ShortProcessor) toImage.getStack().getProcessor(layer);
        float[] fromPixels = (float[]) fromProcessor.getPixels();
        short[] toPixels = (short[]) toProcessor.getPixels();
        for (int i = 0; i < fromPixels.length; i++) {
            float value = fromPixels[i] + 0.5f;
            if (value < 0f) value = 0f;
            if (value > 65535f) value = 65535f;
            toPixels[i] = (short) value;
        }
    }

    private Map<String, String> createTransformationFiles(List<String> sharpenedImagePaths, String derotationWorkFolder, int accurateness) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        boolean referenceEncountered = false;
        Map<String, String> imagesWithTransformation = new HashMap<>();
        log.info("Create transformation files from copies");
        for (int i = 0; i < sharpenedImagePaths.size(); i++) {
            String sourceFullPath = sharpenedImagePaths.get(i);
            String source = LswFileUtil.getFilenameFromPath(sourceFullPath);
            String originalSource = allImagesFilenames.get(i);
            if (referenceImageFilename.equals(originalSource)) {
                referenceEncountered = true;
            } else {
                String targetFullPath =
                        referenceEncountered ? sharpenedImagePaths.get(i - 1) : sharpenedImagePaths.get(i + 1);
                String target = LswFileUtil.getFilenameFromPath(targetFullPath);
                String transformationFile = callBunwarpJAlignImages(derotationWorkFolder, source, target, accurateness);
                imagesWithTransformation.put(allImagesFilenames.get(i), transformationFile);
            }
            increaseProgressCounter("Creating transformation file for image %s".formatted(originalSource));
        }
        log.info("Done");
        return imagesWithTransformation;
    }

    private void createPreSharpenedLuminanceCopies(String rootFolder, String derotationWorkFolder, List<String> sharpenedImagePaths) throws IOException {
        log.info("Create pre-sharpened luminance copies...");
        for (String imageFilename : allImagesFilenames) {
            String imagePath = rootFolder + "/" + imageFilename;
            ImagePlus image = new Opener().openImage(imagePath);
            sharpenAsLuminanceImage(image, anchorStrength);
            Profile profile = Profile.builder()
                    .savitzkyGolayAmount(100)
                    .savitzkyGolayIterations(noiseRobustness)
                    .savitzkyGolaySize(3)
                    .savitzkyGolayAmountGreen(100)
                    .savitzkyGolayIterationsGreen(noiseRobustness)
                    .savitzkyGolaySizeGreen(3)
                    .savitzkyGolayAmountBlue(100)
                    .savitzkyGolayIterationsBlue(noiseRobustness)
                    .savitzkyGolaySizeBlue(3)
                    .denoiseAlgorithm2(Constants.DENOISE_ALGORITHM_SAVGOLAY)
                    .build();
            savitzkyGolayFilter.apply(image, profile, false, null);
            String toBeDeRotatedImageFilenameNoExt = LswFileUtil.getFilename(imageFilename);
            String sharpenedImagePath =
                    saveToDataFolder(
                            toBeDeRotatedImageFilenameNoExt,
                            image,
                            derotationWorkFolder,
                            imagePath);
            sharpenedImagePaths.add(sharpenedImagePath);
            increaseProgressCounter("Creating pre-sharpened luminance copy for image %s".formatted(imageFilename));
        }
        log.info("Done");
    }

    private void increaseProgressCounter(String statusMessage) {
        log.info(statusMessage);
        luckyStackWorkerContext.setFilesProcessedCount(luckyStackWorkerContext.getFilesProcessedCount() + 1);
        luckyStackWorkerContext.setStatus(statusMessage);
        if (luckyStackWorkerContext.isWorkerStopped()) {
            luckyStackWorkerContext.setWorkerStopped(false);
            throw new DeRotationStoppedException("DeRotation was stooped");
        }
    }

    private String callBunwarpJAlignImages(String folder, String source, String target, int accurateness)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String[] args = new String[15];
        args[1] = folder + "/" + source;
        args[2] = "NULL";
        args[3] = folder + "/" + target;
        args[4] = "NULL";
        args[5] = "0"; // min_scale_deformation
        args[6] = Integer.toString(accurateness); // max_scale_deformation
        args[7] = "0"; // max_subsamp_fact
        args[8] = "0"; // divWeight
        args[9] = "0"; // curlWeight
        args[10] = "1"; // imageWeight
        args[11] = "10"; // consistencyWeight
        args[12] = folder + "/D1_" + source; // output 1
        args[13] = folder + "/D2_" + source; // output 2
        args[14] = "-save_transformation";

        Method method = bUnwarpJ_.class.getDeclaredMethod("alignImagesCommandLine", String[].class);
        method.setAccessible(true);
        method.invoke(null, new Object[] {args});
        return folder + "/D2_" + LswFileUtil.getFilename(source) + "_transf.txt";
    }

    private String saveToDataFolder(String fileNameNoExt, ImagePlus image, String dataFolder, String imagePath)
            throws IOException {
        String savedFilePath = dataFolder + "/" + fileNameNoExt + "_sharpened.tif";
        LswFileUtil.saveImage(
                image, null, savedFilePath, LswFileUtil.isPngRgbStack(image, imagePath), false, false, false);
        return savedFilePath;
    }

    private void sharpenAsLuminanceImage(ImagePlus image, double radius) {
        ImageStack stack = image.getStack();

        ImageProcessor ipRed = stack.getProcessor(1);
        ImageProcessor ipGreen = stack.getProcessor(2);
        ImageProcessor ipBlue = stack.getProcessor(3);

        FloatProcessor fpRed = ipRed.toFloat(1, null);
        FloatProcessor fpGreen = ipGreen.toFloat(2, null);
        FloatProcessor fpBlue = ipBlue.toFloat(3, null);
        float[] pixelsRed = (float[]) fpRed.getPixels();
        float[] pixelsGreen = (float[]) fpGreen.getPixels();
        float[] pixelsBlue = (float[]) fpBlue.getPixels();

        float[] pixelsLum = new float[pixelsRed.length];
        for (int i = 0; i < pixelsRed.length; i++) {
            float[] hsl = LswImageProcessingUtil.rgbToHsl(
                    pixelsRed[i], pixelsGreen[i], pixelsBlue[i], true, true, true, true, LSWSharpenMode.LUMINANCE);
            pixelsLum[i] = hsl[2];
        }
        FloatProcessor fpLum = new FloatProcessor(image.getWidth(), image.getHeight(), pixelsLum);
        fpLum.snapshot();

        lswSharpenFilter.doUnsharpMask(radius, 0.990F, 0, fpLum);

        ipRed.setPixels(1, fpLum);
        ipGreen.setPixels(2, fpLum);
        ipBlue.setPixels(3, fpLum);
    }

    public static void main(String[] args) {
        try {
            List<String> arguments = Arrays.stream(args).toList();
            if (arguments.size() < 3) {
                log.error(
                        "Usage: java DeRotationService <rootFolder> <referenceImageFilename> <filename1> ... filenameX>");
                return;
            }

            new DeRotationService(new LSWSharpenFilter(), new SavitzkyGolayFilter(), new LuckyStackWorkerContext())
                    .derotate(arguments.get(0), arguments.get(1), arguments.subList(2, arguments.size()), 4, 2, 4);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException | IOException e) {
            log.error("Error:", e);
        }
    }
}
