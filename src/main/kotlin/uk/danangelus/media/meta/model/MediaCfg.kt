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
        val errorDirectory: String,
        val nomatchDirectory: String,
        val destinationDirectory: String,
        val sourceDirectory: String,
    )
}