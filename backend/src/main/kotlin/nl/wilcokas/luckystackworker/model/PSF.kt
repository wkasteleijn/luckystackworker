package nl.wilcokas.luckystackworker.model

data class PSF(
    var airyDiskRadius: Double = 0.0,
    var seeingIndex: Double = 0.0,
    var diffractionIntensity: Double = 0.0,

    var airyDiskRadiusGreen: Double = 0.0,
    var seeingIndexGreen: Double = 0.0,
    var diffractionIntensityGreen: Double = 0.0,

    var airyDiskRadiusBlue: Double = 0.0,
    var seeingIndexBlue: Double = 0.0,
    var diffractionIntensityBlue: Double = 0.0,

    var type: PSFType? = null,
)
