package com.bitwarden.data.manager.flightrecorder

import com.bitwarden.data.datasource.disk.model.FlightRecorderDataSet
import java.io.File

/**
 * Helper for creating and writing log files.
 */
interface FlightRecorderWriter {
    /**
     * Deletes the file associated with the [data].
     */
    suspend fun deleteLog(data: FlightRecorderDataSet.FlightRecorderData)

    /**
     * Deletes all files associated with the [dataset].
     */
    suspend fun deleteLogs(dataset: FlightRecorderDataSet)

    /**
     * Creates or retrieves already created log files. If a new file is created, it will be
     * pre-populated with metadata.
     */
    suspend fun getOrCreateLogFile(data: FlightRecorderDataSet.FlightRecorderData): Result<File>

    /**
     * Formats the data and writes it to the log file.
     */
    suspend fun writeToLog(
        data: FlightRecorderDataSet.FlightRecorderData,
        priority: Int,
        tag: String?,
        message: String,
        throwable: Throwable?,
    )
}
