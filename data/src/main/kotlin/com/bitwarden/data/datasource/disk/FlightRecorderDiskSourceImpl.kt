package com.bitwarden.data.datasource.disk

import android.content.SharedPreferences
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.core.data.util.decodeFromStringOrNull
import com.bitwarden.data.datasource.disk.model.FlightRecorderDataSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.serialization.json.Json

private const val FLIGHT_RECORDER_KEY = "flightRecorderData"

/**
 * Primary implementation of [FlightRecorderDiskSource].
 */
internal class FlightRecorderDiskSourceImpl(
    private val json: Json,
    sharedPreferences: SharedPreferences,
) : BaseDiskSource(sharedPreferences = sharedPreferences),
    FlightRecorderDiskSource {
    private val mutableFlightRecorderDataFlow = bufferedMutableSharedFlow<FlightRecorderDataSet?>()

    override var flightRecorderData: FlightRecorderDataSet?
        get() = getString(key = FLIGHT_RECORDER_KEY)
            ?.let { json.decodeFromStringOrNull<FlightRecorderDataSet>(it) }
        set(value) {
            putString(key = FLIGHT_RECORDER_KEY, value = value?.let { json.encodeToString(it) })
            mutableFlightRecorderDataFlow.tryEmit(value)
        }

    override val flightRecorderDataFlow: Flow<FlightRecorderDataSet?>
        get() = mutableFlightRecorderDataFlow.onSubscription { emit(flightRecorderData) }
}
