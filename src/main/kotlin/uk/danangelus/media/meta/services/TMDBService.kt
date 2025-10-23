package uk.danangelus.media.meta.services

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.http.RequestEntity
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.util.UriComponentsBuilder
import uk.danangelus.media.meta.error.NoMatchException
import uk.danangelus.media.meta.model.Actor
import uk.danangelus.media.meta.model.MediaMetadata
import uk.danangelus.media.meta.model.TMDBServiceCfg
import uk.danangelus.media.meta.services.model.CastListResponse
import uk.danangelus.media.meta.services.model.ConfigurationResponse
import uk.danangelus.media.meta.services.model.GenreListResponse
import uk.danangelus.media.meta.services.model.MovieDetailsResponse
import uk.danangelus.media.meta.services.model.MovieKeywordsResponse
import uk.danangelus.media.meta.services.model.MovieReleaseDatesResponse
import uk.danangelus.media.meta.services.model.MovieSearchResponse
import uk.danangelus.media.meta.services.model.Series
import uk.danangelus.media.meta.services.model.TVSearchResponse

/**
 * Searches IMDB for movie details to apply to metadata.
 *
 * @author Dan Bennett
 */
@Service
class TMDBService(
    private val restTemplate: RestTemplate,
    private val tmdbServiceCfg: TMDBServiceCfg,
) {

    private var configuration: ConfigurationResponse? = null
    private var filmGenres: MutableMap<Int, String> = mutableMapOf()
    private var seriesGenres: MutableMap<Int, String> = mutableMapOf()

    @PostConstruct
    fun init() {
        // Preload required information
        loadConfiguration()
        loadGenres()
    }

    private fun loadConfiguration() {
        val request = RequestEntity<Void>(
            LinkedMultiValueMap(mapOf("Authorization" to listOf("Bearer ${tmdbServiceCfg.access.token}"))),
            HttpMethod.GET,
            UriComponentsBuilder.fromUriString("${tmdbServiceCfg.baseUrl}${tmdbServiceCfg.api["configuration"]}").build(null),
        )
        configuration = restTemplate.exchange<ConfigurationResponse>(request).body
    }

    private fun loadGenres() {

        getGenres("movie-genres")?.forEach { filmGenres[it.id!!] = it.name!! }
        getGenres("tv-genres")?.forEach { seriesGenres[it.id!!] = it.name!! }
    }

    fun getGenres(apiUri: String): List<MovieDetailsResponse.Genre>? {
        val request = RequestEntity<Void>(
            LinkedMultiValueMap(mapOf("Authorization" to listOf("Bearer ${tmdbServiceCfg.access.token}"))),
            HttpMethod.GET,
            UriComponentsBuilder.fromUriString("${tmdbServiceCfg.baseUrl}${tmdbServiceCfg.api[apiUri]}").build(null),
        )
        return restTemplate.exchange<GenreListResponse>(request).body?.genres
    }

    fun populateMovieDetails(metadata: MediaMetadata) {
        val movieData = findMovie(metadata.title!!, metadata.year!!)
        val fullDetails = retrieveMovieDetails(movieData)
        val releaseDate = retrieveReleaseDate(movieData)
        val keywords = retrieveMovieKeywords(movieData)
        val credits = getCast(movieData.id.toString())

        metadata.tmdbId = movieData.id?.toString()
        metadata.imdbId = fullDetails?.imdbid
        metadata.title = movieData.title
        metadata.originalTitle = movieData.originalTitle
        metadata.rating = movieData.voteAverage?.toString()
        metadata.releaseDate = releaseDate?.releaseDate ?: movieData.releaseDate ?: "Unknown"
        metadata.certification = releaseDate?.certification ?: "Unknown"
        metadata.year = "${movieData.releaseDate?.take(4)?.toInt()}"
        metadata.length = fullDetails?.runtime?.toString() ?: metadata.length
        metadata.outline = fullDetails?.tagline
        metadata.plot = movieData.overview
        metadata.language = fullDetails?.originalLanguage

        metadata.genre = fullDetails?.genres?.map { it.name }
            ?: movieData.genreIds
                ?.filter { filmGenres.keys.contains(it) }
                ?.map { filmGenres[it] }
                ?.filter { it.isNullOrBlank().not() }
                ?.toMutableList()

        if (keywords != null) {
            metadata.keywords = keywords.keywords?.map { it.name }?.toList() ?: listOf()
        }
        if (credits != null) {
            metadata.director = credits.crew?.filter { "Director".equals(it.job, true) }
                ?.map { it.name }
                ?.firstOrNull()

            val producers = credits.crew?.filter { "Producer".equals(it.job, true) }
            metadata.producers = producers?.filter { it.name.isNullOrBlank().not() }?.map { it.name!! }

            val actors = credits.cast?.filter { it.name.isNullOrBlank().not() && it.order != null }
            metadata.actors = actors?.filter { it.name.isNullOrBlank().not() }?.map { Actor(it.name!!, it.character, it.order) }
        }
        metadata.backdrop = retrieveImage(movieData.backdropPath)
        metadata.logo = retrieveImage(movieData.logoPath)
        metadata.poster = retrieveImage(movieData.posterPath)

        log.info("[{}] TMDb metadata found", metadata)

    }

    private fun retrieveReleaseDate(movieData: MovieDetailsResponse): MovieReleaseDatesResponse.ReleaseDate? {
        val uri = UriComponentsBuilder
            .fromUriString("${tmdbServiceCfg.baseUrl}${tmdbServiceCfg.api["movie-release-dates"]}")
            .queryParam("api_key", tmdbServiceCfg.access.key)
            .build(mapOf("movie_id" to movieData.id))
        val request = RequestEntity<Void>(
            LinkedMultiValueMap(mapOf("Authorization" to listOf("Bearer ${tmdbServiceCfg.access.token}"))),
            HttpMethod.GET,
            uri,
        )
        val response = restTemplate.exchange<MovieReleaseDatesResponse>(request).body
        return response?.results?.firstOrNull { it.iso.equals("GB", true) }?.releaseDates?.firstOrNull()
    }

    private fun retrieveMovieDetails(movieData: MovieDetailsResponse): MovieDetailsResponse? {
        val uri = UriComponentsBuilder
            .fromUriString("${tmdbServiceCfg.baseUrl}${tmdbServiceCfg.api["movie-details"]}")
            .queryParam("api_key", tmdbServiceCfg.access.key)
            .build(mapOf("movie_id" to movieData.id))
        val request = RequestEntity<Void>(
            LinkedMultiValueMap(mapOf("Authorization" to listOf("Bearer ${tmdbServiceCfg.access.token}"))),
            HttpMethod.GET,
            uri,
        )
       return restTemplate.exchange<MovieDetailsResponse>(request).body
    }

    private fun retrieveMovieKeywords(movieData: MovieDetailsResponse): MovieKeywordsResponse? {
        val uri = UriComponentsBuilder
            .fromUriString("${tmdbServiceCfg.baseUrl}${tmdbServiceCfg.api["movie-keywords"]}")
            .queryParam("api_key", tmdbServiceCfg.access.key)
            .build(mapOf("movie_id" to movieData.id))
        val request = RequestEntity<Void>(
            LinkedMultiValueMap(mapOf("Authorization" to listOf("Bearer ${tmdbServiceCfg.access.token}"))),
            HttpMethod.GET,
            uri,
        )
       return restTemplate.exchange<MovieKeywordsResponse>(request).body
    }

    fun findMovie(title: String, year: String): MovieDetailsResponse {
        val uri = UriComponentsBuilder
            .fromUriString("${tmdbServiceCfg.baseUrl}${tmdbServiceCfg.api["movie-search"]}")
            .queryParam("api_key", tmdbServiceCfg.access.key)
            .queryParam("query", title)
            .queryParam("year", year)
            .build(null)

        log.info("[{} ({})] Searching TMDb for title", title, year)

        try {
            val request = RequestEntity<Void>(
                LinkedMultiValueMap(mapOf("Authorization" to listOf("Bearer ${tmdbServiceCfg.access.token}"))),
                HttpMethod.GET,
                uri,
            )
            val response = restTemplate.exchange<MovieSearchResponse>(request).body

            var movieData = response?.results
                ?.firstOrNull {
                    it.title?.equals(title, true) == true
                        && it.releaseDate?.contains(year) == true
                }

            if (movieData == null) {
                // Fallback to simply the first result
                movieData = response?.results?.firstOrNull()
            }

            if (movieData != null) {
                return movieData
            } else {
                log.warn("[{} ({})] No results found on TMDb for title", title, year)
                throw NoMatchException("No results found on TMDb for title: ${title}")
            }
        } catch (ex: NoMatchException) {
            throw ex
        } catch (ex: Exception) {
            log.error("Error while fetching metadata from TMDb for title: ${title}", ex)
            throw ex
        }
    }

    private fun getCast(movieId: String): CastListResponse? {
        return try {

            val castRequest = RequestEntity<Void>(
                LinkedMultiValueMap(
                    mapOf(
                        "Authorization" to listOf("Bearer ${tmdbServiceCfg.access.token}"),
                    )
                ),
                HttpMethod.GET,
                UriComponentsBuilder
                    .fromUriString("${tmdbServiceCfg.baseUrl}${tmdbServiceCfg.api["movie-credits"]}")
                    .build(mapOf("movie_id" to movieId)),
            )
            restTemplate.exchange<CastListResponse>(castRequest).body
        } catch (ex: Exception) {
            log.error("Error while fetching cast from TMDb", ex)
            null
        }
    }

    private fun retrieveImage(image: String?): ByteArray? {
        return try {
            if (image == null) return null

            val imageRequest = RequestEntity<Void>(
                LinkedMultiValueMap(
                    mapOf(
                        "Authorization" to listOf("Bearer $tmdbServiceCfg.access.token"),
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

    fun findSeries(title: String, year: String): Series? {

        var uri = UriComponentsBuilder
            .fromUriString("${tmdbServiceCfg.baseUrl}${tmdbServiceCfg.api["tv-search"]}")
            .queryParam("api_key", tmdbServiceCfg.access.key)
            .queryParam("query", title)
            .queryParam("year", year)
            .build(null)

        log.info("[{} ({})] Searching TMDb for title", title, year)

        try {
            var request = RequestEntity<Void>(
                LinkedMultiValueMap(mapOf("Authorization" to listOf("Bearer ${tmdbServiceCfg.access.token}"))),
                HttpMethod.GET,
                uri,
            )
            val response = restTemplate.exchange<TVSearchResponse>(request).body

            var tvData = response?.results
                ?.firstOrNull {
                    it.name?.equals(title, true) == true
                            && it.firstAirDate?.contains(year) == true
                }

            if (tvData == null) {
                // Fallback to simply the first result with a matching name
                tvData = response?.results?.firstOrNull {
                    it.name?.equals(title, true) == true
                }
            }

            if (tvData == null) {
                // Fallback to simply the first result
                tvData = response?.results?.firstOrNull()
            }

            if (tvData != null) {
                uri = UriComponentsBuilder
                    .fromUriString("${tmdbServiceCfg.baseUrl}${tmdbServiceCfg.api["tv-details"]}")
                    .queryParam("api_key", tmdbServiceCfg.access.key)
                    .build(mapOf("tv_id" to tvData.id))
                request = RequestEntity<Void>(
                    LinkedMultiValueMap(mapOf("Authorization" to listOf("Bearer ${tmdbServiceCfg.access.token}"))),
                    HttpMethod.GET,
                    uri,
                )
                return restTemplate.exchange<Series?>(request)?.body
            } else {
                log.warn("[{} ({})] No results found on TMDb for title", title, year)
                throw NoMatchException("No results found on TMDb for title: ${title}")
            }
        } catch (ex: NoMatchException) {
            throw ex
        } catch (ex: Exception) {
            log.error("Error while fetching metadata from TMDb for title: ${title}", ex)
            throw ex
        }
    }

    fun getSeason(seriesId: String, seriesName: String, seasonNumber: String): Series.Season? {
        val uri = UriComponentsBuilder
            .fromUriString("${tmdbServiceCfg.baseUrl}${tmdbServiceCfg.api["tv-season"]}")
            .queryParam("api_key", tmdbServiceCfg.access.key)
            .build(mapOf(
                "tv_id" to seriesId,
                "season_number" to seasonNumber
            ))

        log.info("[{} ({})] Searching TMDb for season {} of {}", seasonNumber, seriesName)

        try {
            val request = RequestEntity<Void>(
                LinkedMultiValueMap(mapOf("Authorization" to listOf("Bearer ${tmdbServiceCfg.access.token}"))),
                HttpMethod.GET,
                uri,
            )
            return restTemplate.exchange<Series.Season>(request).body

        } catch (ex: NoMatchException) {
            throw ex
        } catch (ex: Exception) {
            log.error("Error while fetching metadata from TMDb for: ${seriesName} -> Season ${seasonNumber}", ex)
            throw ex
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(TMDBService::class.java)
    }
}