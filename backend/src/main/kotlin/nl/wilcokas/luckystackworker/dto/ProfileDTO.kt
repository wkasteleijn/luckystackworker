package nl.wilcokas.luckystackworker.dto

import nl.wilcokas.luckystackworker.model.ChannelEnum
import nl.wilcokas.luckystackworker.model.Profile
import nl.wilcokas.luckystackworker.service.bean.OpenImageModeEnum
import java.math.BigDecimal

data class ProfileDTO(
    var name: String? = null,

    // sharpen
    var radius: BigDecimal? = null,
    var amount: BigDecimal? = null,
    var iterations: Int = 0,
    var level: Int = 0,
    var clippingStrength: Int = 0,
    var clippingRange: Int = 0,
    var deringRadius: BigDecimal? = null,
    var deringStrength: Int = 0,
    var blendRaw: Int = 0,
    var wienerIterations: Int = 0,
    var wienerRepetitions: Int = 0,
    var clippingSuppression: Double = 0.0,

    var radiusGreen: BigDecimal? = null,
    var amountGreen: BigDecimal? = null,
    var iterationsGreen: Int = 0,
    var levelGreen: Int = 0,
    var clippingStrengthGreen: Int = 0,
    var clippingRangeGreen: Int = 0,
    var deringRadiusGreen: BigDecimal? = null,
    var deringStrengthGreen: Int = 0,
    var blendRawGreen: Int = 0,
    var wienerIterationsGreen: Int = 0,

    var radiusBlue: BigDecimal? = null,
    var amountBlue: BigDecimal? = null,
    var iterationsBlue: Int = 0,
    var levelBlue: Int = 0,
    var clippingStrengthBlue: Int = 0,
    var clippingRangeBlue: Int = 0,
    var deringRadiusBlue: BigDecimal? = null,
    var deringStrengthBlue: Int = 0,
    var blendRawBlue: Int = 0,
    var wienerIterationsBlue: Int = 0,

    var sharpenMode: String? = null,
    var applySharpenToChannel: ChannelEnum? = null,
    var applyUnsharpMask: Boolean = false,
    var applyWienerDeconvolution: Boolean = false,

    // denoise
    var denoiseAlgorithm1: String? = null,
    var denoise1Amount: BigDecimal? = null,
    var denoise1Radius: BigDecimal? = null,
    var denoise1Iterations: Int = 0,
    var denoiseAlgorithm2: String? = null,
    var savitzkyGolaySize: Int = 0,
    var savitzkyGolayAmount: Int = 0,
    var savitzkyGolayIterations: Int = 0,
    var denoise2Radius: BigDecimal? = null,
    var denoise2Iterations: Int = 0,
    var applyDenoiseToChannel: ChannelEnum? = null,
    var bilateralSigmaColor: Int = 0,
    var bilateralSigmaSpace: Int = 0,
    var bilateralRadius: Int = 0,
    var bilateralIterations: Int = 0,

    var denoise1AmountGreen: BigDecimal? = null,
    var denoise1RadiusGreen: BigDecimal? = null,
    var denoise1IterationsGreen: Int = 0,
    var iansAmountGreen: BigDecimal? = null,
    var iansRecoveryGreen: BigDecimal? = null,
    var rofThetaGreen: Int = 0,
    var rofIterationsGreen: Int = 0,
    var savitzkyGolaySizeGreen: Int = 0,
    var savitzkyGolayAmountGreen: Int = 0,
    var savitzkyGolayIterationsGreen: Int = 0,
    var denoise2RadiusGreen: BigDecimal? = null,
    var denoise2IterationsGreen: Int = 0,
    var bilateralSigmaColorGreen: Int = 0,
    var bilateralSigmaSpaceGreen: Int = 0,
    var bilateralRadiusGreen: Int = 0,
    var bilateralIterationsGreen: Int = 0,

    var denoise1AmountBlue: BigDecimal? = null,
    var denoise1RadiusBlue: BigDecimal? = null,
    var denoise1IterationsBlue: Int = 0,
    var iansAmountBlue: BigDecimal? = null,
    var iansRecoveryBlue: BigDecimal? = null,
    var rofThetaBlue: Int = 0,
    var rofIterationsBlue: Int = 0,
    var savitzkyGolaySizeBlue: Int = 0,
    var savitzkyGolayAmountBlue: Int = 0,
    var savitzkyGolayIterationsBlue: Int = 0,
    var denoise2RadiusBlue: BigDecimal? = null,
    var denoise2IterationsBlue: Int = 0,
    var bilateralSigmaColorBlue: Int = 0,
    var bilateralSigmaSpaceBlue: Int = 0,
    var bilateralRadiusBlue: Int = 0,
    var bilateralIterationsBlue: Int = 0,

    // light & contrast
    var gamma: BigDecimal? = null,
    var contrast: Int = 0,
    var brightness: Int = 0,
    var lightness: Int = 0,
    var background: Int = 0,
    var localContrastMode: String? = null,
    var localContrastFine: Int = 0,
    var localContrastMedium: Int = 0,
    var localContrastLarge: Int = 0,
    var preserveDarkBackground: Boolean = false,

    // color & dispersion
    var red: BigDecimal? = null,
    var green: BigDecimal? = null,
    var blue: BigDecimal? = null,
    var purple: BigDecimal? = null,
    var saturation: BigDecimal? = null,
    var dispersionCorrectionEnabled: Boolean = false,
    var dispersionCorrectionRedX: Double = 0.0,
    var dispersionCorrectionBlueX: Double = 0.0,
    var dispersionCorrectionRedY: Double = 0.0,
    var dispersionCorrectionBlueY: Double = 0.0,
    var luminanceIncludeRed: Boolean = false,
    var luminanceIncludeGreen: Boolean = false,
    var luminanceIncludeBlue: Boolean = false,
    var luminanceIncludeColor: Boolean = false,
    var normalizeColorBalance: Boolean = false,

    var scale: Double = 0.0,
    var rotationAngle: Double = 0.0,
    var openImageMode: OpenImageModeEnum? = null,
    var psf: PSFDTO? = null

) {
    // Secondary constructor for mapping from domain Profile
    constructor(profile: Profile) : this() {
        this.name = profile.name

        // sharpen
        this.applySharpenToChannel = ChannelEnum.RGB
        this.radius = profile.radius
        this.amount = profile.amount
        this.iterations = profile.iterations
        this.level = profile.level
        this.clippingStrength = profile.clippingStrength
        this.clippingRange = profile.clippingRange
        this.deringRadius = profile.deringRadius
        this.deringStrength = profile.deringStrength
        this.blendRaw = profile.blendRaw
        this.wienerIterations = profile.wienerIterations
        this.wienerRepetitions = profile.wienerRepetitions

        this.radiusGreen = profile.radiusGreen
        this.amountGreen = profile.amountGreen
        this.iterationsGreen = profile.iterationsGreen
        this.levelGreen = profile.levelGreen
        this.clippingStrengthGreen = profile.clippingStrengthGreen
        this.clippingRangeGreen = profile.clippingRangeGreen
        this.deringRadiusGreen = profile.deringRadiusGreen
        this.deringStrengthGreen = profile.deringStrengthGreen
        this.blendRawGreen = profile.blendRawGreen
        this.wienerIterationsGreen = profile.wienerIterationsGreen

        this.radiusBlue = profile.radiusBlue
        this.amountBlue = profile.amountBlue
        this.iterationsBlue = profile.iterationsBlue
        this.levelBlue = profile.levelBlue
        this.clippingStrengthBlue = profile.clippingStrengthBlue
        this.clippingRangeBlue = profile.clippingRangeBlue
        this.deringRadiusBlue = profile.deringRadiusBlue
        this.deringStrengthBlue = profile.deringStrengthBlue
        this.blendRawBlue = profile.blendRawBlue
        this.wienerIterationsBlue = profile.wienerIterationsBlue

        this.sharpenMode = profile.sharpenMode
        this.luminanceIncludeRed = profile.luminanceIncludeRed
        this.luminanceIncludeGreen = profile.luminanceIncludeGreen
        this.luminanceIncludeBlue = profile.luminanceIncludeBlue
        this.luminanceIncludeColor = profile.luminanceIncludeColor

        // Let op: In Profile was dit Boolean object (nullable), hier primitive boolean.
        // We gebruiken Elvis operator ?: false voor safety
        this.applyWienerDeconvolution = profile.applyWienerDeconvolution ?: false
        this.applyUnsharpMask = profile.applyUnsharpMask ?: false

        // denoise
        this.denoiseAlgorithm1 = profile.denoiseAlgorithm1
        this.denoise1Amount = profile.denoise1Amount
        this.denoise1Radius = profile.denoise1Radius
        this.denoise1Iterations = profile.denoise1Iterations
        this.denoiseAlgorithm2 = profile.denoiseAlgorithm2
        this.denoise2Radius = profile.denoise2Radius
        this.denoise2Iterations = profile.denoise2Iterations
        this.savitzkyGolaySize = profile.savitzkyGolaySize
        this.savitzkyGolayAmount = profile.savitzkyGolayAmount
        this.savitzkyGolayIterations = profile.savitzkyGolayIterations
        this.bilateralIterations = profile.bilateralIterations
        this.bilateralRadius = profile.bilateralRadius
        this.bilateralSigmaColor = profile.bilateralSigmaColor
        this.bilateralSigmaSpace = profile.bilateralSigmaSpace

        this.denoise1AmountGreen = profile.denoise1AmountGreen
        this.denoise1RadiusGreen = profile.denoise1RadiusGreen
        this.denoise1IterationsGreen = profile.denoise1IterationsGreen
        this.denoise2RadiusGreen = profile.denoise2RadiusGreen
        this.denoise2IterationsGreen = profile.denoise2IterationsGreen
        this.savitzkyGolaySizeGreen = profile.savitzkyGolaySizeGreen
        this.savitzkyGolayAmountGreen = profile.savitzkyGolayAmountGreen
        this.savitzkyGolayIterationsGreen = profile.savitzkyGolayIterationsGreen
        this.rofThetaGreen = profile.rofThetaGreen
        this.rofIterationsGreen = profile.rofIterationsGreen
        this.bilateralIterationsGreen = profile.bilateralIterationsGreen
        this.bilateralRadiusGreen = profile.bilateralRadiusGreen
        this.bilateralSigmaColorGreen = profile.bilateralSigmaColorGreen
        this.bilateralSigmaSpaceGreen = profile.bilateralSigmaSpaceGreen

        this.denoise1AmountBlue = profile.denoise1AmountBlue
        this.denoise1RadiusBlue = profile.denoise1RadiusBlue
        this.denoise1IterationsBlue = profile.denoise1IterationsBlue
        this.denoise2RadiusBlue = profile.denoise2RadiusBlue
        this.denoise2IterationsBlue = profile.denoise2IterationsBlue
        this.savitzkyGolaySizeBlue = profile.savitzkyGolaySizeBlue
        this.savitzkyGolayAmountBlue = profile.savitzkyGolayAmountBlue
        this.savitzkyGolayIterationsBlue = profile.savitzkyGolayIterationsBlue
        this.rofThetaBlue = profile.rofThetaBlue
        this.rofIterationsBlue = profile.rofIterationsBlue
        this.bilateralIterationsBlue = profile.bilateralIterationsBlue
        this.bilateralRadiusBlue = profile.bilateralRadiusBlue
        this.bilateralSigmaColorBlue = profile.bilateralSigmaColorBlue
        this.bilateralSigmaSpaceBlue = profile.bilateralSigmaSpaceBlue

        // color & dispersion
        this.red = profile.red
        this.green = profile.green
        this.blue = profile.blue
        this.purple = profile.purple
        this.saturation = profile.saturation
        this.normalizeColorBalance = profile.normalizeColorBalance

        // constrast & light
        this.gamma = profile.gamma
        this.contrast = profile.contrast
        this.brightness = profile.brightness
        this.lightness = profile.lightness
        this.background = profile.background
        this.localContrastMode = profile.localContrastMode
        this.localContrastFine = profile.localContrastFine
        this.localContrastMedium = profile.localContrastMedium
        this.localContrastLarge = profile.localContrastLarge
        this.dispersionCorrectionEnabled = profile.dispersionCorrectionEnabled
        this.dispersionCorrectionRedX = profile.dispersionCorrectionRedX
        this.dispersionCorrectionBlueX = profile.dispersionCorrectionBlueX
        this.dispersionCorrectionRedY = profile.dispersionCorrectionRedY
        this.dispersionCorrectionBlueY = profile.dispersionCorrectionBlueY
        this.preserveDarkBackground = profile.preserveDarkBackground
        this.clippingSuppression = profile.clippingSuppression

        this.scale = profile.scale
        this.rotationAngle = profile.rotationAngle
        this.openImageMode = profile.openImageMode

        val profilePsf = profile.psf
        if (profilePsf != null) {
            this.psf = PSFDTO(
                profilePsf.airyDiskRadius,
                profilePsf.seeingIndex,
                profilePsf.diffractionIntensity,
                profilePsf.type
            )
        }
    }
}