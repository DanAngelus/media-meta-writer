package uk.danangelus.media.meta.writer

import org.mp4parser.IsoFile
import org.mp4parser.boxes.iso14496.part12.MetaBox
import org.mp4parser.boxes.iso14496.part12.UserDataBox
import org.mp4parser.boxes.iso14496.part12.XmlBox
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.danangelus.media.meta.model.MediaMetadata
import uk.danangelus.media.meta.monitor.DirectoryMonitor
import uk.danangelus.media.meta.services.TMDBService
import java.io.File
import java.io.FileOutputStream

/**
 * Writes metadata into media files and optionally writes an accompanying NFO file.
 * .
 * @author Dan Bennett
 */
@Service
class MetaWriter(
    private val tmdbService: TMDBService,
) {

    fun writeData(file: File, metadata: MediaMetadata) {
        log.debug("Starting to process metadata for file: ${file.absolutePath}")

        // Fetch additional metadata from TMDb
        tmdbService.findMovie(metadata)

        try {
            // Write metadata into MP4 file
            if (file.extension == "mp4") {
                writeMp4Metadata(file, metadata)
            } else {
                log.warn("Unsupported file extension: ${file.extension}. Skipping metadata writing for ${file.name}")
            }

            // Optionally generate an accompanying NFO file
            writeNfoFile(file, metadata)

        } catch (ex: Exception) {
            log.error("Error while writing metadata for file: ${file.absolutePath}", ex)
        }
    }

    private fun writeMp4Metadata(file: File, metadata: MediaMetadata) {
        try {
            val isoFile = IsoFile(file)

            // Locate or create the MovieBox (moov)
            val movieBox = isoFile.movieBox ?: throw IllegalStateException("No MovieBox found in file ${file.name}")

            // Locate or create the UserDataBox (udta)
            val userDataBox = movieBox.getBoxes(UserDataBox::class.java).firstOrNull()
                ?: UserDataBox().also { movieBox.addBox(it) }

            // Create a MetaBox
            val metaBox = MetaBox()
            val xmlBox = XmlBox()

            // Format metadata as XML (required by some players)
            xmlBox.xml = buildXmlFromMetadata(metadata)
            metaBox.addBox(xmlBox)

            // Add the MetaBox to the UserDataBox
            userDataBox.addBox(metaBox)

            // Write updated MP4 container to the file
            val outputStream = FileOutputStream(file)
            isoFile.writeContainer(outputStream.channel)
            outputStream.close()
            isoFile.close()

            log.info("Successfully added metadata to file: ${file.name}")

        } catch (ex: Exception) {
            log.error("Failed to write metadata to MP4 file: ${file.absolutePath}", ex)
        }
    }

    private fun writeNfoFile(file: File, metadata: MediaMetadata) {
        val nfoFile = File(file.parentFile, "${file.nameWithoutExtension}.nfo")
        try {
            nfoFile.printWriter().use { writer ->
                writer.println("<movie>")
                writer.append("<createdby>MediaMetaWriter by DanAngelus</createdby>")
                metadata.title?.let { writer.append("<title>$it</title>") }
                metadata.title?.let { writer.append("<originaltitle>$it</originaltitle>") }
                metadata.plot?.let { writer.append("<plot>$it</plot>") }
                metadata.outline?.let { writer.append("<outline>$it</outline>") }
                metadata.length?.let { writer.append("<runtime>$it</runtime>") }
                metadata.year?.let { writer.append("<year>$it</year>") }
                metadata.releaseDate?.let { writer.append("<fulldate>$it</fulldate>") }
                metadata.rating?.let { writer.append("<rating>$it</rating>") }
                metadata.studio?.let { writer.append("<studio>$it</studio>") }
                metadata.director?.let { writer.append("<director>$it</director>") }
                metadata.genre?.let { writer.append("<genre>$it</genre>") }
                metadata.actors?.forEach { writer.append("<actor><name>$it</name></actor>") }
                metadata.tmdbId?.let { writer.append("<uniqueid type=\"tmdb\" default=\"true\">$it</uniqueid>") }
                writer.println("</movie>")
            }
            log.info("Successfully wrote NFO file: ${nfoFile.absolutePath}")
        } catch (ex: Exception) {
            log.error("Failed to write NFO file for: ${file.absolutePath}", ex)
        }
    }

    /**
     * Formats metadata into XML for compatibility with `UserDataBox` and `MetaBox`.
     */
    private fun buildXmlFromMetadata(metadata: MediaMetadata): String {
        val builder = StringBuilder()
        builder.append("<metadata>")
        metadata.title?.let { builder.append("<title>$it</title>") }
        metadata.year?.let { builder.append("<year>$it</year>") }
        metadata.rating?.let { builder.append("<rating>$it</rating>") }
        metadata.tmdbId?.let { builder.append("<tmdbId>$it</tmdbId>") }
        builder.append("</metadata>")
        return builder.toString()
    }

    companion object {
        private val log = LoggerFactory.getLogger(DirectoryMonitor::class.java)
    }
}