package uk.danangelus.media.meta.services.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

/**
 * List of genres from TMDB
 * @author Dan Bennett
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class GenreListResponse(
    val genres: List<MovieDetailsResponse.Genre>? = null
)