package uk.danangelus.media.meta.model

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Represents the configuration for a monitored media directory.
 *
 * @author Dan Bennett
 */
@ConfigurationProperties(
    prefix = "tmdb"
)
class TMDBServiceCfg(
    val access: Access,
    val baseUrl: String,
    val api: Map<String, String>,
) {

    class Access(
        val key: String,
        val token: String,
    )
}