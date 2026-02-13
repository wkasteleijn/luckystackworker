package nl.wilcokas.luckystackworker.service

import bunwarpj.MiscTools
import bunwarpj.bUnwarpJ_
import ij.ImagePlus
import ij.io.Opener
import ij.process.FloatProcessor
import ij.process.ShortProcessor
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JFrame
import javax.swing.JOptionPane
import kotlin.math.abs
import nl.wilcokas.luckystackworker.LuckyStackWorkerContext
import nl.wilcokas.luckystackworker.constants.Constants
import nl.wilcokas.luckystackworker.exceptions.BatchStoppedException
import nl.wilcokas.luckystackworker.exceptions.DeRotationException
import nl.wilcokas.luckystackworker.filter.BilateralDenoiseFilter
import nl.wilcokas.luckystackworker.filter.LSWSharpenFilter
import nl.wilcokas.luckystackworker.filter.settings.LSWSharpenMode
import nl.wilcokas.luckystackworker.model.Profile
import nl.wilcokas.luckystackworker.service.bean.OpenImageModeEnum.RGB
import nl.wilcokas.luckystackworker.util.LswFileUtil
import nl.wilcokas.luckystackworker.util.LswImageProcessingUtil
import nl.wilcokas.luckystackworker.util.LswUtil
import nl.wilcokas.luckystackworker.util.logger
import org.springframework.stereotype.Service

@Service
class DeRotationService(
    private val lswSharpenFilter: LSWSharpenFilter,
    private val bilateralDenoiseFilter: BilateralDenoiseFilter,
    private val luckyStackWorkerContext: LuckyStackWorkerContext,
    private val stackService: StackService,
) {
  private val log by logger()

  private var _anchorStrength: Int = 0
  val anchorStrength: Int
    get() = _anchorStrength

  fun derotate(
      rootFolder: String,
      referenceImageFilename: String,
      allImagesFilenames: List<String>,
      initialAnchorStrength: Int,
      parentFrame: JFrame?,
  ): String? {

    this._anchorStrength = initialAnchorStrength

    luckyStackWorkerContext.totalFilesCount =
        allImagesFilenames.size * 3 +
            1 // 3 steps * nr of files (pre-sharpening, create transformation files, warp. Luminance
    // masks while Stacking
    // count as a single step).
    luckyStackWorkerContext.filesProcessedCount = 0

    val referenceImagePath = "${rootFolder}/${referenceImageFilename}"
    val referenceImage = openImage(referenceImagePath, parentFrame)
    val derotationWorkFolder =
        LswFileUtil.getDataFolder(LswUtil.getActiveOSProfile()) + "/derotation"

    for (run in 1 until 4) {
      LswFileUtil.createCleanDirectory(derotationWorkFolder)
      try {
        val sharpenedImagePaths =
            createPreSharpenedLuminanceCopies(
                rootFolder,
                derotationWorkFolder,
                allImagesFilenames,
                anchorStrength,
                parentFrame,
            )

        val imagesWithTransformation =
            createTransformationFiles(
                sharpenedImagePaths,
                derotationWorkFolder,
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
            parentFrame,
        )

        log.info("Stacking images")
        stackService.stackImages(
            derotationWorkFolder,
            referenceImage.getWidth(),
            referenceImage.getHeight(),
            listOf("${rootFolder}/${referenceImageFilename}") +
                allImagesFilenames
                    .filterNot { it == referenceImageFilename }
                    .map { f -> "${derotationWorkFolder}/D_${f}" }
                    .toList(),
            parentFrame,
        )
        log.info("Done")
        increaseProgressCounter("Stacked images")
        return "${derotationWorkFolder}/${LswFileUtil.getPathWithoutExtension(referenceImageFilename)}_STACK.tif"
      } catch (e: DeRotationException) {
        log.info("DeRotation run ${run} unsuccessful, trying again with adjusted parameters...")
        _anchorStrength = if (_anchorStrength > 1) _anchorStrength - 1 else 1
        increaseProgressCounter("Run ${run} unsuccessful, trying with adjusted parameters")
        luckyStackWorkerContext.filesProcessedCount = 0
      } catch (e: Exception) {
        log.info("DeRotation was stopped with reason: ", e)
        return null
      }
    }
    if (parentFrame != null) {
      JOptionPane.showMessageDialog(
          parentFrame,
          "Derotation failed after 3 runs.\nChoose different values for Noise Robustness, Anchor Strength or Accuracy.",
      )
    }
    return null // last attempt failed
  }

  private fun warpImages(
      referenceImage: ImagePlus,
      derotationWorkFolder: String,
      rootFolder: String,
      imagesWithTransformation: Map<String, String>,
      allImagesFilenames: List<String>,
      referenceImageFilename: String,
      parentFrame: JFrame?,
  ) {
    log.info("Create warped images based on the transformation files")
    val threads = mutableListOf<Thread>()
    val warpingFailed = AtomicBoolean(false)
    val warpingStopped = AtomicBoolean(false)
    for (i in allImagesFilenames.indices) {
      val thread =
          Thread.ofVirtual().start {
            val sourceImageFilename = allImagesFilenames[i]
            if (referenceImageFilename != sourceImageFilename) {
              val sourceImage = openImage("${rootFolder}/${sourceImageFilename}", parentFrame)
              var targetAtReference = false
              var offset = 1
              var transformationReferenceFile = sourceImageFilename
              // Iteratively warp source image towards the reference image by applying the
              // transformation
              // file of each consecutive image until the reference image is reached.
              while (!targetAtReference) {
                if (warpingFailed.get() || warpingStopped.get()) {
                  break
                }
                val targetImageFilename =
                    if (
                        isSourceBeforeReference(
                            sourceImageFilename,
                            referenceImageFilename,
                            allImagesFilenames,
                        )
                    )
                        allImagesFilenames[i + offset]
                    else allImagesFilenames[i - offset]
                if (referenceImageFilename == targetImageFilename) {
                  targetAtReference = true
                }
                val transformationFile = imagesWithTransformation[transformationReferenceFile]!!
                val targetImage =
                    if (targetImageFilename == referenceImageFilename) referenceImage
                    else openImage("${rootFolder}/${targetImageFilename}", parentFrame)
                applyTransformation(sourceImage, targetImage, transformationFile)
                if (!validateTransformationResult(sourceImage, targetImage)) {
                  warpingFailed.set(true)
                  break
                }
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

              LswFileUtil.saveImage(
                  sourceImage,
                  null,
                  "${derotationWorkFolder}/D_${sourceImageFilename}",
                  true,
                  false,
                  false,
                  false,
              )
              try {
                increaseProgressCounter("Warped image ${sourceImageFilename}")
              } catch (e: BatchStoppedException) {
                warpingStopped.set(true)
              }
            }
          }
      threads.add(thread)
    }
    threads.forEach { it.join() }
    if (warpingFailed.get()) {
      throw DeRotationException("Transformation validation failed")
    }
    if (warpingStopped.get()) {
      throw BatchStoppedException("Transformation validation was stopped")
    }
    log.info("Done")
  }

  private fun isSourceBeforeReference(
      sourceImageFilename: String,
      referenceImageFilename: String,
      allImagesFilenames: List<String>,
  ): Boolean {
    val sourceIndex = allImagesFilenames.indexOf(sourceImageFilename)
    val referenceIndex = allImagesFilenames.indexOf(referenceImageFilename)
    return sourceIndex < referenceIndex
  }

  private fun validateTransformationResult(
      sourceImage: ImagePlus,
      targetImage: ImagePlus,
  ): Boolean {
    val sourceProcessor = sourceImage.getProcessor() as ShortProcessor
    val targetProcessor = targetImage.getProcessor() as ShortProcessor
    var totalValueSource: Long = 0
    var totalValueTarget: Long = 0
    var totalPixels = 0
    for (x in 0 until sourceProcessor.width) {
      for (y in 0 until sourceProcessor.height) {
        val sourceValue = sourceProcessor.getPixel(x, y)
        val targetValue = targetProcessor.getPixel(x, y)
        if (
            targetValue > 655
        ) { // exclude dark pixels (less than 1% saturation) from the average calculation
          totalValueSource += sourceValue
          totalValueTarget += targetValue
          totalPixels++
        }
      }
    }
    val averageValueDeviation =
        if (totalPixels > 0) abs(totalValueSource - totalValueTarget) / totalPixels else 0
    log.info("Average value deviation for image ${sourceImage.title} = $averageValueDeviation")
    if (averageValueDeviation > 4096) {
      return false
    }
    log.info("Transformation validation passed for image ${sourceImage.title}")
    return true
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
      allImagesFilenames: List<String>,
      referenceImageFilename: String,
  ): Map<String, String> {
    var referenceEncountered = false
    val imagesWithTransformation: MutableMap<String, String> = ConcurrentHashMap()
    log.info("Create transformation files from copies")
    val semaphore = Semaphore(2)
    val threads = mutableListOf<Thread>()
    val transformationFailed = AtomicBoolean(false)
    val transformationStopped = AtomicBoolean(false)
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
        threads.add(
            startTransformationThread(
                semaphore,
                transformationFailed,
                transformationStopped,
                derotationWorkFolder,
                source,
                target,
                imagesWithTransformation,
                originalSource,
            )
        )
      }
      if (transformationFailed.get() || transformationStopped.get()) {
        break
      }
    }
    threads.forEach { it.join() }
    if (transformationFailed.get()) {
      throw DeRotationException("Transformation creation failed")
    }
    if (transformationStopped.get()) {
      throw BatchStoppedException("Transformation creation stopped")
    }
    log.info("Done")
    return imagesWithTransformation
  }

  private fun startTransformationThread(
      semaphore: Semaphore,
      transformationFailed: AtomicBoolean,
      transformationStopped: AtomicBoolean,
      derotationWorkFolder: String,
      source: String,
      target: String,
      imagesWithTransformation: MutableMap<String, String>,
      originalSource: String,
  ): Thread {
    semaphore.acquire()
    return Thread.ofVirtual().start {
      try {
        if (
            !transformationFailed.get() &&
                !callBunwarpJAlignImages(
                    derotationWorkFolder,
                    source,
                    target,
                    imagesWithTransformation,
                    originalSource,
                )
        ) {
          transformationFailed.set(true)
        }
      } catch (e: BatchStoppedException) {
        transformationStopped.set(true)
      } finally {
        semaphore.release()
      }
    }
  }

  private fun createPreSharpenedLuminanceCopies(
      rootFolder: String,
      derotationWorkFolder: String,
      allImagesFilenames: List<String>,
      anchorStrength: Int,
      parentFrame: JFrame?,
  ): MutableList<String> {
    log.info("Create pre-sharpened luminance copies...")
    val imageDimensions: MutableList<IntArray> = Collections.synchronizedList(ArrayList())
    val sharpenedImagePaths: MutableList<String> = Collections.synchronizedList(ArrayList())
    val threads = mutableListOf<Thread>()
    val imageProcessingStopped = AtomicBoolean(false)
    for (imageFilename in allImagesFilenames) {
      val thread =
          Thread.ofVirtual().start {
            val imagePath = "${rootFolder}/${imageFilename}"
            val image = openImage(imagePath, parentFrame)
            imageDimensions.add(image.dimensions)
            sharpenAsLuminanceImage(image, anchorStrength.toDouble())
            val denoiseProfile = createDenoiseProfile()
            bilateralDenoiseFilter.apply(image, denoiseProfile, false, null)
            val toBeDeRotatedImageFilenameNoExt = LswFileUtil.getFilename(imageFilename)
            val sharpenedImagePath =
                saveToDataFolder(
                    toBeDeRotatedImageFilenameNoExt,
                    image,
                    derotationWorkFolder,
                    imagePath,
                )
            sharpenedImagePaths.add(sharpenedImagePath)
            try {
              increaseProgressCounter("Created de-rotation mask for image ${imageFilename}")
            } catch (e: BatchStoppedException) {
              imageProcessingStopped.set(true)
            }
          }
      threads.add(thread)
      if (imageProcessingStopped.get()) {
        break
      }
    }
    threads.forEach { it.join() }
    validateImageDimensions(imageDimensions, parentFrame)
    if (imageProcessingStopped.get()) {
      throw BatchStoppedException("Image processing stopped")
    }
    log.info("Done")
    return sharpenedImagePaths.sorted().toMutableList()
  }

  private fun createDenoiseProfile(): Profile {
    val profile = Profile()
    profile.denoiseAlgorithm1 = Constants.DENOISE_ALGORITHM_BILATERAL
    profile.bilateralIterations = 5
    profile.bilateralSigmaColor = 1000
    profile.bilateralSigmaColorGreen = 1000
    profile.bilateralSigmaColorBlue = 1000
    profile.bilateralRadius = 2
    profile.bilateralRadiusGreen = 2
    profile.bilateralRadiusBlue = 2
    return profile
  }

  private fun openImage(imagePath: String, parentFrame: JFrame?): ImagePlus {
    return LswFileUtil.openImage(imagePath, RGB, null, null, 1.0, null, parentFrame).left
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
              "All images must have the same dimensions for de-rotation. Expected ${referenceWidth}x${referenceHeight}, but an image with ${width}x${height} was found",
          )
        }
        throw BatchStoppedException("Image dimensions do not match")
      }
    }
  }

  @Synchronized
  private fun increaseProgressCounter(statusMessage: String) {
    log.info(statusMessage)
    luckyStackWorkerContext.filesProcessedCount += 1
    luckyStackWorkerContext.status = statusMessage
    if (luckyStackWorkerContext.isWorkerStopped) {
      luckyStackWorkerContext.isWorkerStopped = false
      log.info("DeRotation was stopped")
      throw BatchStoppedException("DeRotation was stooped")
    }
  }

  private fun callBunwarpJAlignImages(
      folder: String,
      source: String,
      target: String,
      imagesWithTransformation: MutableMap<String, String>,
      sourceFilename: String,
  ): Boolean {
    val args = arrayOfNulls<String>(15)
    args[1] = "${folder}/${source}"
    args[2] = "NULL"
    args[3] = "${folder}/${target}"
    args[4] = "NULL"
    args[5] = "3" // min_scale_deformation
    args[6] = "3" // max_scale_deformation
    args[7] = "0" // max_subsamp_fact
    args[8] = "0.1" // divWeight
    args[9] = "0.1" // curlWeight
    args[10] = "1" // imageWeight
    args[11] = "10" // consistencyWeight
    args[12] = "${folder}/D1_${source}" // output 1
    args[13] = "${folder}/D2_${source}" // output 2
    args[14] = "-save_transformation"

    val method =
        bUnwarpJ_::class.java.getDeclaredMethod("alignImagesCommandLine", Array<String>::class.java)
    method.isAccessible = true
    method.invoke(null, *arrayOf<Any>(args))

    imagesWithTransformation[sourceFilename] =
        folder + "/D2_${LswFileUtil.getFilename(source)}_transf.txt"
    increaseProgressCounter("Created transformation for image $sourceFilename")
    return validateTransformationFile(args[13])
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

  private fun validateTransformationFile(transformationFilePath: String?): Boolean {
    val image = Opener().openImage(LswFileUtil.getIJFileFormat(transformationFilePath))
    val processor = image.getProcessor() as FloatProcessor
    val minAndMax = LswImageProcessingUtil.getMinAndMaxValues(processor)
    var totalValue: Long = 0
    var totalPixels = 0
    for (x in 0 until processor.width) {
      for (y in 0 until processor.height) {
        val value = processor.getf(x, y)
        val shortValue =
            LswImageProcessingUtil.convertToShort(value, minAndMax.getLeft(), minAndMax.getRight())
        val intValue = LswImageProcessingUtil.convertToUnsignedInt(shortValue)
        totalValue += intValue
        totalPixels++
      }
    }
    val averageValue = totalValue / totalPixels
    if (averageValue < 4096) {
      return false
    }
    log.info("Transformation file validation passed for image ${image.title}")
    return true
  }

  private fun sharpenAsLuminanceImage(image: ImagePlus, radius: Double) {
    val stack = image.getStack()
    val ipRed = stack.getProcessor(1)
    val fpRed = ipRed.toFloat(1, null)
    val pixelsRed = fpRed.pixels as FloatArray
    var fpLum: FloatProcessor
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
    ipRed.setPixels(1, fpLum)
  }

  fun transformFromTo(
      rootFolder: String,
      sourceImageFilename: String,
      targetImageFilename: String,
      transformationFile: String,
      factor: Double,
  ) {
    val sourceImagePath = "${rootFolder}/${sourceImageFilename}"
    val targetImagePath = "${rootFolder}/${targetImageFilename}"
    val source = LswFileUtil.openImage(sourceImagePath, RGB, null, null, 1.0, null, null).left
    val target = LswFileUtil.openImage(targetImagePath, RGB, null, null, 1.0, null, null).left

    val intervals = MiscTools.numberOfIntervalsOfTransformation("$rootFolder/$transformationFile")
    val cx = Array<DoubleArray?>(intervals + 3) { DoubleArray(intervals + 3) }
    val cy = Array<DoubleArray?>(intervals + 3) { DoubleArray(intervals + 3) }
    MiscTools.loadTransformation("${rootFolder}/${transformationFile}", cx, cy)

    val initialCx = Array<DoubleArray?>(intervals + 3) { DoubleArray(intervals + 3) }
    val initialCy = Array<DoubleArray?>(intervals + 3) { DoubleArray(intervals + 3) }

    // Manually calculate the initial control point positions
    val xSpacing = source.width.toDouble() / intervals
    val ySpacing = source.height.toDouble() / intervals
    for (i in 0 until intervals + 3) {
      for (j in 0 until intervals + 3) {
        initialCx[i]!![j] = (j - 1) * xSpacing
        initialCy[i]!![j] = (i - 1) * ySpacing
      }
    }

    for (i in cx.indices) {
      for (j in cx[i]!!.indices) {
        // Calculate displacement from the initial control point positions
        val dx = cx[i]!![j] - initialCx[i]!![j]
        val dy = cy[i]!![j] - initialCy[i]!![j]

        // Apply factor to the displacement and add it back to the initial position
        cx[i]!![j] = initialCx[i]!![j] + dx * factor
        cy[i]!![j] = initialCy[i]!![j] + dy * factor
      }
    }

    MiscTools.saveElasticTransformation(
        intervals,
        cx,
        cy,
        "${rootFolder}/OFFSET_${transformationFile}",
    )

    applyTransformation(source, target, "${rootFolder}/OFFSET_${transformationFile}")

    LswFileUtil.saveImage(
        source,
        null,
        "${rootFolder}/OFFSET_${sourceImageFilename}",
        true,
        false,
        false,
        false,
    )
  }
}

fun main(args: Array<String>) {
  try {
    val arguments = Arrays.stream<String>(args).toList()
    if (arguments.size < 4) {
      println(
          "Usage: java DeRotationService -[derotate|transform] <rootFolder> <referenceImageFilename> [<filename1> ... filenameX>|transformationFile] [factor]"
      )
      return
    }

    val luckyStackWorkerContext = LuckyStackWorkerContext()
    val service =
        DeRotationService(
            LSWSharpenFilter(),
            BilateralDenoiseFilter(),
            luckyStackWorkerContext,
            StackService(luckyStackWorkerContext),
        )

    if (arguments[0].equals("-derotate")) {
      service.derotate(
          arguments[1],
          arguments[2],
          arguments.subList(3, arguments.size),
          4,
          null,
      )
    } else if (arguments[0].equals("-transform")) {
      service.transformFromTo(
          arguments[1],
          arguments[2],
          arguments[3],
          arguments[4],
          arguments[5].toDoubleOrNull() ?: 1.0,
      )
    } else {
      println("Invalid command line arguments: ${arguments[0]}")
    }
  } catch (e: Exception) {
    e.printStackTrace()
  }
}
