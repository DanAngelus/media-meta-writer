package uk.danangelus.media.meta.services.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Represents a response from the TMDB TV details API
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class Series(
    @field:JsonProperty("id") val id: Long? = null,
    @field:JsonProperty("adult") val adult: Boolean? = null,
    @field:JsonProperty("backdrop_path") val backdropPath: String? = null,
    @field:JsonProperty("created_by") val createdBy: List<Creator>? = null,
    @field:JsonProperty("episode_run_time") val episodeRunTime: List<Int>? = null,
    @field:JsonProperty("first_air_date") val firstAirDate: String? = null,
    @field:JsonProperty("genres") val genres: List<Genre>? = null,
    @field:JsonProperty("homepage") val homepage: String? = null,
    @field:JsonProperty("in_production") val inProduction: Boolean? = null,
    @field:JsonProperty("languages") val languages: List<String>? = null,
    @field:JsonProperty("last_air_date") val lastAirDate: String? = null,
    @field:JsonProperty("last_episode_to_air") val lastEpisodeToAir: Episode? = null,
    @field:JsonProperty("name") val name: String? = null,
    @field:JsonProperty("next_episode_to_air") val nextEpisodeToAir: Episode? = null,
    @field:JsonProperty("networks") val networks: List<Network>? = null,
    @field:JsonProperty("number_of_episodes") val numberOfEpisodes: Int? = null,
    @field:JsonProperty("number_of_seasons") val numberOfSeasons: Int? = null,
    @field:JsonProperty("origin_country") val originCountry: List<String>? = null,
    @field:JsonProperty("original_language") val originalLanguage: String? = null,
    @field:JsonProperty("original_name") val originalName: String? = null,
    @field:JsonProperty("overview") val overview: String? = null,
    @field:JsonProperty("popularity") val popularity: Double? = null,
    @field:JsonProperty("poster_path") val posterPath: String? = null,
    @field:JsonProperty("production_companies") val productionCompanies: List<ProductionCompany>? = null,
    @field:JsonProperty("production_countries") val productionCountries: List<Country>? = null,
    @field:JsonProperty("seasons") val seasons: List<Season>? = null,
    @field:JsonProperty("spoken_languages") val spokenLanguages: List<Language>? = null,
    @field:JsonProperty("status") val status: String? = null,
    @field:JsonProperty("tagline") val tagline: String? = null,
    @field:JsonProperty("type") val type: String? = null,
    @field:JsonProperty("vote_average") val voteAverage: Double? = null,
    @field:JsonProperty("vote_count") val voteCount: Int? = null,
) {

    val year: String? = firstAirDate?.substring(0, 4)

    override fun toString(): String {
        return "${name ?: originalName ?: "Unknown"} (${year ?: "Unknown"})"
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
    class Creator(
        @field:JsonProperty("id") val id: Int? = null,
        @field:JsonProperty("credit_id") val creditId: String? = null,
        @field:JsonProperty("name") val name: String? = null,
        @field:JsonProperty("original_name") val originalName: String? = null,
        @field:JsonProperty("gender") val gender: Int? = null,
        @field:JsonProperty("profile_path") val profilePath: String? = null,
    ) {
        override fun toString(): String {
            return name ?: originalName ?: "Unknown"
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    class Episode(
        @field:JsonProperty("id") val id: Int? = null,
        @field:JsonProperty("name") val name: String? = null,
        @field:JsonProperty("overview") val overview: String? = null,
        @field:JsonProperty("vote_average") val voteAverage: Double? = null,
        @field:JsonProperty("vote_count") val voteCount: Int? = null,
        @field:JsonProperty("air_date") val airDate: String? = null,
        @field:JsonProperty("episode_number") val episodeNumber: String? = null,
        @field:JsonProperty("episode_type") val episodeType: String? = null,
        @field:JsonProperty("production_code") val productionCode: String? = null,
        @field:JsonProperty("runtime") val runtime: Int? = null,
        @field:JsonProperty("season_number") val seasonNumber: Int? = null,
        @field:JsonProperty("show_id") val showId: Int? = null,
        @field:JsonProperty("still_path") val stillPath: String? = null,
    ) {
        override fun toString(): String {
            return "s${seasonNumber}e$episodeNumber - $name"
        }
    }

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
    class Network(
        @field:JsonProperty("id") val id: Int? = null,
        @field:JsonProperty("logo_path") val logoPath: String? = null,
        @field:JsonProperty("name") val name: String? = null,
        @field:JsonProperty("origin_country") val originCountry: String? = null,
    ) {
        override fun toString(): String {
            return name ?: "Unknown"
        }
    }

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
    class Language(
        @field:JsonProperty("english_name") val englishName: String? = null,
        @field:JsonProperty("iso_639_1") val iso639: String? = null,
        @field:JsonProperty("name") val name: String? = null,
    ) {
        override fun toString(): String {
            return name ?: "Unknown"
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    class Season(
        @field:JsonProperty("air_date") val airDate: String? = null,
        @field:JsonProperty("episode_count") val episodeCount: Int? = null,
        @field:JsonProperty("episodes") val episodes: List<Episode>? = null,
        @field:JsonProperty("id") val id: Int? = null,
        @field:JsonProperty("name") val name: String? = null,
        @field:JsonProperty("overview") val overview: String? = null,
        @field:JsonProperty("poster_path") val posterPath: String? = null,
        @field:JsonProperty("season_number") val seasonNumber: String? = null,
        @field:JsonProperty("vote_average") val voteAverage: Double? = null,
    ) {
        override fun toString(): String {
            return name ?: "Season $seasonNumber"
        }
    }
}