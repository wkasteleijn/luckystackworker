package nl.wilcokas.luckystackworker.service

import ij.ImagePlus
import ij.io.Opener
import nl.wilcokas.luckystackworker.LuckyStackWorkerContext
import nl.wilcokas.luckystackworker.constants.Constants.STATUS_IDLE
import nl.wilcokas.luckystackworker.exceptions.BatchStoppedException
import nl.wilcokas.luckystackworker.service.bean.LswImageLayers
import nl.wilcokas.luckystackworker.util.LswFileUtil
import nl.wilcokas.luckystackworker.util.LswImageProcessingUtil
import nl.wilcokas.luckystackworker.util.logger
import org.springframework.stereotype.Service
import javax.swing.JFrame
import javax.swing.JOptionPane

@Service
class StackService(private val luckyStackWorkerContext: LuckyStackWorkerContext) {

    private val log by logger()

    fun stackImages(
        workFolder: String,
        width: Int,
        height: Int,
        imagesFilePaths: List<String>,
        parentFrame: JFrame?,
    ): String {
        try {
            luckyStackWorkerContext.totalFilesCount = imagesFilePaths.size
            luckyStackWorkerContext.filesProcessedCount = 0

            val redPixels = LongArray(width * height)
            val greenPixels = LongArray(width * height)
            val bluePixels = LongArray(width * height)
            for (i in imagesFilePaths.indices) {
                val image = Opener().openImage(imagesFilePaths[i])
                validateImageDimensions(image, width, height, parentFrame)
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
                increaseProgressCounter(
                    "Stacking image ${LswFileUtil.getFilenameFromPath(imagesFilePaths[i])}"
                )
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
            val stackedImage =
                LswImageProcessingUtil.create16BitRGBImage(
                    "${workFolder}/STACK_${referenceImageFilename}",
                    lswImageLayers,
                    true,
                    true,
                    true,
                )
            val stackedImagePath = "${workFolder}/STACK_${referenceImageFilename}"
            LswFileUtil.saveImage(stackedImage, null, stackedImagePath, true, false, false, false)
            return stackedImagePath
        } finally {
            luckyStackWorkerContext.status = STATUS_IDLE
            luckyStackWorkerContext.filesProcessedCount = 0
            luckyStackWorkerContext.totalFilesCount = 0
            luckyStackWorkerContext.isProfileBeingApplied = false
        }
    }

    private fun validateImageDimensions(image: ImagePlus, width: Int, height: Int, parentFrame: JFrame?) {
        if (image.width != width || image.height != height) {
            JOptionPane.showMessageDialog(
                parentFrame,
                "Image ${image.title} could not be stacked as its dimensions do not match"
            )
            throw BatchStoppedException("Image dimensions do not match")
        }
    }

    private fun increaseProgressCounter(statusMessage: String) {
        log.info(statusMessage)
        luckyStackWorkerContext.filesProcessedCount += 1
        luckyStackWorkerContext.status = statusMessage
        if (luckyStackWorkerContext.isWorkerStopped) {
            luckyStackWorkerContext.isWorkerStopped = false
            throw BatchStoppedException("Stacking was stooped")
        }
    }
}
