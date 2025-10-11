package uk.danangelus.media.meta.services

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpMethod
import org.springframework.http.RequestEntity
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.util.UriComponentsBuilder
import uk.danangelus.media.meta.model.MediaMetadata

/**
 * Searches IMDB for movie details to apply to metadata.
 *
 * @author Dan Bennett
 */
@Service
class TMDBService(
    private val restTemplate: RestTemplate,
    @Value("\${tmdb.api.key}") private val apiKey: String,
    @Value("\${tmdb.api.access-token}") private val accessToken: String,
    @Value("\${tmdb.api.url}") private val url: String,
) {

    fun findMovie(metadata: MediaMetadata) {
        val uri = UriComponentsBuilder
            .fromUriString(url)
            .queryParam("api_key", apiKey)
            .queryParam("query", metadata.title)
            .queryParam("year", metadata.year)
            .build(null)

        log.info("Searching TMDb for title: ${metadata.title}, year: ${metadata.year ?: "UNKNOWN"}")

        try {
            val request = RequestEntity<Void>(
                LinkedMultiValueMap(mapOf("Authorization" to listOf("Bearer ${accessToken}"))),
                HttpMethod.GET,
                uri,
            )
            val response = restTemplate.exchange<Map<String?, Object?>>(request).body

            val results = response?.get("results") as? List<Map<String, Any>>

            val movieData = results?.firstOrNull() // Take the first result
            if (movieData != null) {
                metadata.tmdbId = movieData["id"]?.toString()
                metadata.title = movieData["title"] as? String
                metadata.rating = movieData["vote_average"]?.toString()
                metadata.year = "${(movieData["release_date"] as? String)?.take(4)?.toInt()}"
                metadata.length = metadata.length ?: "${movieData["runtime"] ?: "Unknown"}"

                log.info("TMDb metadata found: $metadata")
            } else {
                log.warn("No results found on TMDb for title: ${metadata.title}")
            }
        } catch (ex: Exception) {
            log.error("Error while fetching metadata from TMDb for title: ${metadata.title}", ex)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(TMDBService::class.java)
    }
}