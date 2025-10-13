package uk.danangelus.media.meta.monitor

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import uk.danangelus.media.meta.manager.MediaManager
import uk.danangelus.media.meta.model.MediaCfg
import uk.danangelus.media.meta.model.MediaCfg.Media
import java.io.File
import java.nio.file.*
import java.util.concurrent.Executors
import kotlin.io.path.isDirectory

/**
 * Monitors a given directory for new files.
 *
 * @author Dan Bennett
 */
@Service
class DirectoryMonitor(
    private val mediaManager: MediaManager,
    private val mediaCfg: MediaCfg,
//    @Value("\${media.source-directory}") private val sourceDirectories: Map<String, String>,
) {

    private val executorService = Executors.newSingleThreadExecutor()
    private val dirToMedia = mutableMapOf<String, Media>()

    @PostConstruct
    fun init() {
        mediaCfg.media.forEach { media ->
            dirToMedia[File(media.sourceDirectory).absolutePath] = media
        }
    }

    /**
     * Monitors directories from the configuration and processes new files.
     */
    @Scheduled(initialDelay = 2000)
    fun monitor() {
        // Ensure configuration is valid
        if (mediaCfg.media.isEmpty()) {
            throw IllegalArgumentException("No media provided for monitoring")
        }

        executorService.submit {
            val watchService = FileSystems.getDefault().newWatchService()

            for (media in mediaCfg.media) {
                val path = Paths.get(media.sourceDirectory)

                if (!path.isDirectory()) {
                    throw IllegalArgumentException("Path '${media.sourceDirectory}' (key: ${media.type}) is not a valid directory.")
                }

                // Pre-process existing files in directory
                processAllFilesInDirectory(media, path.toFile())

                // Listen specifically for new files
                path.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE
                )
                log.info("Started monitoring directory: {}", media.sourceDirectory)
            }

            // Start the watch service loop
            while (true) {
                val watchKey = watchService.take()

                watchKey.pollEvents().forEach { event ->
                    val kind = event.kind()

                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        val directoryPath = watchKey.watchable() as Path
                        val newFileName = event.context() as Path
                        val parentDirectory = directoryPath.resolve(newFileName).parent.toFile()

                        // Process the new file
                        processAllFilesInDirectory(dirToMedia[parentDirectory.absolutePath]!!, parentDirectory)
                    }
                }

                // Reset watch key to continue receiving events
                if (!watchKey.reset()) {
                    log.warn("Directory monitoring for some directories has been stopped due to errors.")
                    break
                }
            }
        }
    }

    /**
     * Processes all files in the given directory when a new file is detected.
     */
    private fun processAllFilesInDirectory(media: Media, directory: File) {
        if (directory.exists() && directory.isDirectory) {
            log.debug("Processing all files in directory: ${directory.absolutePath}")

            directory.listFiles()?.forEach { file ->
                if (file.isFile && SUPPORTED_VIDEO_FORMATS.contains(file.extension)) {
                    log.debug("Processing file: ${file.absolutePath}")
                    mediaManager.registerMedia(media, file)
                }
            }
        } else {
            log.warn("Path is not a valid directory: ${directory.absolutePath}")
        }
    }

    /**
     * Shutdown the monitoring service gracefully.
     */
    fun shutdown() {
        executorService.shutdownNow()
        log.debug("Directory monitoring service has been stopped.")
    }

    companion object {
        private val SUPPORTED_VIDEO_FORMATS = listOf("mp4", "m4v", "mkv", "mov", "avi", "wmv", "mpeg", "mpg")
        private val log = LoggerFactory.getLogger(DirectoryMonitor::class.java)
    }
}