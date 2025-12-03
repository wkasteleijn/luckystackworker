package nl.wilcokas.luckystackworker.model

data class PSF(
    var airyDiskRadius: Double = 0.0,
    var seeingIndex: Double = 0.0,
    var diffractionIntensity: Double = 0.0,
    var type: PSFType? = null
)