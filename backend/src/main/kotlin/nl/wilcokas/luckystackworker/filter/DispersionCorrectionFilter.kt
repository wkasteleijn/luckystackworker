package nl.wilcokas.luckystackworker.filter

import ij.ImagePlus
import ij.process.ImageProcessor
import nl.wilcokas.luckystackworker.model.ChannelEnum
import nl.wilcokas.luckystackworker.model.ChannelEnum.*
import nl.wilcokas.luckystackworker.model.Profile
import nl.wilcokas.luckystackworker.service.bean.OpenImageModeEnum
import nl.wilcokas.luckystackworker.util.LswFileUtil
import nl.wilcokas.luckystackworker.util.LswImageProcessingUtil
import nl.wilcokas.luckystackworker.util.LswUtil
import nl.wilcokas.luckystackworker.util.logger
import org.springframework.stereotype.Component
import sc.fiji.TurboReg_
import java.util.function.UnaryOperator
import kotlin.math.roundToInt

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

    private fun apply(image: ImagePlus, profile: Profile) {
        val stack = image.stack
        val ipRed = stack.getProcessor(1)
        val ipBlue = stack.getProcessor(3)
        if (!isManuallyCorrected(profile)) {
            determineCorrectionAutomatically(image, profile)
        }
        correctLayer(ipRed, profile.dispersionCorrectionRedX, profile.dispersionCorrectionRedY)
        correctLayer(ipBlue, profile.dispersionCorrectionBlueX, profile.dispersionCorrectionBlueY)
    }

    private fun isManuallyCorrected(profile: Profile): Boolean {
        return profile.dispersionCorrectionRedX!= 0.0 || profile.dispersionCorrectionRedY!= 0.0 ||
                profile.dispersionCorrectionBlueX!= 0.0 || profile.dispersionCorrectionBlueY!= 0.0
    }

    private fun correctLayer(ip: ImageProcessor, dx: Double, dy: Double) {
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

                val x1 = xOrg.toInt()
                val y1 = yOrg.toInt()
                val x2 = x1 + 1
                val y2 = y1 + 1

                if (x1 < 0 || y1 < 0 || x2 >= width || y2 >= height) {
                    pixelsNew[p] = 0
                } else {
                    val xFraction = xOrg - x1
                    val yFraction = yOrg - y1

                    val p1 = pixels[width * y1 + x1].toInt() and 0xFFFF
                    val p2 = pixels[width * y1 + x2].toInt() and 0xFFFF
                    val p3 = pixels[width * y2 + x1].toInt() and 0xFFFF
                    val p4 = pixels[width * y2 + x2].toInt() and 0xFFFF

                    val z1 = p1 * (1 - xFraction) + p2 * xFraction
                    val z2 = p3 * (1 - xFraction) + p4 * xFraction
                    val interpolatedValue = z1 * (1 - yFraction) + z2 * yFraction

                    pixelsNew[p] = interpolatedValue.toInt().toShort()
                }
                p++
            }
        }
        pixelsNew.copyInto(pixels)
    }

    internal fun determineCorrectionAutomatically(image: ImagePlus, profile: Profile) {
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
        updateDispersionCorrection(turboReg, profile, R)
        command = getCommand(width, height, sourceImagePathBlue, sourceImagePathGreen)
        log.info("Running TurboReg command: $command")
        turboReg.run(command)
        updateDispersionCorrection(turboReg, profile, B)
    }

    private fun updateDispersionCorrection(turboReg: TurboReg_, profile: Profile, channel: ChannelEnum) {
        val dx = ((turboReg.sourcePoints[0][0] - turboReg.targetPoints[0][0]) * 10).roundToInt() / 10.0
        val dy = ((turboReg.sourcePoints[0][1] - turboReg.targetPoints[0][1]) * 10).roundToInt() / 10.0
        when (channel) {
            R -> {
                profile.dispersionCorrectionRedX -= dx
                profile.dispersionCorrectionRedY -= dy
            }
            B -> {
                profile.dispersionCorrectionBlueX -= dx
                profile.dispersionCorrectionBlueY -= dy
            }
            else -> throw IllegalArgumentException("Unsupported channel: $channel")
        }
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

        DispersionCorrectionFilter().determineCorrectionAutomatically(image, Profile())

        LswFileUtil.saveImage(
            image, null, args[1], false, false, false, false
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
