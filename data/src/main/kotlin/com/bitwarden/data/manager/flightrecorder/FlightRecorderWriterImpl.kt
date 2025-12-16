package com.bitwarden.data.manager.flightrecorder

import android.os.Build
import android.util.Log
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.core.data.manager.BuildInfoManager
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.util.toFormattedPattern
import com.bitwarden.data.datasource.disk.model.FlightRecorderDataSet
import com.bitwarden.data.manager.file.FileManager
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Clock
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

private const val LOG_TIME_PATTERN: String = "yyyy-MM-dd HH:mm:ss:SSS"

/**
 * The default implementation of the [FlightRecorderWriter].
 */
@OmitFromCoverage
internal class FlightRecorderWriterImpl(
    private val clock: Clock,
    private val fileManager: FileManager,
    private val dispatcherManager: DispatcherManager,
    private val buildInfoManager: BuildInfoManager,
) : FlightRecorderWriter {
    override suspend fun deleteLog(data: FlightRecorderDataSet.FlightRecorderData) {
        fileManager.delete(File(File(fileManager.logsDirectory), data.fileName))
    }

    override suspend fun deleteLogs(dataset: FlightRecorderDataSet) {
        fileManager.delete(
            files = dataset
                .data
                .map { File(File(fileManager.logsDirectory), it.fileName) }
                .toTypedArray(),
        )
    }

    override suspend fun getOrCreateLogFile(
        data: FlightRecorderDataSet.FlightRecorderData,
    ): Result<File> = withContext(dispatcherManager.io) {
        runCatching {
            val logFolder = File(fileManager.logsDirectory)
            if (!logFolder.exists()) logFolder.mkdirs()
            val logFile = File(logFolder, data.fileName)
            if (!logFile.exists()) {
                logFile.createNewFile()
                val startTime = Instant
                    .ofEpochMilli(data.startTimeMs)
                    .toFormattedPattern(pattern = LOG_TIME_PATTERN, clock = clock)
                val operatingSystem = "${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})"
                // Upon creating the new file, we pre-populate it with basic data
                BufferedWriter(FileWriter(logFile, true)).use { bw ->
                    bw.append("Bitwarden Android - ${buildInfoManager.applicationName}")
                    bw.newLine()
                    bw.append("Log Start Time: $startTime")
                    bw.newLine()
                    bw.append("Log Duration: ${data.durationMs.milliseconds}")
                    bw.newLine()
                    bw.append("App Version: ${buildInfoManager.versionData}")
                    bw.newLine()
                    bw.append("Build: ${buildInfoManager.buildAndFlavor}")
                    bw.newLine()
                    bw.append("Operating System: $operatingSystem")
                    bw.newLine()
                    bw.append("Device: ${Build.BRAND} ${Build.MODEL}")
                    bw.newLine()
                    bw.append("Fingerprint: ${Build.FINGERPRINT}")
                    bw.newLine()
                }
            }
            logFile
        }
    }

    override suspend fun writeToLog(
        data: FlightRecorderDataSet.FlightRecorderData,
        priority: Int,
        tag: String?,
        message: String,
        throwable: Throwable?,
    ) {
        val logFile = getOrCreateLogFile(data = data).getOrNull() ?: return
        val formattedTime = clock
            .instant()
            .toFormattedPattern(pattern = LOG_TIME_PATTERN, clock = clock)
        withContext(context = dispatcherManager.io) {
            runCatching {
                BufferedWriter(FileWriter(logFile, true)).use { bw ->
                    bw.append(formattedTime)
                    bw.append(" – ")
                    bw.append(priority.logLevel)
                    tag?.let {
                        bw.append(" – ")
                        bw.append(it)
                    }
                    bw.append(" – ")
                    bw.append(message)
                    throwable?.let {
                        bw.append(" – ")
                        bw.append(it.getStackTraceString())
                    }
                    bw.newLine()
                }
            }
        }
    }
}

/**
 * Helper function modifier from the [Timber] library.
 */
@Suppress("MagicNumber")
private fun Throwable.getStackTraceString(): String {
    // Don't replace this with Log.getStackTraceString() - it hides
    // UnknownHostException, which is not what we want.
    return StringWriter(256).use { sw ->
        PrintWriter(sw, false).use { pw ->
            this.printStackTrace(pw)
            pw.flush()
        }
        sw.toString()
    }
}

private val Int.logLevel: String
    get() = when (this) {
        Log.VERBOSE -> "VERBOSE"
        Log.DEBUG -> "DEBUG"
        Log.INFO -> "INFO"
        Log.WARN -> "WARNING"
        Log.ERROR -> "ERROR"
        Log.ASSERT -> "ASSERT"
        else -> "UNKNOWN"
    }
