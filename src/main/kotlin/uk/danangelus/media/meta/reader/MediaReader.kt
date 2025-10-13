package uk.danangelus.media.meta.reader

import org.apache.tika.Tika
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.parser.ParseContext
import org.apache.tika.sax.BodyContentHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.danangelus.media.meta.model.MediaCfg
import uk.danangelus.media.meta.model.MediaMetadata
import uk.danangelus.media.meta.monitor.DirectoryMonitor
import java.io.File
import java.io.FileInputStream



/**
 * Reads file attributes and then calls the iMDB API to retrieve full details.
 *
 * @author Dan Bennett
 */
@Service
class MediaReader() {

    private val tika = Tika()
    private val parser = AutoDetectParser()
    private val context = ParseContext()

    fun readFile(media: MediaCfg.Media, file: File): MediaMetadata? {
        if (!file.exists() || !file.isFile) {
            log.warn("File does not exist or is not a valid file: ${file.absolutePath}")
            return null
        }

        val metadataHandle = Metadata() //empty metadata object
        val inputstream = FileInputStream(file)
        try {
            val handler = BodyContentHandler()

            log.info("Detected file: {} -> {}", file.name, tika.detect(file))

            inputstream.channel.position(0)
            parser.parse(inputstream, handler, metadataHandle, context)

            // ToDo :: Handle TV shows episodes in the same way


        } catch (ex: Exception) {
            log.error("Error while extracting metadata from file: ${file.absolutePath}", ex)
        } finally {
            inputstream.close()
        }

        // Read relevant metadata
        val metadata = MediaMetadata(
            title = metadataHandle.get("dc:title") ?: getTitle(file.nameWithoutExtension),
            plot = metadataHandle.get("dc:description"),
            rating = metadataHandle.get("xmp:Rating") ?: "Unknown",
            length = metadataHandle.get("xmpDM:duration")?.toDoubleOrNull()?.let { "${Math.ceil(it / 60).toInt()} minutes" },
            resolution = metadataHandle.get("tiff:ImageWidth")
                ?.let { width ->
                    metadataHandle.get("tiff:ImageLength")
                        ?.let { height -> "${width}x${height}" }
                } ?: "Unknown",
            releaseDate = metadataHandle["xmpDM:releaseDate"] ?: "Unknown",
            year = metadataHandle["xmpDM:releaseDate"]?.take(4) ?: getYear(file.nameWithoutExtension),
        )
        log.info("Extracted metadata from file: {}\n{}", file.absolutePath, metadata)
        return metadata
    }

    fun getTitle(fileName: String): String {
        return fileName.split("(").first().trim()
    }

    fun getYear(fileName: String): String {
        return fileName.substringAfter("(").substringBefore(")").trim()
    }

    companion object {
        private val log = LoggerFactory.getLogger(DirectoryMonitor::class.java)
    }
}