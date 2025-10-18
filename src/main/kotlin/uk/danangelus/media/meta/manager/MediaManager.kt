package uk.danangelus.media.meta.manager

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.danangelus.media.meta.error.NoMatchException
import uk.danangelus.media.meta.model.MediaCfg.Media
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
class MediaManager(
    private val mediaReader: MediaReader,
    private val mediaOrganiser: FileOrganiser,
    private val metaWriter: MetaWriter,
    private val tmdbService: TMDBService,
    @Value("\${media.writing-enabled:true}") private val writingEnabled: Boolean,
) {

    fun registerMedia(media: Media, file: File) {
        try {
            val metadata = mediaReader.readFile(media, file)
            if (metadata?.title == null) {
                log.warn("Skipping file: ${file.absolutePath} as title is no metadata can be found.")
                return
            }
            log.info("[{}] Retrieving updated information from TMDB", metadata)

            try {
                tmdbService.populateMovieDetails(metadata)
                log.info("[{}] Retrieved data from TMDB", metadata)
            } catch (_: NoMatchException) {
                mediaOrganiser.moveToNoMatch(media, file)
                return
            } catch (_: Exception) {
                log.warn("Error for: {}", metadata)
                mediaOrganiser.moveToError(media, file)
                return
            }

            log.info("[{}] Writing updated data to: {}", file.absolutePath)
            if (writingEnabled && metaWriter.writeData(media, file, metadata)) {
                log.info("[{}] Moving file: {} to new location: {}", metadata, file.absolutePath, media.destinationDirectory)
                val newLocation = mediaOrganiser.organise(media, metadata, file)
                if (newLocation != null) {
                    log.info("[{}] Video file moved to new location: {}", metadata, newLocation.absolutePath)
                    metaWriter.writeNfoFile(media, newLocation, metadata)

                    if (metadata.backdrop != null || metadata.logo != null || metadata.poster != null) {
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
                log.info("**** [{}] FINISHED!", metadata)
            }
        } catch (ex: Exception) {
            log.error("Error while processing file: ${file.absolutePath}", ex)
            mediaOrganiser.moveToError(media, file)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(MediaManager::class.java)
    }
}