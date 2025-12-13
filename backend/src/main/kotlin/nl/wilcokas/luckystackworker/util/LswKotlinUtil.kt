package nl.wilcokas.luckystackworker.util

import ij.io.Opener
import nl.wilcokas.luckystackworker.service.bean.LswImageLayers
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun <T : Any> T.logger(): Lazy<Logger> {
    return lazy { LoggerFactory.getLogger(this.javaClass) }
}
