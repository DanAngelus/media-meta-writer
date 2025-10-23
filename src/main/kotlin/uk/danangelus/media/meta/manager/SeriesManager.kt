package uk.danangelus.media.meta.manager

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.danangelus.media.meta.error.NoMatchException
import uk.danangelus.media.meta.model.MediaCfg.Media
import uk.danangelus.media.meta.model.MediaMetadata
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
    @Value("\${media.enable.create-nfo}") private val createNfoEnabled: Boolean,
    @Value("\${media.enable.find-artwork}") private val findArtworkEnabled: Boolean,
) {

    fun register(media: Media, file: File) {
        try {
            val (seriesName, seriesYear) = retrieveFileData(file)
            if (seriesName == null || seriesYear == null) return
            val series = getSeriesMetadata(seriesName, seriesYear)
            if (series == null) return

            log.info("[{}] Retrieving series information from TMDB", series)
            Files.list(file.toPath()).forEach { seasonDir ->
                val seasonNumber = seasonDir.fileName.toString().substringAfter("Season ").trim()
                val season = getSeasonMetadata(series, seasonNumber)
                Files.list(seasonDir).forEach {
                    val file = it.toFile()

                    val (episodeNumber, title) = retrieveFileData(seasonDir.fileName.toString(), file)
                    val episode = getEpisodeMetadata(series, season, episodeNumber, title)
                    // ToDo :: Find top level series names and search tmdb
                    processMedia(
                        series,
                        season,
                        episode,
                        file
                    )
                    // ToDo :: Find season numbers
                    // ToDo :: Find episode name/number
                    // ToDo :: Search TMDB for episode details
                    // ToDo :: Create .nfo file in TV format
                    log.info("[{}] Retrieving updated information from TMDB", series ?: "Unknown")
                }
            }
            //            processMedia(media, metadata, file)
        } catch (ex: Exception) {
            log.error("Error while processing file: ${file.absolutePath}", ex)
            mediaOrganiser.moveToError(media, file)
        }
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

    fun processMedia(
        series: Series,
        season: Series.Season,
        episode: Series.Episode,
        file: File,
    ) {
        try {

            log.info("[{}] Writing updated data to", file.absolutePath)
//            if (metaWriter.writeData(media, file, metadata)) {
//                log.info("[{}] Moving file: {} to new location: {}", metadata, file.absolutePath, media.destinationDirectory)
//                val newLocation = mediaOrganiser.organise(media, metadata, file)
//                if (newLocation != null) {
//                    log.info("[{}] Video file moved to new location: {}", metadata, newLocation.absolutePath)
//
//                    createNfo(media, metadata, newLocation)
//                    findArtwork(metadata, newLocation)
//                }
                log.info("**** [{}] FINISHED!", series)
//            }
        } catch (_: NoMatchException) {
//            mediaOrganiser.moveToNoMatch(series, file)
        } catch (ex: Exception) {
            log.error("Error while processing file: ${file.absolutePath}", ex)
//            mediaOrganiser.moveToError(media, file)
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
        return tmdbService.getSeason(series.id.toString(), series.name!!, season.seasonNumber!!)
            ?: throw NoMatchException("No matching season found for: $seasonNumber")
    }

    fun getEpisodeMetadata(series: Series, season: Series.Season, episodeNumber: String, title: String): Series.Episode {
        val episode = season.episodes?.firstOrNull { it.episodeNumber.equals(episodeNumber, true) }

        log.info("[{} - {}] Found matching episode: {}", series, season, episode)
        return episode ?: throw NoMatchException("No matching season found for: $episodeNumber")
    }

    fun createNfo(media: Media, metadata: MediaMetadata, newLocation: File) {
        if (createNfoEnabled) {
            metaWriter.writeNfoFile(media, newLocation, metadata)
        }
    }

    fun findArtwork(metadata: MediaMetadata, newLocation: File) {
        if (findArtworkEnabled && metadata.backdrop != null || metadata.logo != null || metadata.poster != null) {
            val artworkLocation = newLocation.parentFile

            log.info("[{}] Writing additional images to: {}", metadata, artworkLocation.absolutePath)
            if (metadata.backdrop != null) {
                val backdropPath = File(artworkLocation, "backdrop.jpg").absolutePath
                Files.write(Paths.get(backdropPath), metadata.backdrop!!)
                log.info("[{}] Wrote backdrop to: {}", metadata, backdropPath)
            }

            if (metadata.logo != null) {
                val logoPath = File(artworkLocation, "logo.jpg").absolutePath
                Files.write(Paths.get(logoPath), metadata.logo!!)
                log.info("[{}] Wrote logo to: {}", metadata, logoPath)
            }

            if (metadata.poster != null) {
                val posterPath = File(artworkLocation, "poster.jpg").absolutePath
                Files.write(Paths.get(posterPath), metadata.poster!!)
                log.info("[{}] Wrote poster to: {}", metadata, posterPath)
            }
        }
    }

    fun getTitle(fileName: String): String? {
        return fileName.split("(").first().trim().ifBlank { null }
    }

    fun getYear(fileName: String): String? {
        return fileName.substringAfter("(").substringBefore(")").trim().ifBlank { null }
    }

    companion object {
        private val EPISODE_REGEX = """^(.+?)\s*-\s*s(\d{2})e(\d{2})\s*-\s*(.+)?$""".toRegex(RegexOption.IGNORE_CASE)

        private val log = LoggerFactory.getLogger(SeriesManager::class.java)
    }
}