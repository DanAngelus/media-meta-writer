package uk.danangelus.media.meta.model

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Represents the configuration for a monitored media directory.
 *
 * @author Dan Bennett
 */
@ConfigurationProperties(
    prefix = "media"
)
class MediaCfg(
    val media: List<Media> = mutableListOf(),
) {

    class Media(
        val type: MediaType,
        val sourceDirectory: String,
        val destinationDirectory: String,
    )
}