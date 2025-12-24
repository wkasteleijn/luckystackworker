package nl.wilcokas.luckystackworker.service

import bunwarpj.bUnwarpJ_
import ij.ImagePlus
import ij.io.Opener
import ij.process.FloatProcessor
import ij.process.ShortProcessor
import nl.wilcokas.luckystackworker.LuckyStackWorkerContext
import nl.wilcokas.luckystackworker.constants.Constants
import nl.wilcokas.luckystackworker.constants.Constants.STATUS_IDLE
import nl.wilcokas.luckystackworker.exceptions.BatchStoppedException
import nl.wilcokas.luckystackworker.filter.LSWSharpenFilter
import nl.wilcokas.luckystackworker.filter.SavitzkyGolayFilter
import nl.wilcokas.luckystackworker.filter.settings.LSWSharpenMode
import nl.wilcokas.luckystackworker.model.Profile
import nl.wilcokas.luckystackworker.util.LswFileUtil
import nl.wilcokas.luckystackworker.util.LswImageProcessingUtil
import nl.wilcokas.luckystackworker.util.LswUtil
import nl.wilcokas.luckystackworker.util.logger
import org.springframework.stereotype.Service
import java.util.*
import javax.swing.JFrame
import javax.swing.JOptionPane
import kotlin.math.abs

@Service
class DeRotationService(
    private val lswSharpenFilter: LSWSharpenFilter,
    private val savitzkyGolayFilter: SavitzkyGolayFilter,
    private val luckyStackWorkerContext: LuckyStackWorkerContext,
    private val stackService: StackService,
) {
    private val log by logger()

    private var _anchorStrength: Int = 0
    val anchorStrength: Int
        get() = _anchorStrength

    private var _noiseRobustness: Int = 0
    val noiseRobustness: Int
        get() = _noiseRobustness

    private var _accurateness: Int = 0
    val accurateness: Int
        get() = _accurateness

    fun derotate(
        rootFolder: String,
        referenceImageFilename: String,
        allImagesFilenames: List<String>,
        anchorStrength: Int,
        noiseRobustness: Int,
        accurateness: Int,
        parentFrame: JFrame?
    ): String? {

        this._anchorStrength = anchorStrength
        this._noiseRobustness = noiseRobustness
        this._accurateness = accurateness

        luckyStackWorkerContext.totalFilesCount =
            allImagesFilenames.size * 3 +
                    1 // 4 steps * nr of files (pre-sharpening, create transformation files, warp. Stacking
        // counts as a single step).
        luckyStackWorkerContext.filesProcessedCount = 0

        val referenceImagePath = "${rootFolder}/${referenceImageFilename}"
        val referenceImage = Opener().openImage(referenceImagePath)
        val derotationWorkFolder =
            LswFileUtil.getDataFolder(LswUtil.getActiveOSProfile()) + "/derotation"
        LswFileUtil.createCleanDirectory(derotationWorkFolder)

        val sharpenedImagePaths: MutableList<String> = ArrayList<String>()

        try {
            createPreSharpenedLuminanceCopies(
                rootFolder,
                derotationWorkFolder,
                sharpenedImagePaths,
                allImagesFilenames,
                anchorStrength,
                noiseRobustness,
                parentFrame
            )

            val imagesWithTransformation =
                createTransformationFiles(
                    sharpenedImagePaths,
                    derotationWorkFolder,
                    accurateness,
                    allImagesFilenames,
                    referenceImageFilename,
                )

            warpImages(
                referenceImage,
                derotationWorkFolder,
                rootFolder,
                imagesWithTransformation,
                allImagesFilenames,
                referenceImageFilename,
                parentFrame
            )

            log.info("Stacking images")
            stackService.stackImages(
                derotationWorkFolder,
                referenceImage.getWidth(),
                referenceImage.getHeight(),
                listOf("${rootFolder}/${referenceImageFilename}") +
                        allImagesFilenames.stream().map { f -> "${derotationWorkFolder}/D_${f}" }.toList(),
                parentFrame
            )
            log.info("Done")
            increaseProgressCounter("Stacked images")

            return "${derotationWorkFolder}/STACK_$referenceImageFilename"
        } catch (e: BatchStoppedException) {
            log.info("DeRotation was stopped: " + e.message)
            return null
        } finally {
            luckyStackWorkerContext.status = STATUS_IDLE
            luckyStackWorkerContext.filesProcessedCount = 0
            luckyStackWorkerContext.totalFilesCount = 0
            luckyStackWorkerContext.isProfileBeingApplied = false
        }
    }

    private fun warpImages(
        referenceImage: ImagePlus,
        derotationWorkFolder: String,
        rootFolder: String,
        imagesWithTransformation: Map<String, String>,
        allImagesFilenames: List<String>,
        referenceImageFilename: String,
        parentFrame: JFrame?
    ) {
        log.info("Create warped images based on the transformation files")
        var sourceReachedReference = false
        for (i in allImagesFilenames.indices) {
            val sourceImageFilename = allImagesFilenames[i]
            val sourceImage = Opener().openImage("${rootFolder}/${sourceImageFilename}")
            if (referenceImageFilename == sourceImageFilename) {
                sourceReachedReference = true
            } else {
                var targetAtReference = false
                var offset = 1
                var transformationReferenceFile = sourceImageFilename
                // Iteratively warp source image towards the reference image by applying the transformation
                // file of each consecutive image until the reference image is reached.
                while (!targetAtReference) {
                    val targetImageFilename =
                        if (sourceReachedReference) allImagesFilenames[i - offset]
                        else allImagesFilenames[i + offset]
                    if (referenceImageFilename == targetImageFilename) {
                        targetAtReference = true
                    }
                    val transformationFile = imagesWithTransformation[transformationReferenceFile]!!
                    val targetImage =
                        if (targetImageFilename == referenceImageFilename) referenceImage
                        else Opener().openImage("${rootFolder}/${targetImageFilename}")
                    applyTransformation(sourceImage, targetImage, transformationFile)
                    validateTransformationResult(sourceImage, targetImage, parentFrame)
                    sourceImage.updateAndDraw()
                    LswFileUtil.saveImage(
                        sourceImage,
                        null,
                        "${derotationWorkFolder}/D_${sourceImageFilename}",
                        true,
                        false,
                        false,
                        false,
                    )
                    offset++
                    transformationReferenceFile = targetImageFilename
                }
            }
            LswFileUtil.saveImage(
                sourceImage,
                null,
                "${derotationWorkFolder}/D_${sourceImageFilename}",
                true,
                false,
                false,
                false,
            )
            increaseProgressCounter("Warping image ${sourceImageFilename}")
        }
        log.info("Done")
    }

    private fun validateTransformationResult(sourceImage: ImagePlus, targetImage: ImagePlus, parentFrame: JFrame?) {
        val sourceProcessor = sourceImage.getProcessor() as ShortProcessor
        val targetProcessor = targetImage.getProcessor() as ShortProcessor
        var totalValueSource: Long = 0
        var totalValueTarget: Long = 0
        val totalPixels = sourceProcessor.width * sourceProcessor.height
        for (x in 0 until sourceProcessor.width) {
            for (y in 0 until sourceProcessor.height) {
                totalValueSource += sourceProcessor.getPixel(x,y)
                totalValueTarget += targetProcessor.getPixel(x,y)
            }
        }
        val averageValueDeviation = abs(totalValueSource - totalValueTarget) / totalPixels
        if (averageValueDeviation > 4096) { // significant deviation in brightness detected, the transformation must have failed
            if (parentFrame != null) {
                JOptionPane.showMessageDialog(
                    parentFrame,
                    "Image warping failed for image ${sourceImage.title}. Choose different values for Noise Robustness and/or Anchor Strength"
                )
            }
            throw BatchStoppedException("Transformation validation failed")
        }
        log.info("Transformation validation passed")
    }

    private fun applyTransformation(
        sourceImage: ImagePlus,
        targetImage: ImagePlus,
        transformationFile: String,
    ) {
        for (layer in 1..sourceImage.stack.size) {
            val sourceProcessor = sourceImage.getStack().getProcessor(layer).toFloat(1, null)
            val targetProcessor = targetImage.getStack().getProcessor(layer).toFloat(1, null)
            val sourceLayerImage = ImagePlus("Layer ${layer}", sourceProcessor)
            bUnwarpJ_.applyTransformToSource(
                transformationFile,
                ImagePlus("Layer ${layer}", targetProcessor),
                sourceLayerImage,
            )
            copyPixelsFromTo(sourceLayerImage, sourceImage, layer)
        }
    }

    private fun copyPixelsFromTo(fromImage: ImagePlus, toImage: ImagePlus, layer: Int) {
        val fromProcessor = fromImage.getProcessor() as FloatProcessor
        val toProcessor = toImage.getStack().getProcessor(layer) as ShortProcessor
        LswImageProcessingUtil.copyPixelsFromFloatToShortProcessor(fromProcessor, toProcessor)
    }

    private fun createTransformationFiles(
        sharpenedImagePaths: List<String>,
        derotationWorkFolder: String,
        accurateness: Int,
        allImagesFilenames: List<String>,
        referenceImageFilename: String,
    ): Map<String, String> {
        var referenceEncountered = false
        val imagesWithTransformation: MutableMap<String, String> = HashMap<String, String>()
        log.info("Create transformation files from copies")
        for (i in sharpenedImagePaths.indices) {
            val sourceFullPath = sharpenedImagePaths[i]
            val source = LswFileUtil.getFilenameFromPath(sourceFullPath)
            val originalSource = allImagesFilenames[i]
            if (referenceImageFilename == originalSource) {
                referenceEncountered = true
            } else {
                val targetFullPath =
                    if (referenceEncountered) sharpenedImagePaths[i - 1] else sharpenedImagePaths[i + 1]
                val target = LswFileUtil.getFilenameFromPath(targetFullPath)
                val transformationFile =
                    callBunwarpJAlignImages(derotationWorkFolder, source, target, accurateness)
                imagesWithTransformation[allImagesFilenames[i]] = transformationFile
            }
            increaseProgressCounter("Creating transformation file for image $originalSource")
        }
        log.info("Done")
        return imagesWithTransformation
    }

    private fun createPreSharpenedLuminanceCopies(
        rootFolder: String,
        derotationWorkFolder: String,
        sharpenedImagePaths: MutableList<String>,
        allImagesFilenames: List<String>,
        anchorStrength: Int,
        noiseRobustness: Int,
        parentFrame: JFrame?,
    ) {
        log.info("Create pre-sharpened luminance copies...")
        var imageDimensions: MutableList<IntArray> = ArrayList()
        for (imageFilename in allImagesFilenames) {
            val imagePath = "${rootFolder}/${imageFilename}"
            val image = Opener().openImage(imagePath)
            imageDimensions.add(image.dimensions)
            sharpenAsLuminanceImage(image, anchorStrength.toDouble())
            val profile = Profile()
            profile.savitzkyGolayAmount = 100
            profile.savitzkyGolayIterations = noiseRobustness
            profile.savitzkyGolaySize = 3
            profile.savitzkyGolayAmountGreen = 100
            profile.savitzkyGolayIterationsGreen = noiseRobustness
            profile.savitzkyGolaySizeGreen = 3
            profile.savitzkyGolayAmountBlue = 100
            profile.savitzkyGolayIterationsBlue = noiseRobustness
            profile.savitzkyGolaySizeBlue = 3
            profile.denoiseAlgorithm2 = Constants.DENOISE_ALGORITHM_SAVGOLAY
            savitzkyGolayFilter.apply(image, profile, false, null)
            val toBeDeRotatedImageFilenameNoExt = LswFileUtil.getFilename(imageFilename)
            val sharpenedImagePath =
                saveToDataFolder(toBeDeRotatedImageFilenameNoExt, image, derotationWorkFolder, imagePath)
            sharpenedImagePaths.add(sharpenedImagePath)
            increaseProgressCounter("Creating pre-sharpened luminance copy for image $imageFilename")
        }
        validateImageDimensions(imageDimensions, parentFrame)
        log.info("Done")
    }

    private fun validateImageDimensions(imageDimensions: List<IntArray>, parentFrame: JFrame?) {
        val firstDimension = imageDimensions.first()
        val referenceWidth = firstDimension[0]
        val referenceHeight = firstDimension[1]
        imageDimensions.forEach { dimension ->
            val width = dimension[0]
            val height = dimension[1]
            if (width != referenceWidth || height != referenceHeight) {
                if (parentFrame != null) {
                    JOptionPane.showMessageDialog(
                        parentFrame,
                        "All images must have the same dimensions for de-rotation. Expected ${referenceWidth}x${referenceHeight}, but an image with ${width}x${height} was found"
                    )
                }
                throw BatchStoppedException("Image dimensions do not match")
            }
        }
    }

    private fun increaseProgressCounter(statusMessage: String) {
        log.info(statusMessage)
        luckyStackWorkerContext.filesProcessedCount += 1
        luckyStackWorkerContext.status = statusMessage
        if (luckyStackWorkerContext.isWorkerStopped) {
            luckyStackWorkerContext.isWorkerStopped = false
            throw BatchStoppedException("DeRotation was stooped")
        }
    }

    private fun callBunwarpJAlignImages(
        folder: String,
        source: String,
        target: String,
        accurateness: Int,
    ): String {
        val args = arrayOfNulls<String>(15)
        args[1] = "${folder}/${source}"
        args[2] = "NULL"
        args[3] = "${folder}/${target}"
        args[4] = "NULL"
        args[5] = "0" // min_scale_deformation
        args[6] = accurateness.toString() // max_scale_deformation
        args[7] = "0" // max_subsamp_fact
        args[8] = "0" // divWeight
        args[9] = "0" // curlWeight
        args[10] = "1" // imageWeight
        args[11] = "10" // consistencyWeight
        args[12] = "${folder}/D1_${source}" // output 1
        args[13] = "${folder}/D2_${source}" // output 2
        args[14] = "-save_transformation"

        val method =
            bUnwarpJ_::class.java.getDeclaredMethod("alignImagesCommandLine", Array<String>::class.java)
        method.isAccessible = true
        method.invoke(null, *arrayOf<Any>(args))
        return folder + "/D2_${LswFileUtil.getFilename(source)}_transf.txt"
    }

    private fun saveToDataFolder(
        fileNameNoExt: String,
        image: ImagePlus?,
        dataFolder: String,
        imagePath: String,
    ): String {
        val savedFilePath = "${dataFolder}/${fileNameNoExt}_sharpened.tif"
        LswFileUtil.saveImage(
            image,
            null,
            savedFilePath,
            LswFileUtil.isPngRgbStack(image, imagePath),
            false,
            false,
            false,
        )
        return savedFilePath
    }

    private fun sharpenAsLuminanceImage(image: ImagePlus, radius: Double) {
        val stack = image.getStack()
        val ipRed = stack.getProcessor(1)
        val fpRed = ipRed.toFloat(1, null)
        val pixelsRed = fpRed.pixels as FloatArray
        var fpLum: FloatProcessor
        if (stack.size > 1) {
            val ipGreen = stack.getProcessor(2)
            val ipBlue = stack.getProcessor(3)
            val fpGreen = ipGreen.toFloat(2, null)
            val fpBlue = ipBlue.toFloat(3, null)
            val pixelsGreen = fpGreen.pixels as FloatArray
            val pixelsBlue = fpBlue.pixels as FloatArray
            val pixelsLum = FloatArray(pixelsRed.size)
            for (i in pixelsRed.indices) {
                val hsl =
                    LswImageProcessingUtil.rgbToHsl(
                        pixelsRed[i],
                        pixelsGreen[i],
                        pixelsBlue[i],
                        true,
                        true,
                        true,
                        true,
                        LSWSharpenMode.LUMINANCE,
                    )
                pixelsLum[i] = hsl[2]
            }
            fpLum = FloatProcessor(image.getWidth(), image.getHeight(), pixelsLum)
            fpLum.snapshot()
            lswSharpenFilter.doUnsharpMask(radius, 0.990f, 0f, fpLum)
            ipGreen.setPixels(2, fpLum)
            ipBlue.setPixels(3, fpLum)
        } else {
            fpLum = FloatProcessor(image.getWidth(), image.getHeight(), pixelsRed)
            lswSharpenFilter.doUnsharpMask(radius, 0.990f, 0f, fpLum)
        }
        ipRed.setPixels(1, fpLum)
    }
}

fun main(args: Array<String>) {
    try {
        val arguments = Arrays.stream<String>(args).toList()
        if (arguments.size < 3) {
            println(
                "Usage: java DeRotationService <rootFolder> <referenceImageFilename> <filename1> ... filenameX>"
            )
            return
        }

        val luckyStackWorkerContext = LuckyStackWorkerContext()
        DeRotationService(
            LSWSharpenFilter(),
            SavitzkyGolayFilter(),
            luckyStackWorkerContext,
            StackService(luckyStackWorkerContext),
        )
            .derotate(arguments[0], arguments[1], arguments.subList(2, arguments.size), 4, 2, 4, null)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
