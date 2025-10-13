package uk.danangelus.media.meta.services.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Represents a response from the TMDB movie details API
 *
 * @author Dan Bennett
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class MovieDetailsResponse(
    @field:JsonProperty("id") val id: Long? = null,
    @field:JsonProperty("adult") val adult: Boolean? = null,
    @field:JsonProperty("imdb_id") val imdbid: String? = null,
    @field:JsonProperty("release_date") val releaseDate: String? = null,
    @field:JsonProperty("title") val title: String? = null,
    @field:JsonProperty("original_title") val originalTitle: String? = null,
    @field:JsonProperty("tagline") val tagline: String? = null,
    @field:JsonProperty("overview") val overview: String? = null,
    @field:JsonProperty("popularity") val popularity: Double? = null,
    @field:JsonProperty("vote_average") val voteAverage: Double? = null,
    @field:JsonProperty("runtime") val runtime: Int? = null,
    @field:JsonProperty("genre_ids") val genreIds: List<Int>? = null,
    @field:JsonProperty("genres") val genres: List<Genre>? = null,
    @field:JsonProperty("backdrop_path") val backdropPath: String? = null,
    @field:JsonProperty("logo_path") val logoPath: String? = null,
    @field:JsonProperty("poster_path") val posterPath: String? = null,
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    class Genre(
        @field:JsonProperty("id") val id: Int? = null,
        @field:JsonProperty("name") val name: String? = null,
    )
}