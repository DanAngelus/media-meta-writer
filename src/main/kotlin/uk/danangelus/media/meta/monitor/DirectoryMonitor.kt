package uk.danangelus.media.meta.monitor

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import uk.danangelus.media.meta.reader.MediaReader
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
    private val mediaReader: MediaReader,
    private val sourceDirectories: Map<String, String> = mapOf("films" to "C:/Users/me/Media/_drop/Films"),
//    @Value("\${media.source-directory}") private val sourceDirectories: Map<String, String>,
) {

    private val executorService = Executors.newSingleThreadExecutor()

    /**
     * Monitors directories from the configuration and processes new files.
     */
    @Scheduled(fixedDelay = 1000)
    fun monitor() {
        // Ensure configuration is valid
        if (sourceDirectories.isEmpty()) {
            throw IllegalArgumentException("No source directories provided for monitoring in configuration.")
        }

        executorService.submit {
            val watchService = FileSystems.getDefault().newWatchService()

            // Register directories in watch service
            for ((key, dirPath) in sourceDirectories) {
                val path = Paths.get(dirPath)

                if (!path.isDirectory()) {
                    throw IllegalArgumentException("Path '$dirPath' (key: $key) is not a valid directory.")
                }

                // Pre-process existing files in directory
                processAllFilesInDirectory(path.toFile())

                path.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE // Listen specifically for new files
                )

                log.info("Started monitoring directory: $dirPath")
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
                        processAllFilesInDirectory(parentDirectory)
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
    private fun processAllFilesInDirectory(directory: File) {
        if (directory.exists() && directory.isDirectory) {
            log.debug("Processing all files in directory: ${directory.absolutePath}")

            directory.listFiles()?.forEach { file ->
                if (file.isFile) {
                    log.debug("Processing file: ${file.absolutePath}")
                    mediaReader.readFile(file)
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
        private val log = LoggerFactory.getLogger(DirectoryMonitor::class.java)
    }
}