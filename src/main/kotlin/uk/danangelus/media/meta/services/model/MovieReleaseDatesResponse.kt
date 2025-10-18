package uk.danangelus.media.meta.services.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * List of keywords from TMDB
 *
 * @author Dan Bennett
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class MovieReleaseDatesResponse(
    val results: List<ISODate>? = null
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    class ISODate(
        @field:JsonProperty("iso_3166_1") val iso: String? = null,
        @field:JsonProperty("release_dates") val releaseDates: List<ReleaseDate>? = null,
    )

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    class ReleaseDate(
        @field:JsonProperty("certification") val certification: String? = null,
        @field:JsonProperty("release_date") val releaseDate: String? = null,
    )
}