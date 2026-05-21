package com.bitwarden.network.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Represents a pending auth request associated with a device.
 *
 * @property id The unique identifier of the pending auth request.
 * @property creationDate The date and time on which this auth request was created.
 */
@Serializable
data class DevicePendingAuthRequestJson(
    @SerialName("id") val id: String,
    @Contextual @SerialName("creationDate") val creationDate: Instant,
)
