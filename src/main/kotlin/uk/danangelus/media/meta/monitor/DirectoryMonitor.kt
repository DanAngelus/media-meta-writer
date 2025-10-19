package uk.danangelus.media.meta.monitor

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import uk.danangelus.media.meta.manager.MediaManager
import uk.danangelus.media.meta.model.MediaCfg
import uk.danangelus.media.meta.model.MediaCfg.Media
import uk.danangelus.media.meta.model.MediaMetadata
import uk.danangelus.media.meta.reader.MediaReader
import java.io.File
import java.nio.file.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.extension
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
    private val mediaReader: MediaReader,
    cfg: MediaCfg,
//    @Value("\${media.source-directory}") private val sourceDirectories: Map<String, String>,
) {

    private val executorService = Executors.newSingleThreadExecutor()
    private val dirToMedia = mutableMapOf<String, Media>()
    private val processing: AtomicBoolean = AtomicBoolean(false)

    @PostConstruct
    fun init() {
        mediaCfg.media.forEach { media ->
            dirToMedia[File(media.sourceDirectory).absolutePath] = media
        }

        fixNfoFiles("\\\\ZionMedia\\media\\Library\\Films")
//        createNfoFiles("C:\\Developer\\Projects\\DanAngelus.UK\\media-meta-writer\\films")
    }

    private fun createNfoFiles(list: String) {
        val filmsList = Paths.get(list).toFile().readText()
        filmsList.lines().forEach {
            val media = mediaCfg.media.first()
            val metadata = MediaMetadata(
                title = mediaReader.getTitle(it),
                year = mediaReader.getYear(it),
            )
            val root = File(media.destinationDirectory, metadata.filename())
            if (root.exists()) {
                return@forEach
            }

            mediaManager.populateMetadata(media, metadata)

            if (root.mkdirs()) {
                mediaManager.createNfo(media, metadata, File(root, "nothing"))
            } else {
                log.warn("WARNING ****************************************** Failed to create directory: {}", root.absolutePath)
            }
        }
    }

    private fun fixNfoFiles(directory: String) {
        val filmsDir = File(directory)
        if (!filmsDir.isDirectory) {
            return
        }

        Files.walk(filmsDir.toPath()).forEach {
            if (it.isDirectory()) {
                return@forEach
            }
            if (it.extension.equals("nfo", true)) {
                var content = it.toFile().readText()

                // General content fixes
                    .replace("</certification>", "</mpaa>")
//                    .replace("<fulldate>", "<releasedate>")
//                    .replace("</fulldate>", "</releasedate>")
//                    .replace(" minutes</runtime>", "</runtime>")
//                    .replace("<logo>backdrop.png</logo>", "<backdrop>backdrop.jpg</backdrop>")
//                    .replace("<backdrop>backdrop.png</backdrop>", "<backdrop>backdrop.jpg</backdrop>")
//                    .replace("<logo>poster.png</logo>", "<poster>poster.jpg</poster>")
//                    .replace("<poster>poster.png</poster>", "<poster>poster.jpg</poster>")
//                    .replace("&", "&amp;")
//                log.info("Fixed NFO file: ${it.toFile().absolutePath}:\n$content")

                // Fix for genres being grouped in a single tag
//                val genres = content.lines()
//                    .filter { it.contains("<genre>") }
//                    .joinToString()
//                    .trim()
//                val genresFixed = genres.trim()
//                    .removePrefix("<genre>")
//                    .removeSuffix("</genre>")
//                    .split(",")
//                    .toSet()
//                    .joinToString("\n  ") { "<genre>$it</genre>" }
//                content = content.replace(genres, genresFixed)
//                log.info("Fixed genres in NFO file from:\n$genres \n$genresFixed")

                Files.write(it, content.toByteArray())
            }
        }
    }

    /**
     * Monitors directories from the configuration and processes new files.
     */
//    @Scheduled(initialDelay = 3000, fixedDelay = Long.MAX_VALUE)
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
        if (processing.get()) {
            return
        }
        processing.set(true)
        if (directory.exists() && directory.isDirectory) {
            log.debug("Processing all files in directory: ${directory.absolutePath}")

            val allFiles = directory.listFiles()
                ?.filter { file -> file.isFile && SUPPORTED_VIDEO_FORMATS.contains(file.extension) }

            if (allFiles.isNullOrEmpty()) {
                return
            }

            val files = if (allFiles.size < MAX_THRESHOLD) {
                allFiles
            } else {
                allFiles.subList(0, MAX_THRESHOLD - 1)
            }

            // Process one batch at a time
            files.parallelStream()
                .forEach { file ->
                    log.debug("Processing file: ${file.absolutePath}")
                    mediaManager.registerMedia(media, file)
                }

            log.info("Finished batch: {} from: {}", files.size, directory.absolutePath)
            // Keep checking for new files
            processing.set(false)
            processAllFilesInDirectory(media, directory)
        } else {
            log.warn("Path is not a valid directory: ${directory.absolutePath}")
        }
        log.info("***** FINISHED processing available files *****")
        processing.set(false)
    }

    /**
     * Shutdown the monitoring service gracefully.
     */
    fun shutdown() {
        executorService.shutdownNow()
        log.debug("Directory monitoring service has been stopped.")
    }

    companion object {
        private val MAX_THRESHOLD = 20
        private val SUPPORTED_VIDEO_FORMATS = listOf("mp4", "m4v", "mkv", "mov", "avi", "wmv", "mpeg", "mpg")
        private val log = LoggerFactory.getLogger(DirectoryMonitor::class.java)
    }
}