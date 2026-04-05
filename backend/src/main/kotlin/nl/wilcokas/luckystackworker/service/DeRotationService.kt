package nl.wilcokas.luckystackworker.service

import bunwarpj.MiscTools
import bunwarpj.bUnwarpJ_
import ij.ImagePlus
import ij.io.Opener
import ij.process.FloatProcessor
import ij.process.ShortProcessor
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
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
import nl.wilcokas.luckystackworker.model.ImageOutputFormatType.TIF
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

  private var _noiseRobustness: Int = 0
  val noiseRobustness: Int
    get() = _noiseRobustness

  private var _accurateness: Int = 0
  val accurateness: Int
    get() = _accurateness

  private var _lowSNRData: Boolean = false
  val lowSNRData: Boolean
    get() = _lowSNRData

  fun derotate(
      rootFolder: String,
      referenceImageFilenameParam: String?,
      allImagesFilenames: List<String>,
      initialAnchorStrength: Int,
      initialNoiseRobustness: Int,
      initialAccurateness: Int,
      initialReferenceTime: LocalTime?,
      initiallowSNRData: Boolean,
      parentFrame: JFrame?,
  ): String? {

    this._anchorStrength = initialAnchorStrength
    this._noiseRobustness = initialNoiseRobustness
    this._accurateness = initialAccurateness
    this._lowSNRData = initiallowSNRData

    luckyStackWorkerContext.totalFilesCount =
        allImagesFilenames.size * 3 +
            1 // 3 steps * nr of files (pre-sharpening, create transformation files, warp. Luminance
    // masks while Stacking
    // count as a single step).
    luckyStackWorkerContext.filesProcessedCount = 0

    val result =
        determineReferenceImageFilenames(
            allImagesFilenames,
            referenceImageFilenameParam,
            initialReferenceTime,
        )
    val referenceImageFilenames = result.first
    val referenceTime = result.second

    val derotationWorkFolder =
        LswFileUtil.getDataFolder(LswUtil.getActiveOSProfile()) + "/derotation"

    val referenceInterpolationFactors =
        if (referenceTime == null) null
        else
            calculateReferenceInterpolationFactors(
                referenceImageFilenames,
                allImagesFilenames,
                referenceTime,
            )

    for (run in 1 until 5) {
      LswFileUtil.createCleanDirectory(derotationWorkFolder)
      try {
        val sharpenedImagePaths =
            createPreSharpenedLuminanceCopies(
                rootFolder,
                derotationWorkFolder,
                allImagesFilenames,
                anchorStrength,
                noiseRobustness,
                lowSNRData,
                parentFrame,
            )

        val imagesWithTransformation =
            createTransformationFiles(
                sharpenedImagePaths,
                derotationWorkFolder,
                accurateness,
                allImagesFilenames,
                referenceImageFilenames,
                referenceInterpolationFactors,
            )

        val referenceImage =
            warpImages(
                derotationWorkFolder,
                rootFolder,
                imagesWithTransformation,
                allImagesFilenames,
                referenceImageFilenames,
                parentFrame,
                referenceTime,
            )

        log.info("Stacking images")
        val width = if (referenceImage == null) null else referenceImage.width
        val height = if (referenceImage == null) null else referenceImage.height
        val referenceImagesToStack =
            if (referenceTime == null) listOf("${rootFolder}/${referenceImageFilenameParam}")
            else emptyList()
        val resultFilePath =
            stackService.stackImages(
                rootFolder,
                width,
                height,
                referenceImagesToStack +
                    allImagesFilenames
                        .filterNot { it == referenceImageFilenameParam }
                        .map { f -> "${derotationWorkFolder}/D_${f}" }
                        .toList(),
                parentFrame,
                true,
            )
        log.info("Done")
        increaseProgressCounter("Stacked images")
        return resultFilePath
      } catch (e: DeRotationException) {
        log.info("DeRotation run ${run} unsuccessful, trying again with adjusted parameters...")
        _noiseRobustness = if (_noiseRobustness < 5) _noiseRobustness + 1 else 5
        if (run > 2) {
          _anchorStrength = if (_anchorStrength > 1) _anchorStrength - 1 else 1
        }
        if (run > 1 && _accurateness == 4) {
          _accurateness = 3
        }
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
          "Derotation failed after 4 runs.\nChoose different values for Noise Robustness, Anchor Strength or Accuracy.",
      )
    }
    return null // last attempt failed
  }

  /*
   * If a reference time was provided, the reference images are determined from the list of files based on the winjupos timestamp in the filaname.
   * The 2 references will be the files containing a timestamp that happened after and before the provided reference time.
   */
  private fun determineReferenceImageFilenames(
      allImagesFilenames: List<String>,
      referenceImageFilenameParam: String?,
      referenceTime: LocalTime?,
  ): Pair<Pair<String, String>, LocalDateTime?> {
    if (referenceTime == null) {
      // If this is not time de-rotation then there is only one reference image.
      return Pair(Pair(referenceImageFilenameParam!!, referenceImageFilenameParam), null)
    } else {
      for (i in allImagesFilenames.indices) {
        val imageTime = LswFileUtil.getObjectDateTime(allImagesFilenames[i])
        val referenceDateTime = determineReferenceDateTime(imageTime, referenceTime)
        if (imageTime.isAfter(referenceDateTime)) {
          if (i == 0) {
            throw BatchStoppedException(
                "The provided reference time is before the first image timestamp"
            )
          }
          return Pair(Pair(allImagesFilenames[i - 1], allImagesFilenames[i]), referenceDateTime)
        }
      }
      throw BatchStoppedException("The provided reference time is after the last image timestamp")
    }
  }

  private fun determineReferenceDateTime(
      imageTime: LocalDateTime,
      referenceTime: LocalTime,
  ): LocalDateTime {
    val referenceDate = imageTime.toLocalDate()
    val refDateTimeSameDay = LocalDateTime.of(referenceDate, referenceTime)
    val refDateTimeNextDay = refDateTimeSameDay.plusDays(1)
    val diffSameDay = Duration.between(imageTime, refDateTimeSameDay).abs()
    val diffNextDay = Duration.between(imageTime, refDateTimeNextDay).abs()
    val referenceDateTime =
        if (diffSameDay < diffNextDay) refDateTimeSameDay else refDateTimeNextDay
    return referenceDateTime
  }

  /*
   * The interpolation factors are used to generate the transformation file from the images adjacent to the reference image to the reference image.
   * These factor are only applied when a reference time was provided instead of a reference image.
   * The calculation assumes that all files contain winjupos formatted timestamps and that the provided reference time falls inside the first and the last images timestamps.
   * If not it will throw a BatchStoppedException.
   */
  private fun calculateReferenceInterpolationFactors(
      referenceImageFilenames: Pair<String, String>,
      allImagesFilenames: List<String>,
      referenceTime: LocalDateTime,
  ): Pair<Double, Double> {
    var previousImage: String? = null
    for (i in allImagesFilenames.indices) {
      if (previousImage != null && allImagesFilenames[i] == referenceImageFilenames.second) {
        val previousImageTime = LswFileUtil.getObjectDateTime(previousImage)
        val currentImageTime = LswFileUtil.getObjectDateTime(allImagesFilenames[i])
        val differencePreviousCurrent =
            previousImageTime.until(currentImageTime, ChronoUnit.SECONDS).toDouble()
        val differencePreviousReference =
            previousImageTime.until(referenceTime, ChronoUnit.SECONDS).toDouble()
        val differenceCurrentReference =
            referenceTime.until(currentImageTime, ChronoUnit.SECONDS).toDouble()
        val interpolationFactorBeforeRef = differencePreviousReference / differencePreviousCurrent
        val interpolationFactorAfterRef = differenceCurrentReference / differencePreviousCurrent
        return Pair(interpolationFactorBeforeRef, interpolationFactorAfterRef)
      }
      previousImage = allImagesFilenames[i]
    }
    throw BatchStoppedException("Provided reference files not found: $referenceImageFilenames")
  }

  private fun warpImages(
      derotationWorkFolder: String,
      rootFolder: String,
      imagesWithTransformation: Map<String, String>,
      allImagesFilenamesParam: List<String>,
      referenceImageFilenames: Pair<String, String>,
      parentFrame: JFrame?,
      referenceTime: LocalDateTime?,
  ): ImagePlus? {
    log.info("Create warped images based on the transformation files")

    // Scenario where no reference time is provided
    var referenceImageFilename = referenceImageFilenames.first
    var allImagesFilenames = allImagesFilenamesParam.toMutableList()

    // Scenario where a reference time is provided,  we need to add the interpolated reference image
    // to the list of images at the correct position
    var referenceImage: ImagePlus? = null
    if (referenceTime != null) {
      allImagesFilenames =
          addInterpolatedReferenceImageFilename(allImagesFilenamesParam, referenceTime)
      referenceImageFilename = referenceTime.toString()
    } else {
      // Scenario where no reference time is provided
      referenceImage = openImage("${rootFolder}/${referenceImageFilename}", parentFrame)
    }

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
                applyTransformation(sourceImage, transformationFile)
                // targetImage is null when time based de-rotation is applied since we don't
                // actually have a reference image in that case
                if (
                    targetImage != null && !validateTransformationResult(sourceImage, targetImage)
                ) {
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
                    TIF,
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
                  TIF,
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
    return referenceImage
  }

  private fun addInterpolatedReferenceImageFilename(
      allImagesFilenamesParam: List<String>,
      referenceTime: LocalDateTime,
  ): MutableList<String> {
    val allImagesFilenames = ArrayList<String>()
    var referenceAdded = false
    for (imageFilename in allImagesFilenamesParam) {
      val imageTime = LswFileUtil.getObjectDateTime(imageFilename)
      if (!referenceAdded && imageTime.isAfter(referenceTime)) {
        allImagesFilenames.add(
            referenceTime.toString()
        ) // Add reference time as fake filename, we only need it for comparison
        referenceAdded = true
      }
      allImagesFilenames.add(imageFilename)
    }
    return allImagesFilenames
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
      transformationFile: String,
  ) {
    for (layer in 1..sourceImage.stack.size) {
      val sourceProcessor = sourceImage.getStack().getProcessor(layer).toFloat(1, null)
      val sourceLayerImage = ImagePlus("Layer ${layer}", sourceProcessor)
      bUnwarpJ_.applyTransformToSource(
          transformationFile,
          sourceLayerImage,
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
      referenceImageFilenames: Pair<String, String>,
      referenceInterpolationFactors: Pair<Double, Double>?,
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
      if (!referenceEncountered && referenceImageFilenames.first == originalSource) {
        referenceEncountered = true
      }

      val targetFullPath =
          if (referenceEncountered) sharpenedImagePaths[i - 1] else sharpenedImagePaths[i + 1]
      val target = LswFileUtil.getFilenameFromPath(targetFullPath)
      var factor: Double? = null
      val actualReferenceImageFilename =
          if (referenceEncountered) referenceImageFilenames.first
          else referenceImageFilenames.second
      if (
          target.replace("_sharpened", "") == actualReferenceImageFilename &&
              referenceInterpolationFactors != null
      ) {
        factor =
            if (referenceEncountered) referenceInterpolationFactors.first
            else referenceInterpolationFactors.second
      }
      val isTimeDeRotation = referenceImageFilenames.first != referenceImageFilenames.second
      if (isTimeDeRotation || referenceImageFilenames.first != originalSource) {
        threads.add(
            startTransformationThread(
                semaphore,
                transformationFailed,
                transformationStopped,
                derotationWorkFolder,
                source,
                target,
                accurateness,
                imagesWithTransformation,
                originalSource,
                factor,
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
      accurateness: Int,
      imagesWithTransformation: MutableMap<String, String>,
      originalSource: String,
      factor: Double?,
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
                    accurateness,
                    imagesWithTransformation,
                    originalSource,
                    factor,
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
      noiseRobustness: Int,
      lowSNRData: Boolean,
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
            sharpenAsLuminanceImage(image, anchorStrength.toDouble(), lowSNRData)
            val denoiseProfile = createDenoiseProfile(noiseRobustness)
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

  private fun createDenoiseProfile(noiseRobustness: Int): Profile {
    val profile = Profile()
    profile.denoiseAlgorithm1 = Constants.DENOISE_ALGORITHM_BILATERAL
    profile.bilateralIterations = noiseRobustness
    profile.bilateralIterationsGreen = noiseRobustness
    profile.bilateralIterationsBlue = noiseRobustness
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
      accurateness: Int,
      imagesWithTransformation: MutableMap<String, String>,
      sourceFilename: String,
      factor: Double?,
  ): Boolean {
    val args = arrayOfNulls<String>(15)
    args[1] = "${folder}/${source}"
    args[2] = "NULL"
    args[3] = "${folder}/${target}"
    args[4] = "NULL"
    args[5] = accurateness.toString() // min_scale_deformation
    args[6] = accurateness.toString() // max_scale_deformation
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

    var transformationFile = "D2_${LswFileUtil.getFilename(source)}_transf.txt"
    if (factor != null) {
      val sourceImage = openImage("${folder}/${source}", null)
      transformationFile =
          interpolateNewTransformation(
              folder,
              transformationFile,
              sourceImage.width,
              sourceImage.height,
              factor,
          )
    }

    imagesWithTransformation[sourceFilename] = folder + "/${transformationFile}"
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
        TIF,
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

  private fun sharpenAsLuminanceImage(image: ImagePlus, radius: Double, lowSNRData: Boolean) {
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
    var amount = 0.990f
    if (lowSNRData) {
      amount = 0.945f
    }
    lswSharpenFilter.doUnsharpMask(radius, amount, 0f, fpLum)
    ipGreen.setPixels(2, fpLum)
    ipBlue.setPixels(3, fpLum)
    ipRed.setPixels(1, fpLum)
  }

  fun transformFromTo(
      rootFolder: String,
      sourceImageFilename: String,
      transformationFile: String,
      factor: Double,
  ) {
    val sourceImagePath = "${rootFolder}/${sourceImageFilename}"
    val source = LswFileUtil.openImage(sourceImagePath, RGB, null, null, 1.0, null, null).left

    val interPolatedTransformationFile =
        interpolateNewTransformation(
            rootFolder,
            transformationFile,
            source.width,
            source.height,
            factor,
        )

    applyTransformation(source, interPolatedTransformationFile)

    LswFileUtil.saveImage(
        source,
        null,
        "${rootFolder}/OFFSET_${sourceImageFilename}",
        true,
        false,
        TIF,
        false,
    )
  }

  private fun interpolateNewTransformation(
      rootFolder: String,
      transformationFile: String,
      imageWidth: Int,
      imageHeight: Int,
      factor: Double,
  ): String {
    val intervals = MiscTools.numberOfIntervalsOfTransformation("$rootFolder/$transformationFile")
    val cx = Array<DoubleArray?>(intervals + 3) { DoubleArray(intervals + 3) }
    val cy = Array<DoubleArray?>(intervals + 3) { DoubleArray(intervals + 3) }
    MiscTools.loadTransformation("${rootFolder}/${transformationFile}", cx, cy)

    val initialCx = Array<DoubleArray?>(intervals + 3) { DoubleArray(intervals + 3) }
    val initialCy = Array<DoubleArray?>(intervals + 3) { DoubleArray(intervals + 3) }

    // Manually calculate the initial control point positions
    val xSpacing = imageWidth.toDouble() / intervals
    val ySpacing = imageHeight.toDouble() / intervals
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

    val interpolatedTransformationPath = "${rootFolder}/OFFSET_${transformationFile}"
    MiscTools.saveElasticTransformation(
        intervals,
        cx,
        cy,
        interpolatedTransformationPath,
    )
    return interpolatedTransformationPath
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

    if (arguments[0] == "-derotate") {
      service.derotate(
          arguments[1],
          arguments[2],
          arguments.subList(3, arguments.size),
          4,
          2,
          4,
          LocalTime.now(),
          false,
          null,
      )
    } else if (arguments[0] == "-transform") {
      service.transformFromTo(
          arguments[1],
          arguments[2],
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
