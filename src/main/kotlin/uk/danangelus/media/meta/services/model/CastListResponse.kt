package uk.danangelus.media.meta.services.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Response from the TMDB cast API
 *
 * @author Dan Bennett
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class CastListResponse(
    @field:JsonProperty("cast") val cast: List<Person>? = null,
    @field:JsonProperty("crew") val crew: List<Person>? = null,
)