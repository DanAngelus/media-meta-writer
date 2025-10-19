package uk.danangelus.media.meta

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import uk.danangelus.media.meta.model.MediaCfg
import java.nio.file.Files
import java.nio.file.Paths

/**
 * @author Dan Bennett
 */
@Service
class MediaChecker(
    val mediaCfg: MediaCfg,
) {

//    @PostConstruct
    fun checkForMissingDetails() {
        val mediaDirectory = Paths.get(mediaCfg.media.first().destinationDirectory)
        Files.walk(mediaDirectory)
            .filter { Files.isDirectory(it) && it != mediaDirectory }
            .forEach { filmDir ->
                val existingFiles = Files.list(filmDir).map { it.fileName.toString() }.toList()
                val missingFiles = KEY_FILES.filterNot { it in existingFiles }

                if (missingFiles.isNotEmpty() && missingFiles.size == KEY_FILES.size) {
                    println("Warning: The directory '${filmDir.fileName}' is missing files: $missingFiles")
                }
            }
    }

    companion object {
        private val KEY_FILES = listOf("poster.jpg", "backdrop.jpg", "logo.jpg")
        private val log = org.slf4j.LoggerFactory.getLogger(MediaChecker::class.java)
    }
}