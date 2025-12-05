package nl.wilcokas.luckystackworker.filter

import ij.ImagePlus
import ij.process.ImageProcessor
import nl.wilcokas.luckystackworker.model.ChannelEnum
import nl.wilcokas.luckystackworker.model.ChannelEnum.*
import nl.wilcokas.luckystackworker.model.Profile
import nl.wilcokas.luckystackworker.service.bean.OpenImageModeEnum
import nl.wilcokas.luckystackworker.util.LswFileUtil
import nl.wilcokas.luckystackworker.util.LswImageProcessingUtil
import nl.wilcokas.luckystackworker.util.LswImageProcessingUtil.copyPixelsFromFloatToShortProcessor
import nl.wilcokas.luckystackworker.util.LswUtil
import nl.wilcokas.luckystackworker.util.logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import sc.fiji.TurboReg_
import java.util.function.UnaryOperator

@Component
class DispersionCorrectionFilter : LSWFilter {

    private val log by logger()

    override fun apply(
        image: ImagePlus,
        profile: Profile,
        isMono: Boolean,
        vararg additionalArguments: String
    ): Boolean {
        if (isApplied(profile, image)) {
            log.info("Applying dispersion correction")
            apply(image, profile)
            return true
        }
        return false
    }

    override fun isSlow(): Boolean {
        return false
    }

    override fun isApplied(profile: Profile, image: ImagePlus): Boolean {
        return profile.dispersionCorrectionEnabled && LswImageProcessingUtil.validateRGBStack(image)
    }

    fun apply(image: ImagePlus, profile: Profile) {
        if (profile.automaticDispersionCorrection) {
            automaticallyCorrect(image);
        } else {
            val stack = image.stack
            val ipRed = stack.getProcessor(1)
            val ipBlue = stack.getProcessor(3)
            correctLayer(ipRed, profile.dispersionCorrectionRedX, profile.dispersionCorrectionRedY)
            correctLayer(ipBlue, profile.dispersionCorrectionBlueX, profile.dispersionCorrectionBlueY)
        }
    }

    private fun correctLayer(ip: ImageProcessor, dx: Int, dy: Int) {
        val pixels = ip.pixels as ShortArray
        val pixelsNew = ShortArray(pixels.size)
        val width = ip.width
        val height = ip.height
        val roi = ip.roi

        for (y in roi.y until roi.y + roi.height) {
            var p = width * y + roi.x
            for (x in roi.x until roi.x + roi.width) {
                val xOrg = x - dx
                val yOrg = y - dy

                if (xOrg < 0 || yOrg < 0 || xOrg >= width || yOrg >= height) {
                    pixelsNew[p] = 0
                } else {
                    val pOrg = width * yOrg + xOrg
                    pixelsNew[p] = pixels[pOrg]
                }
                p++
            }
        }

        // Efficiently copy the new array back into the original reference
        pixelsNew.copyInto(pixels)
    }

    internal fun automaticallyCorrect(image: ImagePlus) {
        val stack = image.stack
        val ipRed = stack.getProcessor(1)
        val ipGreen = stack.getProcessor(2)
        val ipBlue = stack.getProcessor(3)
        val width = image.width
        val height = image.height

        // Write R, G and B slices to separate temporary files
        val alignmentWorkFolder = LswFileUtil.getDataFolder(LswUtil.getActiveOSProfile()) + "/alignment"
        LswFileUtil.createDirectory(alignmentWorkFolder)

        val sourceImagePathRed = createTemporaryImageFile(alignmentWorkFolder, ipRed, R)
        val sourceImagePathGreen = createTemporaryImageFile(alignmentWorkFolder, ipGreen, G)
        val sourceImagePathBlue = createTemporaryImageFile(alignmentWorkFolder, ipBlue, B)

        // Align the red and blue to green
        val turboReg = TurboReg_()
        var command = getCommand(width, height, sourceImagePathRed, sourceImagePathGreen)
        log.info("Running TurboReg command: $command")
        turboReg.run(command)
        val transformedImageRed = turboReg.transformedImage
        command = getCommand(width, height, sourceImagePathBlue, sourceImagePathGreen)
        log.info("Running TurboReg command: $command")
        turboReg.run(command)
        val transformedImageBlue = turboReg.transformedImage
        copyPixelsFromFloatToShortProcessor(transformedImageRed.processor, ipRed)
        copyPixelsFromFloatToShortProcessor(transformedImageBlue.processor, ipBlue)
    }

    private fun getCommand(
        width: Int,
        height: Int,
        sourceImagePath: String,
        imagePathTarget: String
    ): String {
        val coordinates = "0 0 ${width} ${height}"
        return "-align -file ${sourceImagePath} ${coordinates} -file ${imagePathTarget} ${coordinates} -translation ${width / 2} ${height / 2} ${width / 2} ${height / 2} -hideOutput"
    }

    private fun createTemporaryImageFile(
        alignmentWorkFolder: String,
        ip: ImageProcessor,
        channel: ChannelEnum
    ): String {
        val sourceImagePath = "${alignmentWorkFolder}/C${channel.name}.tif"
        val sourceImage = ImagePlus("Layer ${channel.name}", ip)
        LswFileUtil.saveImage(sourceImage, null, sourceImagePath, false, false, false, false)
        return sourceImagePath
    }
}

fun main(args: Array<String>) {
    try {
        if (args.isEmpty()) {
            println("Please provide an image path as a command-line argument.")
            return
        }

        val image = LswFileUtil.openImage(
            args[0],
            OpenImageModeEnum.RGB,
            1.0,
            UnaryOperator { img: ImagePlus? -> img })
            .getLeft()

        DispersionCorrectionFilter().automaticallyCorrect(image)

        LswFileUtil.saveImage(
            image, null, args[1], false, false, false, false
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
