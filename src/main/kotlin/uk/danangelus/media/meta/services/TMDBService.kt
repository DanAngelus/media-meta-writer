package uk.danangelus.media.meta.services

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpMethod
import org.springframework.http.RequestEntity
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.util.UriComponentsBuilder
import uk.danangelus.media.meta.model.MediaMetadata
import uk.danangelus.media.meta.services.model.ConfigurationResponse
import uk.danangelus.media.meta.services.model.GenreListResponse
import uk.danangelus.media.meta.services.model.MovieSearchResponse

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

    private var configuration: ConfigurationResponse? = null
    private var genres: MutableMap<Int, String> = mutableMapOf()

    @PostConstruct
    fun init() {
        // Preload required information
        loadConfiguration()
        loadGenres()
    }

    private fun loadConfiguration() {
        val request = RequestEntity<Void>(
            LinkedMultiValueMap(mapOf("Authorization" to listOf("Bearer $accessToken"))),
            HttpMethod.GET,
            UriComponentsBuilder.fromUriString("https://api.themoviedb.org/3/configuration").build(null),
        )
        configuration = restTemplate.exchange<ConfigurationResponse>(request).body
    }

    private fun loadGenres() {
        val request = RequestEntity<Void>(
            LinkedMultiValueMap(mapOf("Authorization" to listOf("Bearer $accessToken"))),
            HttpMethod.GET,
            UriComponentsBuilder.fromUriString("https://api.themoviedb.org/3/genre/movie/list").build(null),
        )
        restTemplate.exchange<GenreListResponse>(request).body?.genres
            ?.forEach { genres[it.id!!] = it.name!! }
    }

    fun findMovie(metadata: MediaMetadata) {
        val uri = UriComponentsBuilder
            .fromUriString(url)
            .queryParam("api_key", apiKey)
            .queryParam("query", metadata.title)
            .queryParam("year", metadata.year)
            .build(null)

        log.info("Searching TMDb for title: ${metadata.title}, year: ${metadata.year ?: "Unknown"}")

        try {
            val request = RequestEntity<Void>(
                LinkedMultiValueMap(mapOf("Authorization" to listOf("Bearer $accessToken"))),
                HttpMethod.GET,
                uri,
            )
            val response = restTemplate.exchange<MovieSearchResponse>(request).body

            val movieData = response?.results?.firstOrNull() // Take the first result
            if (movieData != null) {
                metadata.tmdbId = movieData.id?.toString()
                metadata.imdbId = movieData.imdbid
                metadata.title = movieData.title
                metadata.originalTitle = movieData.originalTitle
                metadata.rating = movieData.voteAverage?.toString()
                metadata.year = "${movieData.releaseDate?.take(4)?.toInt()}"
                metadata.length = metadata.length ?: "${movieData.runtime ?: "Unknown"}"
                metadata.outline = movieData.tagline
                metadata.plot = movieData.overview

                movieData.genreIds
                    ?.filter { genres.keys.contains(it) }
                    ?.map {
                        metadata.genre += genres[it]
                    }
                    ?.joinToString(",")

                // ToDo :: Grab more data!

//                getImages(metadata)

                metadata.backdrop = retrieveImage(movieData.backdropPath)
                metadata.logo = retrieveImage(movieData.logoPath)
                metadata.poster = retrieveImage(movieData.posterPath)

                log.info("TMDb metadata found: $metadata")
            } else {
                log.warn("No results found on TMDb for title: ${metadata.title}")
            }
        } catch (ex: Exception) {
            log.error("Error while fetching metadata from TMDb for title: ${metadata.title}", ex)
        }
    }

    private fun retrieveImage(image: String?): ByteArray? {
        return try {
            val imageRequest = RequestEntity<Void>(
                LinkedMultiValueMap(
                    mapOf(
                        "Authorization" to listOf("Bearer $accessToken"),
                        "Content-Type" to listOf("image/jpeg", "image/png"),
                    )
                ),
                HttpMethod.GET,
                UriComponentsBuilder.fromUriString("${configuration?.images?.baseUrl}/{resolution}${image}")
                    .build(mapOf("resolution" to "original")),
            )
            restTemplate.exchange<ByteArray?>(imageRequest).body
        } catch (ex: Exception) {
            log.error("Error while fetching image from TMDb", ex)
            null
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(TMDBService::class.java)
    }
}