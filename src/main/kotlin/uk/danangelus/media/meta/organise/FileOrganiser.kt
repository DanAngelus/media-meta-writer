package uk.danangelus.media.meta.organise

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.danangelus.media.meta.model.MediaCfg
import uk.danangelus.media.meta.model.MediaMetadata
import uk.danangelus.media.meta.model.MediaType.FILM
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.name

/**
 * Moves files to organised folders once metadata has been applied.
 *
 * @author Dan Bennett
 */
@Service
class FileOrganiser(
    @Value("\${media.enable.move-files}") private val movingFilesEnabled: Boolean,
    @Value("\${media.enable.move-video}") private val movingVideoEnabled: Boolean,
) {

    fun organise(media: MediaCfg.Media, metadata: MediaMetadata, file: File): File? {
        log.debug("[{}] Organising file: {}", metadata, file.absolutePath)

        if (media.type != FILM) {
            log.warn("[{}] Unsupported file type: $media. Skipping file: ${file.absolutePath}", metadata)
            return null
        }

        val name = metadata.filename()

        val dest = File(media.destinationDirectory, name)
        return if (dest.exists() || dest.mkdirs()) {
            val newFile = File(dest, "${name}.${file.extension}")
            if (movingVideoEnabled) {
                log.debug("[{}] Moving file: ${file.absolutePath} to: ${dest.absolutePath}", metadata)
                Files.move(file.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                log.debug("[{}] File moved successfully: ${file.absolutePath} to: ${dest.absolutePath}", metadata)
            }
            if (movingFilesEnabled) {
                log.debug("[{}] Moving similar files: ${file.absolutePath} to: ${dest.absolutePath}", metadata)
                Files.list(file.parentFile.toPath())
                    .filter { it.fileName.toString().startsWith("${file.nameWithoutExtension}.") }
                    .forEach {
                        try {
                            val dest = File(dest, it.name.replace(file.nameWithoutExtension, name))
                            Files.move(it, dest.toPath())
                            log.debug("[{}] File moved successfully: ${it.toFile().absolutePath} to: ${dest.absolutePath}", metadata)
                        } catch (ex: Exception) {
                            log.error("[{}] Failed to move file: ${it.toFile().absolutePath}", metadata, ex)
                        }
                    }
            }
            newFile
        } else {
            log.warn("[{}] Failed to create directory: ${dest.absolutePath}", metadata)
            null
        }
    }

    fun moveToNoMatch(mediaCfg: MediaCfg.Media, file: File) {
        if (movingFilesEnabled) {
            Files.move(file.toPath(), File(mediaCfg.nomatchDirectory, file.name).toPath())
        }
    }

    fun moveToError(mediaCfg: MediaCfg.Media, file: File) {
        if (movingFilesEnabled) {
            Files.move(file.toPath(), File(mediaCfg.errorDirectory, file.name).toPath())
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(FileOrganiser::class.java)
    }
}