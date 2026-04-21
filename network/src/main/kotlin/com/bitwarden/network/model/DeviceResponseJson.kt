package com.bitwarden.network.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Response body for a single device registered to the current user.
 *
 * @property id The unique identifier of the device.
 * @property name The name of the device.
 * @property identifier The unique install identifier of the device.
 * @property type The type of the device.
 * @property creationDate The date and time on which this device was created.
 * @property isTrusted Whether this device is trusted.
 * @property encryptedUserKey The encrypted user key for this device, if available.
 * @property encryptedPublicKey The encrypted public key for this device, if available.
 * @property devicePendingAuthRequest The pending auth request for this device, if any.
 */
@Serializable
data class DeviceResponseJson(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("identifier") val identifier: String,
    @SerialName("type") val type: Int,
    @Contextual @SerialName("creationDate") val creationDate: Instant,
    @Contextual @SerialName("lastActivityDate") val lastActivityDate: Instant?,
    @SerialName("isTrusted") val isTrusted: Boolean,
    @SerialName("encryptedUserKey") val encryptedUserKey: String?,
    @SerialName("encryptedPublicKey") val encryptedPublicKey: String?,
    @SerialName("devicePendingAuthRequest")
    val devicePendingAuthRequest: DevicePendingAuthRequestJson?,
)
