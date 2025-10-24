package uk.danangelus.media.meta.manager

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.danangelus.media.meta.error.NoMatchException
import uk.danangelus.media.meta.model.MediaCfg.Media
import uk.danangelus.media.meta.organise.FileOrganiser
import uk.danangelus.media.meta.services.TMDBService
import uk.danangelus.media.meta.services.model.Series
import uk.danangelus.media.meta.writer.MetaWriter
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Manages the media files and their metadata, moving relevant files to organised folders.
 *
 * @author Dan Bennett
 */
@Service
class SeriesManager(
    private val mediaOrganiser: FileOrganiser,
    private val metaWriter: MetaWriter,
    private val tmdbService: TMDBService,
    @Value("\${media.enable.create-nfo:true}") private val createNfoEnabled: Boolean,
    @Value("\${media.enable.find-artwork}") private val findArtworkEnabled: Boolean,
) {

    fun register(media: Media, seriesDir: File) {
        try {
            val (seriesName, seriesYear) = retrieveFileData(seriesDir)
            if (seriesName == null || seriesYear == null) return
            val series = getSeriesMetadata(seriesName, seriesYear)
            if (series == null) return
            series.directory = seriesDir

            log.info("[{}] Retrieving series information from TMDB", series)
            Files.list(seriesDir.toPath())
                .filter { it.toFile().isDirectory && it.fileName.toString().startsWith("Season ") }
                .forEach { seasonDir ->
                    val seasonNumber = seasonDir.fileName.toString().substringAfter("Season ").trim()
                    val season = getSeasonMetadata(series, seasonNumber)
                    season.directory = seasonDir.toFile()
                    season.episodes = season.episodes?.toMutableList()
                    Files.list(seasonDir)
                        .filter { file -> file.toFile().isFile && SUPPORTED_VIDEO_FORMATS.contains(file.toFile().extension) }
                        .forEach {
                            try {
                                val file = it.toFile()

                                val (episodeNumber, title) = retrieveFileData(seasonDir.fileName.toString(), file)
                                val episode = getEpisodeMetadata(series, season, episodeNumber, title)
                                episode.file = file
                                log.info("[{}] Retrieving updated information from TMDB", series ?: "Unknown")
                            } catch (ex: Exception) {
                                log.error("[{}] Failed to process file: {}", series, it.toFile().absolutePath, ex)
                            }
                        }
                }
            processMedia(series)
        } catch (ex: Exception) {
            log.error("Error while processing file: ${seriesDir.absolutePath}", ex)
            mediaOrganiser.moveToError(media, seriesDir)
        }
    }

    fun getSeriesMetadata(name: String, year: String): Series? {
        val metadata = tmdbService.findSeries(name, year)
        log.info("[{}] Retrieved data from TMDB", metadata)
        return metadata
    }

    fun getSeasonMetadata(series: Series, seasonNumber: String): Series.Season {
        val season = series.seasons?.firstOrNull { it.seasonNumber.equals(seasonNumber, true) }

        log.info("[{}] Found matching season: {}", series, season)
        if (season == null) {
            log.info("[{}] No matching season found for: {}", series, seasonNumber)
            throw NoMatchException("No matching season found for: $seasonNumber")
        }
        val fullSeasonDetails = tmdbService.getSeason(series.id.toString(), series.name!!, season.seasonNumber!!)
            ?: throw NoMatchException("No matching season found for: $seasonNumber")

        season.episodes = fullSeasonDetails.episodes
        return season
    }

    fun getEpisodeMetadata(series: Series, season: Series.Season, episodeNumber: String, title: String): Series.Episode {
        val episode = season.episodes?.firstOrNull { it.episodeNumber.equals(episodeNumber, true) }

        log.info("[{} - {}] Found matching episode: {}", series, season, episode)
        return episode ?: throw NoMatchException("No matching season found for: $episodeNumber")
    }

    fun getTitle(fileName: String): String? {
        return fileName.split("(").first().trim().ifBlank { null }
    }

    fun getYear(fileName: String): String? {
        return fileName.substringAfter("(").substringBefore(")").trim().ifBlank { null }
    }

    fun retrieveFileData(file: File): Pair<String?, String?> {
        val fileName = file.nameWithoutExtension.trim()
        return Pair(getTitle(fileName), getYear(fileName))
    }

    fun retrieveFileData(seriesDirName: String, file: File): Pair<String, String> {
        //ToDo :: Write more sophisticated logic to retrieve naming
        val fileName = file.nameWithoutExtension.trim()
        val matchResult = EPISODE_REGEX.matchEntire(fileName)

        if (matchResult != null) {

        val (_, _, episodeStr, episodeName) = matchResult.destructured
            return Pair(episodeStr.trim().toInt().toString(), episodeName.trim())
        }
        return Pair(fileName, "")
    }

    fun processMedia(series: Series) {
        try {

            log.info("[{}] Writing updated data to", series.directory?.absolutePath)
            if (createNfoEnabled) metaWriter.writeSeriesDataToNfo(series.directory!!, series)
            findArtwork(series, series.directory!!)
            series.seasons?.forEach { season ->
//                if (createNfoEnabled) metaWriter.writeSeasonDataToNfo(season.directory!!, series, season)
                if (season.directory == null) {
                    season.directory = File(series.directory, season.name!!)
                    season.directory?.mkdirs()
                }
                findArtwork(season, season.directory!!)
                season.episodes?.forEach { episode ->
                    val name = episode.filename(series.name!!)
                    if (episode.file == null) {
                        log.warn("[{}] File not found for episode: {}", series, episode)
                        return@forEach
                    }
                    if (episode.file!!.nameWithoutExtension.equals(name).not()) {
                        mediaOrganiser.renameEpisode(name, episode.file!!)
                    }

                    if (createNfoEnabled) metaWriter.writeEpisodeDataToNfo(season.directory!!, series, season, episode, name)
                    findArtwork(episode, name, season.directory!!)

                }
            }

            log.info("**** [{}] FINISHED!", series)
        } catch (_: NoMatchException) {
            log.error("Failed to find a match for: ${series.directory?.absolutePath}")
        } catch (ex: Exception) {
            log.error("Error while processing file: ${series.directory?.absolutePath}", ex)
        }
    }

    fun findArtwork(series: Series, artworkLocation: File) {
        if (findArtworkEnabled && (series.backdrop != null || series.poster != null)) {
            log.info("[{}] Writing additional images to: {}", series, artworkLocation.absolutePath)
            if (series.backdrop != null) {
                val backdropPath = File(artworkLocation, "backdrop.jpg").absolutePath
                Files.write(Paths.get(backdropPath), series.backdrop!!)
                log.info("[{}] Wrote backdrop to: {}", series, backdropPath)
            }

            if (series.poster != null) {
                val posterPath = File(artworkLocation, "poster.jpg").absolutePath
                Files.write(Paths.get(posterPath), series.poster!!)
                log.info("[{}] Wrote poster to: {}", series, posterPath)
            }
        }
    }

    fun findArtwork(season: Series.Season, artworkLocation: File) {
        if (findArtworkEnabled && season.poster != null) {
            log.info("[{}] Writing additional images to: {}", season, artworkLocation.absolutePath)

            if (season.poster != null) {
                val posterPath = File(artworkLocation, "poster.jpg").absolutePath
                Files.write(Paths.get(posterPath), season.poster!!)
                log.info("[{}] Wrote poster to: {}", season, posterPath)
            }
        }
    }

    fun findArtwork(episode: Series.Episode, episodeName: String, artworkLocation: File) {
        if (findArtworkEnabled && episode.still != null) {
            log.info("[{}] Writing additional images to: {}", episode, artworkLocation.absolutePath)

            val stillPath = File(artworkLocation, "$episodeName.jpg").absolutePath
            Files.write(Paths.get(stillPath), episode.still!!)
            log.info("[{}] Wrote poster to: {}", episode, stillPath)
        }
    }

    companion object {
        private val EPISODE_REGEX = """^(.+?)\s*-\s*s(\d{2})e(\d{2})\s*-\s*(.+)?$""".toRegex(RegexOption.IGNORE_CASE)
        private val SUPPORTED_VIDEO_FORMATS = listOf("mp4", "m4v", "mkv", "mov", "avi", "wmv", "mpeg", "mpg")

        private val log = LoggerFactory.getLogger(SeriesManager::class.java)
    }
}