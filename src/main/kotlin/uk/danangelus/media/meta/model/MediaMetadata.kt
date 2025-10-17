package uk.danangelus.media.meta.model

/**
 * Represents the metadata for a movie or film.
 *
 * @author Dan Bennett
 */
class MediaMetadata(
    var title: String? = null,
    var originalTitle: String? = null,
    var year: String? = null,
    var outline: String? = null,
    var plot: String? = null,
    var resolution: String? = null,
    var releaseDate: String? = null,
    var rating: String? = null,
    var length: String? = null,
    var director: String? = null,
    var genre: List<String?>? = null,
    var studio: String? = null,
    var producers: List<String>? = null,
    var actors: List<Actor>? = null,
    var series: String? = null,
    var seriesNumber: String? = null,
    var episodeNumber: String? = null,
    var poster: ByteArray? = null,
    var logo: ByteArray? = null,
    var backdrop: ByteArray? = null,
    var imdbId: String? = null,
    var tmdbId: String? = null,
    var tvdbId: String? = null,
) {

    fun filename(): String = "${title
        ?.replace("&", "and")
        ?.replace(": ", " - ")
        ?.replace(Regex("[^\\p{L}\\p{N}.\\- ]"), "")
        ?.trim()} ($year)"

    override fun toString(): String {
        return "$title ($year)"
    }
}

class Actor(
    val actor: String? = null,
    val character: String? = null,
    val order: Int? = null,
)

enum class MediaType {
    FILM,
    SERIES,
    MUSIC,
    BOOK
}