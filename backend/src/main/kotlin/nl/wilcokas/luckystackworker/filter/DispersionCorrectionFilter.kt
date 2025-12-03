package nl.wilcokas.luckystackworker.filter

import ij.ImagePlus
import ij.process.ImageProcessor
import nl.wilcokas.luckystackworker.model.Profile
import nl.wilcokas.luckystackworker.util.LswImageProcessingUtil
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class DispersionCorrectionFilter : LSWFilter {

    private val log = LoggerFactory.getLogger(DispersionCorrectionFilter::class.java)

    override fun apply(image: ImagePlus, profile: Profile, isMono: Boolean, vararg additionalArguments: String): Boolean {
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
        val stack = image.stack
        val ipRed = stack.getProcessor(1)
        val ipBlue = stack.getProcessor(3)

        correctLayer(ipRed, profile.dispersionCorrectionRedX, profile.dispersionCorrectionRedY)
        correctLayer(ipBlue, profile.dispersionCorrectionBlueX, profile.dispersionCorrectionBlueY)
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
}