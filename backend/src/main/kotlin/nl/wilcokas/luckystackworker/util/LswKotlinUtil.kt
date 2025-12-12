package nl.wilcokas.luckystackworker.util

import ij.io.Opener
import nl.wilcokas.luckystackworker.service.bean.LswImageLayers
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun <T : Any> T.logger(): Lazy<Logger> {
    return lazy { LoggerFactory.getLogger(this.javaClass) }
}

fun stackImages(
    workFolder: String,
    width: Int,
    height: Int,
    imagesFilePaths: List<String>
) : String {
    val redPixels = LongArray(width * height)
    val greenPixels = LongArray(width * height)
    val bluePixels = LongArray(width * height)
    for (i in imagesFilePaths.indices) {
        val image = Opener().openImage(imagesFilePaths[i])
        val redIP = image.getStack().getProcessor(1)
        val greenLayerIndex = if (image.stackSize == 1) 1 else 2
        val blueLayerIndex = if (image.stackSize == 1) 1 else 3
        val greenIP = image.getStack().getProcessor(greenLayerIndex)
        val blueIP = image.getStack().getProcessor(blueLayerIndex)
        for (y in 0..<height) {
            for (x in 0..<width) {
                val index = y * width + x
                redPixels[index] += redIP.getPixel(x, y).toLong()
                greenPixels[index] += greenIP.getPixel(x, y).toLong()
                bluePixels[index] += blueIP.getPixel(x, y).toLong()
            }
        }
    }
    val redPixelsAverages = ShortArray(redPixels.size)
    val greenPixelsAverages = ShortArray(greenPixels.size)
    val bluePixelsAverages = ShortArray(bluePixels.size)
    for (i in redPixels.indices) {
        redPixelsAverages[i] = (redPixels[i] / imagesFilePaths.size).toShort()
        greenPixelsAverages[i] = (greenPixels[i] / imagesFilePaths.size).toShort()
        bluePixelsAverages[i] = (bluePixels[i] / imagesFilePaths.size).toShort()
    }
    val layers = arrayOf(redPixelsAverages, greenPixelsAverages, bluePixelsAverages)
    val lswImageLayers = LswImageLayers(width, height, layers)
    val referenceImageFilename = LswFileUtil.getFilenameFromPath(imagesFilePaths[0])
    val stackedImage = LswImageProcessingUtil.create16BitRGBImage(
        "${workFolder}/STACK_${referenceImageFilename}", lswImageLayers, true, true, true
    )
    val stackedImagePath = "${workFolder}/STACK_${referenceImageFilename}";
    LswFileUtil.saveImage(
        stackedImage,
        null,
        stackedImagePath,
        true,
        false,
        false,
        false
    )
    return stackedImagePath
}