package nl.wilcokas.luckystackworker.dto

import nl.wilcokas.luckystackworker.model.PSFType

data class PSFDTO(
    var airyDiskRadius: Double = 0.0,
    var seeingIndex: Double = 0.0,
    var diffractionIntensity: Double = 0.0,
    var type: PSFType? = null
)