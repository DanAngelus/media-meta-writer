package uk.danangelus.media.meta.services.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Represents a response from the TMDB configuration API
 *
 * @author Dan Bennett
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class ConfigurationResponse(
    @field:JsonProperty("images") val images: Image? = null,
    @field:JsonProperty("change_keys") val changeKeys: List<String> = emptyList(),
) {

    class Image(
        @field:JsonProperty("base_url") val baseUrl: String? = null,
        @field:JsonProperty("secure_base_url") val secureBaseUrl: String? = null,
        @field:JsonProperty("backdrop_sizes") val backdropSizes: List<String>? = null,
        @field:JsonProperty("logo_sizes") val logoSizes: List<String>? = null,
        @field:JsonProperty("poster_sizes") val posterSizes: List<String>? = null,
    )
}