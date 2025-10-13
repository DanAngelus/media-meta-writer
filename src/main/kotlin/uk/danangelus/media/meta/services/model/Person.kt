package uk.danangelus.media.meta.services.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * @author Dan Bennett
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class Person(
    @field:JsonProperty("name") val name: String? = null,
    @field:JsonProperty("character") val character: String? = null,
    @field:JsonProperty("role") val role: String? = null,
    @field:JsonProperty("job") val job: String? = null,
    @field:JsonProperty("order") val order: Int? = null,
)