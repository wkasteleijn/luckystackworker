package nl.wilcokas.luckystackworker.service.bean

data class LswImageLayers(
    val width: Int,
    val height: Int,
    val layers: Array<ShortArray>
) {

}