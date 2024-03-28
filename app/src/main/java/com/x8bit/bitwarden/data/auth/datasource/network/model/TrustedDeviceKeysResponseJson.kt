package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

/**
 * The response body for trusting a device.
 */
@Serializable
data class TrustedDeviceKeysResponseJson(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("identifier") val identifier: String,
    @SerialName("type") val type: Int,
    @Contextual @SerialName("creationDate") val creationDate: ZonedDateTime,
)
