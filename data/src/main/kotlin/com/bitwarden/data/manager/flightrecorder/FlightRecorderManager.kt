package com.bitwarden.data.manager.flightrecorder

import android.content.Context
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.data.datasource.disk.FlightRecorderDiskSource
import com.bitwarden.data.datasource.disk.model.FlightRecorderDataSet
import com.bitwarden.data.manager.model.FlightRecorderDuration
import kotlinx.coroutines.flow.StateFlow
import java.time.Clock

/**
 * Manager class that handles recording logs for the flight recorder.
 */
interface FlightRecorderManager {
    /**
     * Returns a set of all flight recorder data currently stored on the device.
     */
    val flightRecorderData: FlightRecorderDataSet

    /**
     * Tracks changes to [FlightRecorderDataSet].
     */
    val flightRecorderDataFlow: StateFlow<FlightRecorderDataSet>

    /**
     * Dismisses the all flight recorder banners.
     */
    fun dismissFlightRecorderBanner()

    /**
     * Starts the flight recorder for the given [duration].
     */
    fun startFlightRecorder(duration: FlightRecorderDuration)

    /**
     * Cancels the active flight recorder if one is currently active.
     */
    fun endFlightRecorder()

    /**
     * Deletes the raw log file and metadata associated with the [data].
     */
    fun deleteLog(data: FlightRecorderDataSet.FlightRecorderData)

    /**
     * Deletes the raw log files and metadata.
     */
    fun deleteAllLogs()

    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates a new instance of the [FlightRecorderManager].
         */
        fun create(
            context: Context,
            clock: Clock,
            flightRecorderDiskSource: FlightRecorderDiskSource,
            flightRecorderWriter: FlightRecorderWriter,
            dispatcherManager: DispatcherManager,
        ): FlightRecorderManager = FlightRecorderManagerImpl(
            context = context,
            clock = clock,
            flightRecorderDiskSource = flightRecorderDiskSource,
            flightRecorderWriter = flightRecorderWriter,
            dispatcherManager = dispatcherManager,
        )
    }
}
