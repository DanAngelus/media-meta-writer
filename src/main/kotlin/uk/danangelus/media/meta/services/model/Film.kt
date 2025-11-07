package uk.danangelus.media.meta.services.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class Film(
    @field:JsonProperty("adult") val adult: Boolean? = null,
    @field:JsonProperty("backdrop_path") val backdropPath: String? = null,
    @field:JsonProperty("belongs_to_collection") val belongsToCollection: Any? = null,
    @field:JsonProperty("budget") val budget: Int? = null,
    @field:JsonProperty("genres") val genres: List<Genre>? = null,
    @field:JsonProperty("genre_ids") val genreIds: List<Int>? = null,
    @field:JsonProperty("homepage") val homepage: String? = null,
    @field:JsonProperty("id") val id: Int? = null,
    @field:JsonProperty("imdb_id") val imdbId: String? = null,
    @field:JsonProperty("logo_path") val logoPath: String? = null,
    @field:JsonProperty("original_language") val originalLanguage: String? = null,
    @field:JsonProperty("original_title") val originalTitle: String? = null,
    @field:JsonProperty("overview") val overview: String? = null,
    @field:JsonProperty("popularity") val popularity: Double? = null,
    @field:JsonProperty("poster_path") val posterPath: String? = null,
    @field:JsonProperty("production_companies") val productionCompanies: List<ProductionCompany>? = null,
    @field:JsonProperty("production_countries") val productionCountries: List<Country>? = null,
    @field:JsonProperty("release_date") val releaseDate: String? = null,
    @field:JsonProperty("revenue") val revenue: Int? = null,
    @field:JsonProperty("runtime") val runtime: Int? = null,
    @field:JsonProperty("spoken_languages") val spokenLanguages: List<SpokenLanguage>? = null,
    @field:JsonProperty("status") val status: String? = null,
    @field:JsonProperty("tagline") val tagline: String? = null,
    @field:JsonProperty("title") val title: String? = null,
    @field:JsonProperty("video") val video: Boolean? = null,
    @field:JsonProperty("vote_average") val voteAverage: Double? = null,
    @field:JsonProperty("vote_count") val voteCount: Int? = null,
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    class Country(
        @field:JsonProperty("iso_3166_1") val iso3166: String? = null,
        @field:JsonProperty("name") val name: String? = null,
    ) {
        override fun toString(): String {
            return name ?: "Unknown"
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    class Genre(
        @field:JsonProperty("id") val id: Int? = null,
        @field:JsonProperty("name") val name: String? = null,
    ) {
        override fun toString(): String {
            return name ?: "Unknown"
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    class ISODate(
        @field:JsonProperty("iso_3166_1") val iso: String? = null,
        @field:JsonProperty("release_dates") val releaseDates: List<ReleaseDate>? = null,
    )

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    class Keyword(
        @field:JsonProperty("id") val id: Int? = null,
        @field:JsonProperty("name") val name: String? = null,
    )

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    class ProductionCompany(
        @field:JsonProperty("id") val id: Int? = null,
        @field:JsonProperty("name") val name: String? = null,
        @field:JsonProperty("logo_path") val logoPath: String? = null,
    ) {
        override fun toString(): String {
            return name ?: "Unknown"
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    class ReleaseDate(
        @field:JsonProperty("certification") val certification: String? = null,
        @field:JsonProperty("release_date") val releaseDate: String? = null,
    )

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class SpokenLanguage(
        @field:JsonProperty("english_name") val englishName: String? = null,
        @field:JsonProperty("iso_639_1") val iso639_1: String? = null,
        @field:JsonProperty("name") val name: String? = null
    )
}