package uk.danangelus.media.meta.manager

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.danangelus.media.meta.error.NoMatchException
import uk.danangelus.media.meta.model.MediaCfg.Media
import uk.danangelus.media.meta.model.MediaMetadata
import uk.danangelus.media.meta.model.MediaType
import uk.danangelus.media.meta.organise.FileOrganiser
import uk.danangelus.media.meta.reader.MediaReader
import uk.danangelus.media.meta.services.TMDBService
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
class FilmManager(
    private val mediaReader: MediaReader,
    private val mediaOrganiser: FileOrganiser,
    private val metaWriter: MetaWriter,
    private val tmdbService: TMDBService,
    @Value("\${media.enable.create-nfo}") private val createNfoEnabled: Boolean,
    @Value("\${media.enable.find-artwork}") private val findArtworkEnabled: Boolean,
) {

    fun register(media: Media, file: File) {
        try {
            val metadata = mediaReader.readFile(media, file)
            if (metadata?.title == null) {
                log.warn("Skipping file: ${file.absolutePath} as title is no metadata can be found.")
                return
            }
            log.info("[{}] Retrieving updated information from TMDB", metadata)
            processMedia(media, metadata, file)
        } catch (ex: Exception) {
            log.error("Error while processing file: ${file.absolutePath}", ex)
            mediaOrganiser.moveToError(media, file)
        }
    }

    fun processMedia(media: Media, metadata: MediaMetadata, file: File) {
        try {
            populateMetadata(media, metadata)

            log.info("[{}] Writing updated data to", file.absolutePath)
            if (metaWriter.writeData(media, file, metadata)) {
                log.info("[{}] Moving file: {} to new location: {}", metadata, file.absolutePath, media.destinationDirectory)
                val newLocation = mediaOrganiser.organise(media, metadata, file)
                if (newLocation != null) {
                    log.info("[{}] Video file moved to new location: {}", metadata, newLocation.absolutePath)

                    createNfo(media, metadata, newLocation)
                    findArtwork(metadata, newLocation)
                }
                log.info("**** [{}] FINISHED!", metadata)
            }
        } catch (_: NoMatchException) {
            mediaOrganiser.moveToNoMatch(media, file)
        } catch (ex: Exception) {
            log.error("Error while processing file: ${file.absolutePath}", ex)
            mediaOrganiser.moveToError(media, file)
        }
    }

    fun populateMetadata(media: Media, metadata: MediaMetadata) {
        if (media.type == MediaType.FILM) {
            tmdbService.populateMovieDetails(metadata)
            log.info("[{}] Retrieved data from TMDB", metadata)
        }
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

    companion object {
        private val log = LoggerFactory.getLogger(FilmManager::class.java)
    }
}