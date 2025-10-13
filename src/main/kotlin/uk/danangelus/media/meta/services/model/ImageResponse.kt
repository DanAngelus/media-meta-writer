package uk.danangelus.media.meta.services.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Represents a response from the TMDB image API
 *
 * @author Dan Bennett
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class ImageResponse(
    @field:JsonProperty("backdrops") val backdrops: List<Image> = emptyList(),
    @field:JsonProperty("logos") val logos: List<Image> = emptyList(),
    @field:JsonProperty("posters") val posters: List<Image> = emptyList(),
) {

    class Image(
        @field:JsonProperty("aspect_ratio") val aspectRatio: Long? = null,
        @field:JsonProperty("file_path") val filePath: String? = null,
        @field:JsonProperty("height") val height: Int? = null,
        @field:JsonProperty("width") val width: Int? = null,
    )
}