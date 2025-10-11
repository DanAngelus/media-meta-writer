package uk.danangelus.media.meta.reader

import org.apache.tika.Tika
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.parser.ParseContext
import org.apache.tika.sax.BodyContentHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.danangelus.media.meta.model.MediaMetadata
import uk.danangelus.media.meta.monitor.DirectoryMonitor
import uk.danangelus.media.meta.writer.MetaWriter
import java.io.File
import java.io.FileInputStream



/**
 * Reads file attributes and then calls the iMDB API to retrieve full details.
 *
 * @author Dan Bennett
 */
@Service
class MediaReader(
    private val metaWriter: MetaWriter
) {

    private val tika = Tika()
    private val parser = AutoDetectParser()
    private val context = ParseContext()

    fun readFile(file: File) {
        if (!file.exists() || !file.isFile) {
            log.warn("File does not exist or is not a valid file: ${file.absolutePath}")
            return
        }

        try {
            val handler = BodyContentHandler()
            val metadataHandle = Metadata() //empty metadata object
            val inputstream = FileInputStream(file)

            log.info("Detected file: {} -> {}", file.name, tika.detect(file))

            inputstream.channel.position(0)
            parser.parse(inputstream, handler, metadataHandle, context)

            // Read relevant metadata
            val metadata = MediaMetadata(
                title = metadataHandle.get("dc:title") ?: getTitle(file.nameWithoutExtension),
                description = metadataHandle.get("dc:description") ?: file.nameWithoutExtension,
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

            // Log extracted metadata
            log.info("Extracted metadata from file: {}\n{}", file.absolutePath, metadata)

            inputstream.close()

            metaWriter.writeData(file, metadata)

        } catch (ex: Exception) {
            log.error("Error while extracting metadata from file: ${file.absolutePath}", ex)
        }
    }

    fun getTitle(fileName: String): String {
        return fileName.split("(").first().trim()
    }

    fun getYear(fileName: String): String {
        return fileName.split("[(|)]").last().trim()
    }

    companion object {
        private val log = LoggerFactory.getLogger(DirectoryMonitor::class.java)
    }
}