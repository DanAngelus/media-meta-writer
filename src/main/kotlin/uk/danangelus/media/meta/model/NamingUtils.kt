package uk.danangelus.media.meta.model

/**
 * @author Dan Bennett
 */
object NamingUtils {

    fun clean(name: String?): String = name
        ?.replace("&", "and")
        ?.replace(": ", " - ")
        ?.replace(Regex("[^\\p{L}\\p{N}.\\- ]"), "")
        ?.trim() ?: ""
}