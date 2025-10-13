package uk.danangelus.media.meta.organise

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.danangelus.media.meta.model.MediaCfg
import uk.danangelus.media.meta.model.MediaMetadata
import uk.danangelus.media.meta.model.MediaType.FILM
import java.io.File
import java.nio.file.Files

/**
 * Moves files to organised folders once metadata has been applied.
 *
 * @author Dan Bennett
 */
@Service
class FileOrganiser {

    fun organise(media: MediaCfg.Media, metadata: MediaMetadata, file: File): File? {
        log.debug("Organising file: {}", file.absolutePath)

        if (media.type != FILM) {
            log.warn("Unsupported file type: $media. Skipping file: ${file.absolutePath}")
            return null
        }

        val name = if (metadata.title.isNullOrBlank().not()) {
            "${metadata.title} (${metadata.year})" // ToDo :: add resolution X
        } else {
            file.nameWithoutExtension
        }

        val dest = File(media.destinationDirectory, name)
        return if (dest.exists() || dest.mkdirs()) {
            log.debug("Moving file: ${file.absolutePath} to: ${dest.absolutePath}")
            val newFile = File(dest, "${name}.${file.extension}")
            Files.move(file.toPath(), newFile.toPath())
            log.debug("File moved successfully: ${file.absolutePath} to: ${dest.absolutePath}")
            newFile
        } else {
            log.warn("Failed to create directory: ${dest.absolutePath}")
            null
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(FileOrganiser::class.java)
    }
}