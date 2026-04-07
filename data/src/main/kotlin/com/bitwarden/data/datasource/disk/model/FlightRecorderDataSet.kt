package com.bitwarden.data.datasource.disk.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Persistable class containing all required data for a all instances of the flight recorder.
 */
@Serializable
data class FlightRecorderDataSet(
    @SerialName("data")
    val data: Set<FlightRecorderData>,
) {
    /**
     * Returns `true` is there is an active [FlightRecorderDataSet.FlightRecorderData].
     */
    val hasActiveFlightRecorderData: Boolean
        get() = this.activeFlightRecorderData != null

    /**
     * Retrieves tha active log if available.
     */
    val activeFlightRecorderData: FlightRecorderData?
        get() = this.data.find { it.isActive }

    /**
     * Persistable class containing all required data for a single instance of the flight recorder.
     */
    @Serializable
    data class FlightRecorderData(
        @SerialName("id")
        val id: String,

        @SerialName("fileName")
        val fileName: String,

        @SerialName("startTime")
        val startTimeMs: Long,

        @SerialName("duration")
        val durationMs: Long,

        @SerialName("isActive")
        val isActive: Boolean,

        @SerialName("isBannerDismissed")
        val isBannerDismissed: Boolean = false,

        @SerialName("expirationTime")
        val expirationTimeMs: Long? = null,
    )
}
