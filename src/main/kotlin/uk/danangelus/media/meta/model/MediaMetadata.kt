package uk.danangelus.media.meta.model

/**
 * Represents the metadata for a movie or film.
 *
 * @author Dan Bennett
 */
data class MediaMetadata(
    var title: String? = null,
    var year: String? = null,
    var description: String? = null,
    var resolution: String? = null,
    var releaseDate: String? = null,
    var rating: String? = null,
    var length: String? = null,
    var series: String? = null,
    var seriesNumber: String? = null,
    var episodeNumber: String? = null,
    var poster: ByteArray? = null, // ToDo :: Separate this and store image next to movie.
    var imdbId: String? = null,
    var tmdbId: String? = null,
    var tvdbId: String? = null,
)