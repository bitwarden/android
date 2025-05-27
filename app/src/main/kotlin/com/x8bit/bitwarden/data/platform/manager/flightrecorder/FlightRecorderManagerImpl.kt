package com.x8bit.bitwarden.data.platform.manager.flightrecorder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.bitwarden.core.data.util.concurrentMapOf
import com.bitwarden.data.manager.DispatcherManager
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.model.FlightRecorderDataSet
import com.x8bit.bitwarden.data.platform.repository.model.FlightRecorderDuration
import com.x8bit.bitwarden.ui.platform.util.toFormattedPattern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Clock
import java.time.temporal.ChronoUnit
import java.util.UUID

private const val EXPIRATION_DURATION_DAYS: Long = 30

/**
 * The default implementation of the [FlightRecorderManager].
 */
internal class FlightRecorderManagerImpl(
    private val context: Context,
    private val clock: Clock,
    private val settingsDiskSource: SettingsDiskSource,
    private val flightRecorderWriter: FlightRecorderWriter,
    dispatcherManager: DispatcherManager,
) : FlightRecorderManager {
    private val unconfinedScope = CoroutineScope(context = dispatcherManager.unconfined)
    private val ioScope = CoroutineScope(context = dispatcherManager.io)
    private var cancellationJob: Job = Job().apply { complete() }
    private val expirationJobMap: MutableMap<String, Job> = concurrentMapOf()
    private val flightRecorderTree = FlightRecorderTree()

    override val flightRecorderData: FlightRecorderDataSet
        get() = settingsDiskSource.flightRecorderData ?: FlightRecorderDataSet(data = emptySet())

    override val flightRecorderDataFlow: StateFlow<FlightRecorderDataSet>
        get() = settingsDiskSource
            .flightRecorderDataFlow
            .map { it ?: FlightRecorderDataSet(data = emptySet()) }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = flightRecorderData,
            )

    init {
        // Always plant the tree, it will not log anything if the flight recorder is off.
        Timber.plant(flightRecorderTree)
        flightRecorderDataFlow
            .onEach {
                it.configureFlightRecorder()
                it.configureExpirationJobs()
            }
            .launchIn(scope = unconfinedScope)
        context.registerReceiver(
            ScreenStateBroadcastReceiver(),
            IntentFilter(Intent.ACTION_SCREEN_ON),
        )
    }

    override fun dismissFlightRecorderBanner() {
        val originalData = flightRecorderData
        settingsDiskSource.flightRecorderData = originalData.copy(
            data = originalData.data.map { it.copy(isBannerDismissed = true) }.toSet(),
        )
    }

    override fun startFlightRecorder(duration: FlightRecorderDuration) {
        val startTime = clock.instant()
        val originalData = flightRecorderData
        settingsDiskSource.flightRecorderData = originalData.copy(
            data = originalData
                .data
                .mapToInactive(clock = clock)
                .toMutableSet()
                .apply {
                    val formattedTime = startTime.toFormattedPattern(
                        pattern = "yyyy-MM-dd_HH-mm-ss",
                        clock = clock,
                    )
                    add(
                        element = FlightRecorderDataSet.FlightRecorderData(
                            id = UUID.randomUUID().toString(),
                            fileName = "flight_recorder_$formattedTime.txt",
                            startTimeMs = startTime.toEpochMilli(),
                            durationMs = duration.milliseconds,
                            isActive = true,
                        ),
                    )
                },
        )
    }

    override fun endFlightRecorder() {
        val originalData = flightRecorderData
        settingsDiskSource.flightRecorderData = originalData.copy(
            data = originalData.data.mapToInactive(clock = clock),
        )
    }

    override fun deleteAllLogs() {
        val activeLog = flightRecorderData.activeFlightRecorderData
        val inactiveDataset = flightRecorderData.copy(
            data = flightRecorderData.data.filterNot { it.isActive }.toSet(),
        )
        // Clear everything but the active log.
        settingsDiskSource.flightRecorderData = activeLog?.let {
            FlightRecorderDataSet(data = setOf(it))
        }
        // Clear all logs but the active one.
        ioScope.launch { flightRecorderWriter.deleteLogs(dataset = inactiveDataset) }
    }

    override fun deleteLog(data: FlightRecorderDataSet.FlightRecorderData) {
        if (data.isActive) return
        val originalData = flightRecorderData
        settingsDiskSource.flightRecorderData = originalData.copy(
            data = originalData.data.filterNot { it == data }.toSet(),
        )
        ioScope.launch { flightRecorderWriter.deleteLog(data = data) }
        expirationJobMap.remove(data.id)?.cancel()
    }

    private fun cancelCancellationJob() {
        cancellationJob.cancel()
    }

    private fun startCancellationJob(data: FlightRecorderDataSet.FlightRecorderData) {
        cancelCancellationJob()
        cancellationJob = unconfinedScope.launch {
            val endTimeMs = data.startTimeMs + data.durationMs
            val remainingTimeMs = endTimeMs - clock.instant().toEpochMilli()
            delay(timeMillis = remainingTimeMs)
            endFlightRecorder()
        }
    }

    /**
     * Configures the the flight recorder based on the [FlightRecorderDataSet].
     *
     * This sets the log to ensure that the log file is being updated properly as well as starting
     * the cancellation job.
     */
    private fun FlightRecorderDataSet.configureFlightRecorder() {
        this
            .activeFlightRecorderData
            ?.let {
                // Set the file data to be recorded too.
                flightRecorderTree.flightRecorderData = it
                startCancellationJob(data = it)
            }
            ?: run {
                // Clear the file data to stop recording.
                flightRecorderTree.flightRecorderData = null
                cancelCancellationJob()
            }
    }

    /**
     * Configure the expiration jobs based on the [FlightRecorderDataSet].
     *
     * This cancels each expiration job and rebuilds it with the up-to-date info.
     */
    private fun FlightRecorderDataSet.configureExpirationJobs() {
        this
            .data
            .filterNot { it.isActive }
            .onEach {
                expirationJobMap.remove(it.id)?.cancel()
                expirationJobMap[it.id] = ioScope.launch {
                    // If an inactive job does not have an expiration time, something has gone
                    // wrong and we just delete it.
                    val delay = (it.expirationTimeMs ?: 0L) - clock.instant().toEpochMilli()
                    delay(timeMillis = delay)
                    deleteLog(data = it)
                }
            }
    }

    private inner class FlightRecorderTree : Timber.Tree() {
        var flightRecorderData: FlightRecorderDataSet.FlightRecorderData? = null
            set(value) {
                value?.let {
                    ioScope.launch { flightRecorderWriter.getOrCreateLogFile(data = it) }
                }
                field = value
            }

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            flightRecorderData?.let {
                ioScope.launch {
                    flightRecorderWriter.writeToLog(
                        data = it,
                        priority = priority,
                        tag = tag,
                        message = message,
                        throwable = t,
                    )
                }
            }
        }
    }

    /**
     * A custom [BroadcastReceiver] that listens for when the screen is powered on and restarts the
     * cancellation job and expiration jobs to ensure they complete at the correct time.
     *
     * This is necessary because the [delay] function in a coroutine will not keep accurate time
     * when the screen is off. We do not cancel the job when the screen is off, this allows the
     * job to complete as-soon-as possible if the screen is powered off for an extended period.
     */
    private inner class ScreenStateBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            flightRecorderData.configureFlightRecorder()
            flightRecorderData.configureExpirationJobs()
        }
    }
}

/**
 * Marks all flight recorders as inactive and ensures that they have an expiration time.
 */
private fun Set<FlightRecorderDataSet.FlightRecorderData>.mapToInactive(
    clock: Clock,
): Set<FlightRecorderDataSet.FlightRecorderData> =
    this
        .map { data ->
            data.copy(
                isActive = false,
                expirationTimeMs = data
                    .expirationTimeMs
                    ?: clock
                        .instant()
                        .plus(EXPIRATION_DURATION_DAYS, ChronoUnit.DAYS)
                        .toEpochMilli(),
            )
        }
        .toSet()
