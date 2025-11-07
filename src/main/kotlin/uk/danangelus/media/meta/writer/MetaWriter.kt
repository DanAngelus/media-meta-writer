package uk.danangelus.media.meta.writer

import org.mp4parser.IsoFile
import org.mp4parser.boxes.iso14496.part12.MetaBox
import org.mp4parser.boxes.iso14496.part12.UserDataBox
import org.mp4parser.boxes.iso14496.part12.XmlBox
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.danangelus.media.meta.model.MediaCfg.Media
import uk.danangelus.media.meta.model.MediaMetadata
import uk.danangelus.media.meta.model.MediaType
import uk.danangelus.media.meta.monitor.DirectoryMonitor
import uk.danangelus.media.meta.services.model.Series
import java.io.File
import java.io.FileOutputStream

/**
 * Writes metadata into media files and optionally writes an accompanying NFO file.
 * .
 * @author Dan Bennett
 */
@Service
class MetaWriter(
    @Value("\${media.enable.write-to-mp4}") private val writingToMp4Enabled: Boolean,
    @Value("\${media.enable.write-to-mkv}") private val writingToMkvEnabled: Boolean,
) {

    fun writeData(media: Media, file: File, metadata: MediaMetadata): Boolean {
        log.debug("Starting to process metadata for file: ${file.absolutePath}")

        return try {
            // Write metadata into MP4 file
            if (writingToMp4Enabled && SUPPORTED_MP4_FORMATS.contains(file.extension)) {
                writeMp4Metadata(file, metadata)
            } else if (writingToMkvEnabled && SUPPORTED_MKV_FORMATS.contains(file.extension)) {
                writeMkvMetadata(file, metadata)
            } else {
                log.warn("[{}] Unsupported file extension: ${file.extension}. Skipping metadata writing for ${file.name}", metadata)
            }
            true

        } catch (ex: Exception) {
            log.error("Error while writing metadata for file: ${file.absolutePath}", ex)
            false
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

            log.info("[{}] Successfully added metadata to file: ${file.name}", metadata)

        } catch (ex: Exception) {
            log.error("Failed to write metadata to MP4 file: ${file.absolutePath}", ex)
        }
    }

    /**
     * Formats metadata into XML for compatibility with `UserDataBox` and `MetaBox`.
     */
    private fun buildXmlFromMetadata(metadata: MediaMetadata): String {
        val builder = StringBuilder()
        builder.append("<metadata>")
        metadata.title?.let { builder.append("<title>$it</title>") }
        metadata.plot?.let { builder.append("<description>$it</description>") }
        metadata.length?.let { builder.append("<duration>$it</duration>") }
        metadata.releaseDate?.let { builder.append("<releaseDate>$it</releaseDate>") }
        metadata.year?.let { builder.append("<year>$it</year>") }
        metadata.rating?.let { builder.append("<rating>$it</rating>") }
        metadata.tmdbId?.let { builder.append("<tmdbId>$it</tmdbId>") }
        builder.append("</metadata>")
        return builder.toString()
    }

    private fun writeMkvMetadata(file: File, metadata: MediaMetadata) {
        try {
            // Check if the file exists
            if (!file.exists()) {
                log.warn("File does not exist: ${file.absolutePath}")
                return
            }

            log.info("MKV NOT SUPPORTED: ${file.name}")
        } catch (ex: Exception) {
            log.error("Error while writing metadata to MKV file: ${file.absolutePath}", ex)
        }
    }

    fun writeFilmDataToNfo(media: Media, file: File, metadata: MediaMetadata) {
        val nfoFile = File(file.parentFile, "${metadata.filename()}.nfo")
        try {
            if (nfoFile.exists()) {
                log.warn("[{}] NFO file already exists for file: ${file.name}. Skipping writing.", metadata)
                return
            }

            if (media.type == MediaType.FILM) {
                nfoFile.printWriter().use { writer ->
                    writer.println("<movie>")
                    writer.println("  <createdby>MediaMetaWriter by DanAngelus</createdby>")
                    metadata.title?.let { writer.println("  <title>$it</title>") }
                    metadata.originalTitle?.let { writer.println("  <originaltitle>$it</originaltitle>") }
                    metadata.plot?.let { writer.println("  <plot>$it</plot>") }
                    metadata.outline?.let { writer.println("  <tagline>$it</tagline>") }
                    metadata.language?.let { writer.println("  <language>$it</language>") }
                    metadata.length?.let { writer.println("  <runtime>${it.replace(" minutes", "")}</runtime>") }
                    metadata.year?.let {
                        writer.println("  <premiered>$it</premiered>")
                        writer.println("  <year>$it</year>")
                    }
                    metadata.releaseDate?.let { writer.println("  <releasedate>${it.substring(0,10)}</releasedate>") }
                    metadata.certification?.let { writer.println("  <mpaa>GB-$it</mpaa>") }
                    metadata.tmdbId?.let { writer.println("  <uniqueid type=\"tmdb\" default=\"true\">$it</uniqueid>") }
                    metadata.imdbId?.let { writer.println("  <uniqueid type=\"imdb\" default=\"true\">$it</uniqueid>") }
                    metadata.rating?.let { writer.println("  <rating>$it</rating>") }
                    if (metadata.poster != null ||
                            metadata.logo != null ||
                            metadata.backdrop != null) {
                        writer.println("  <art>")
                        if (metadata.backdrop != null) writer.println("    <backdrop>backdrop.jpg</backdrop>")
                        if (metadata.logo != null) writer.println("    <logo>logo.jpg</logo>")
                        if (metadata.poster != null) writer.println("    <poster>poster.jpg</poster>")
                        writer.println("  </art>")
                    }
                    metadata.keywords?.forEach { writer.println("  <tag>$it</tag>") }
                    metadata.genre?.forEach { writer.println("  <genre>$it</genre>") }
                    metadata.studio?.let { writer.println("  <studio>$it</studio>") }
                    metadata.director?.let { writer.println("  <director>$it</director>") }
                    metadata.producers?.forEach { writer.println("  <producer>$it</producer>") }
                    metadata.actors?.forEach {
                        writer.println("  <actor>")
                        writer.println("      <name>${it.actor}</name>")
                        writer.println("      <role>${it.character}</role>")
                        writer.println("      <order>${it.order}</order>")
                        writer.println("  </actor>")
                    }
                    writer.println("</movie>")
                }
            }
            log.info("[{}] Successfully wrote NFO file: ${nfoFile.absolutePath}", metadata)
        } catch (ex: Exception) {
            log.error("[{}] Failed to write NFO file for: ${file.absolutePath}", metadata, ex)
        }
    }

    fun writeSeriesDataToNfo(
        seriesDir: File,
        series: Series,
    ) {
        val nfoFile = File(seriesDir, "tvshow.nfo")
        try {
            if (nfoFile.exists()) {
                log.warn("[{}] NFO file already exists for file: ${seriesDir.name}. Skipping writing.", series)
                return
            }

                nfoFile.printWriter().use { writer ->
                    writer.println("<tvshow>")
                    writer.println("  <createdby>MediaMetaWriter by DanAngelus</createdby>")
                    series.name?.let { writer.println("  <title>$it</title>") }
                    series.originalName?.let { writer.println("  <originaltitle>$it</originaltitle>") }
                    series.overview?.let { writer.println("  <plot>$it</plot>") }
                    series.tagline?.let { writer.println("  <tagline>$it</tagline>") }
                    series.originalLanguage?.let { writer.println("  <language>$it</language>") }
                    series.year?.let { writer.println("  <premiered>$it</premiered>") }
                    series.status?.let { writer.println("  <status>$it</status>") }
                    series.voteAverage?.let { writer.println("  <userrating>${it}</userrating>") }
//                    series.certification?.let { writer.println("  <mpaa>GB-$it</mpaa>") }
                    series.id?.let { writer.println("  <uniqueid type=\"tmdb\" default=\"true\">$it</uniqueid>") }
//                    series.imdbId?.let { writer.println("  <uniqueid type=\"imdb\" default=\"true\">$it</uniqueid>") }
                    if (series.poster != null ||
                        series.backdrop != null) {
                        writer.println("  <art>")
                        if (series.backdrop != null) writer.println("    <backdrop>backdrop.jpg</backdrop>")
                        if (series.poster != null) writer.println("    <poster>poster.jpg</poster>")
                        writer.println("  </art>")
                    }
//                    series.keywords?.forEach { writer.println("  <tag>$it</tag>") }
//                    series.genre?.forEach { writer.println("  <genre>$it</genre>") }
//                    series.studio?.let { writer.println("  <studio>$it</studio>") }
//                    series.director?.let { writer.println("  <director>$it</director>") }
//                    series.producers?.forEach { writer.println("  <producer>$it</producer>") }
//                    series.actors?.forEach {
//                        writer.println("  <actor>")
//                        writer.println("      <name>${it.actor}</name>")
//                        writer.println("      <role>${it.character}</role>")
//                        writer.println("      <order>${it.order}</order>")
//                        writer.println("  </actor>")
//                    }
                    writer.println("</tvshow>")
            }
            log.info("[{}] Successfully wrote NFO file: ${nfoFile.absolutePath}", series)
        } catch (ex: Exception) {
            log.error("[{}] Failed to write NFO file for: ${seriesDir.absolutePath}", series, ex)
        }
    }

    fun writeEpisodeDataToNfo(
        seasonDir: File,
        series: Series,
        season: Series.Season,
        episode: Series.Episode,
        episodeName: String,
    ) {
        val nfoFile = File(seasonDir, "${episodeName}.nfo")
        try {
            if (nfoFile.exists()) {
                log.warn("[{}] NFO file already exists for file: ${seasonDir.name}. Skipping writing.", series)
                return
            }

            nfoFile.printWriter().use { writer ->
                writer.println("<episodedetails>")
                writer.println("  <createdby>MediaMetaWriter by DanAngelus</createdby>")
                season.seasonNumber?.let { writer.println("  <season>$it</season>") }
                episode.episodeNumber?.let {
                    writer.println("  <episode>$it</episode>")
                    writer.println("  <displayorder>$it</displayorder>")
                }
                episode.airDate?.let { writer.println("  <premiered>${it}</premiered>") }
                episode.runtime?.let { writer.println("  <runtime>$it</runtime>") }
                episode.name?.let { writer.println("  <title>$it</title>") }
                episode.overview?.let { writer.println("  <plot>$it</plot>") }
                episode.voteAverage?.let { writer.println("  <userrating>${it}</userrating>") }
                episode.airDate?.let { writer.println("  <aired>${it}</aired>") }
//                    series.certification?.let { writer.println("  <mpaa>GB-$it</mpaa>") }
                episode.id?.let { writer.println("  <uniqueid type=\"tmdb\" default=\"true\">$it</uniqueid>") }
//                    series.imdbId?.let { writer.println("  <uniqueid type=\"imdb\" default=\"true\">$it</uniqueid>") }
                if (series.poster != null ||
                    series.backdrop != null) {
                    writer.println("  <art>")
                    if (episode.still != null) writer.println("    <thumb>$episodeName-thumb.jpg</thumb>")
                    writer.println("  </art>")
                }
//                    series.keywords?.forEach { writer.println("  <tag>$it</tag>") }
//                    series.genre?.forEach { writer.println("  <genre>$it</genre>") }
//                    series.studio?.let { writer.println("  <studio>$it</studio>") }
//                    series.director?.let { writer.println("  <director>$it</director>") }
//                    series.producers?.forEach { writer.println("  <producer>$it</producer>") }
//                    series.actors?.forEach {
//                        writer.println("  <actor>")
//                        writer.println("      <name>${it.actor}</name>")
//                        writer.println("      <role>${it.character}</role>")
//                        writer.println("      <order>${it.order}</order>")
//                        writer.println("  </actor>")
//                    }
                writer.println("</episodedetails>")
            }
            log.info("[{}] Successfully wrote NFO file: ${nfoFile.absolutePath}", series)
        } catch (ex: Exception) {
            log.error("[{}] Failed to write NFO file for: ${seasonDir.absolutePath}", series, ex)
        }
    }

    companion object {
        private val SUPPORTED_MP4_FORMATS = listOf("mp4", "m4v")
        private val SUPPORTED_MKV_FORMATS = listOf("mkv")
        private val log = LoggerFactory.getLogger(DirectoryMonitor::class.java)
    }
}