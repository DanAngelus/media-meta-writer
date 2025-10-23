package uk.danangelus.media.meta.model

/**
 * Represents the metadata for a movie or film.
 *
 * @author Dan Bennett
 */
open class MediaMetadata(
    var title: String? = null,
    var originalTitle: String? = null,
    var year: String? = null,
    var outline: String? = null,
    var plot: String? = null,
    var resolution: String? = null,
    var releaseDate: String? = null,
    var certification: String? = null,
    var rating: String? = null,
    var language: String? = null,
    var length: String? = null,
    var director: String? = null,
    var keywords: List<String?>? = null,
    var genre: List<String?>? = null,
    var studio: String? = null,
    var producers: List<String>? = null,
    var actors: List<Actor>? = null,
    var series: String? = null,
    var season: Int? = null,
    var episode: Int? = null,
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

    fun filmTitle(): String = "$title ($year)"

    fun episodeTitle(): String {
        val ssn = "s${season.toString().padStart(2, '0')}"
        val ep = "e${episode.toString().padStart(2, '0')}"
        val suffix = if (title.isNullOrBlank()) {
            ""
        } else {
            " - $title"
        }
        return "$series - $ssn$ep$suffix"
    }

    override fun toString(): String {
        return if (series.isNullOrBlank()) filmTitle()
        else episodeTitle()
    }
}

class Actor(
    val actor: String? = null,
    val character: String? = null,
    val order: Int? = null,
    var releaseDate: String? = null,
    var certification: String? = null,
    var rating: String? = null,
)

class SeriesMetadata(
    name: String? = null,
    originalName: String? = null,
    year: String? = null,
    outline: String? = null,
    plot: String? = null,
    keywords: List<String?>? = null,
    poster: ByteArray? = null,
    logo: ByteArray? = null,
    backdrop: ByteArray? = null,
    imdbId: String? = null,
    tmdbId: String? = null,
    tvdbId: String? = null,
    val seasons: List<SeasonMetadata>? = null,
) : MediaMetadata(name, originalName, year, outline, plot, null, null, null, null, null, null, null, keywords, null, null, null, null, null, null, null, poster, logo, backdrop, imdbId, tmdbId, tvdbId)


class SeasonMetadata(
    name: String? = null,
    originalName: String? = null,
    year: String? = null,
    outline: String? = null,
    plot: String? = null,
    keywords: List<String?>? = null,
    poster: ByteArray? = null,
    logo: ByteArray? = null,
    backdrop: ByteArray? = null,
    imdbId: String? = null,
    tmdbId: String? = null,
    tvdbId: String? = null,
    val episodes: List<MediaMetadata>? = null,
) : MediaMetadata(name, originalName, year, outline, plot, null, null, null, null, null, null, null, keywords, null, null, null, null, null, null, null, poster, logo, backdrop, imdbId, tmdbId, tvdbId)

enum class MediaType {
    FILM,
    SERIES,
    MUSIC,
    BOOK
}