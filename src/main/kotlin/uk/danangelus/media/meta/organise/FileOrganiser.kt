package uk.danangelus.media.meta.organise

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.danangelus.media.meta.model.MediaMetadata
import java.io.File

/**
 * Moves files to organised folders once metadata has been applied.
 *
 * @author Dan Bennett
 */
@Service
class FileOrganiser {

    fun organise(file: File, metadata: MediaMetadata) {
        log.debug("Organising file: {}", file.absolutePath)

        // ToDo :: Move files to organised folders
    }

    companion object {
        private val log = LoggerFactory.getLogger(FileOrganiser::class.java)
    }
}