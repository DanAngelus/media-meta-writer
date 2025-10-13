package uk.danangelus.media.meta.manager

import org.mp4parser.tools.Path
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
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
) {

    fun registerMedia(media: Media, file: File) {
        val metadata = mediaReader.readFile(media, file)
        if (metadata?.title == null) {
            log.warn("Skipping file: ${file.absolutePath} as title is no metadata can be found.")
            return
        }
        log.info("Retrieving updated information from TMDB for: {}", metadata.title)

        tmdbService.findMovie(metadata)
        log.info("Retrieved! {}", metadata)

        log.info("Writing updated data to: {}", file.absolutePath)
        if (metaWriter.writeData(media, file, metadata)) {
            log.info("Moving file: {} to new location: {}", file.absolutePath, media.destinationDirectory)
            val newLocation = mediaOrganiser.organise(media, metadata, file)
            if (newLocation != null) {
                metaWriter.writeNfoFile(media, newLocation, metadata)

                if (metadata.backdrop != null) {
                    val backdropPath = File(newLocation.parentFile, "backdrop.jpg").absolutePath
                    Files.write(Paths.get(backdropPath), metadata.backdrop!!)
                    log.info("Wrote backdrop to: {}", backdropPath)
                }

                if (metadata.logo != null) {
                    val logoPath = File(newLocation.parentFile, "logo.jpg").absolutePath
                    Files.write(Paths.get(logoPath), metadata.logo!!)
                    log.info("Wrote logo to: {}", logoPath)
                }

                if (metadata.poster != null) {
                    val posterPath = File(newLocation.parentFile, "poster.jpg").absolutePath
                    Files.write(Paths.get(posterPath), metadata.poster!!)
                    log.info("Wrote poster to: {}", posterPath)
                }
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(MediaManager::class.java)
    }
}