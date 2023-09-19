package io.bouckaert.referendumreader

import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.*
import java.util.zip.ZipInputStream

class AecFtpService(
    host: String,
    port: Int?,
    private val electoralEvent: Long
) : AutoCloseable {
    private val ftp = FTPClient().apply {
        if (port != null) connect(host, port) else connect(host)
        enterLocalPassiveMode()
        login("anonymous", "ftpService@io.bouckaert.aec-media-feed-loader")
        if (!FTPReply.isPositiveCompletion(this.replyCode)) {
            this.disconnect()
            throw RuntimeException("Could not successfully establish FTP connection")
        }
    }

    private val fileDateFormat = DateTimeFormatterBuilder()
        .appendPattern("uuuuMMddHHmmss")
        .parseDefaulting(ChronoField.MILLI_OF_SECOND, 0)
        .parseDefaulting(ChronoField.MICRO_OF_SECOND, 0)
        .toFormatter(Locale.UK)
        .withZone(ZoneId.systemDefault())

    val preloadStream: ZipFtpInputStream get() = getRelevantInputStream(Granularity.DETAILED, Verbosity.PRELOAD, "aec-mediafeed-results-detailed-preload")
    val lightProgressStream: ZipFtpInputStream get() = getRelevantInputStream(Granularity.DETAILED, Verbosity.LIGHT_PROGRESS, "aec-mediafeed-results-detailed-lightprogress")
    private fun getRelevantInputStream(
        granularity: Granularity,
        verbosity: Verbosity,
        xmlFile: String
    ): ZipFtpInputStream {
        ftp.changeToParentDirectory()
        ftp.changeToParentDirectory()
        ftp.changeToParentDirectory()
        if (ftp.changeWorkingDirectory(electoralEvent.toString())) {
            if (ftp.changeWorkingDirectory(granularity.pathValue) && ftp.changeWorkingDirectory(verbosity.pathValue)) {
                val filesByDate = ftp.listFiles().associateBy {
                    it.name
                        .removePrefix("aec-mediafeed-${granularity.pathValue}-${verbosity.pathValue}-$electoralEvent-")
                        .removeSuffix(".zip")
                        .let { date -> fileDateFormat.parse(date, Instant::from) }
                }
                val latest =
                    filesByDate.keys.reduce { latest, current -> if (current.isAfter(latest)) current else latest }
                val latestName = filesByDate[latest]?.name ?: filesByDate.values.first().name

                val fis = ftp.retrieveFileStream(latestName)
                val zis = ZipInputStream(fis)
                var zipEntry = zis.nextEntry

                while (
                    zipEntry != null
                ) {
                    if (
                        !zipEntry.isDirectory &&
                        zipEntry.name.startsWith("xml/") &&
                        zipEntry.name.endsWith(".xml") &&
                        zipEntry.name.contains(xmlFile)
                    )  {
                        break
                    } else {
                        zipEntry = zis.nextEntry
                    }
                }

                return ZipFtpInputStream(zis, fis)
            } else throw RuntimeException("Directory structure does not match expectations for granularity $granularity and verbosity $verbosity")
        } else throw RuntimeException("Could not find electoral event $electoralEvent")
    }
    override fun close() {
        if (ftp.isConnected) {
            try {
                ftp.disconnect()
            } catch (_: IOException) {
            }
        }
    }

    private enum class Granularity(val pathValue: String) {
        DETAILED("Detailed"),
        STANDARD("Standard")
    }

    private enum class Verbosity(val pathValue: String) {
        LIGHT("Light"),
        LIGHT_PROGRESS("LightProgress"),
        PRELOAD("Preload"),
        VERBOSE("Verbose")
    }

    inner class ZipFtpInputStream(
        val delegate: ZipInputStream,
        private val fis: InputStream
    ): Closeable by delegate {
        override fun close() {
            delegate.close()
            fis.close()
            ftp.completePendingCommand()
        }
    }
}