package com.x8bit.bitwarden.data.auth.repository.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.Instant

/**
 * Domain model for a device registered to the current user.
 *
 * @property id The unique identifier of the device.
 * @property name The name of the device.
 * @property identifier The unique device install identifier of the device.
 * @property type The type of the device.
 * @property isTrusted Whether this device is trusted.
 * @property creationDate The date and time on which this device was created.
 * @property lastActivityDate The date and time of the device's last activity, if available.
 * @property pendingAuthRequest The pending auth request for this device, if any.
 * @property isCurrentDevice If this is the current device being used.
 */
@Parcelize
data class DeviceInfo(
    val id: String,
    val name: String,
    val identifier: String,
    val type: Int,
    val isTrusted: Boolean,
    val creationDate: Instant,
    val lastActivityDate: Instant?,
    val pendingAuthRequest: DevicePendingAuthRequest?,
    val isCurrentDevice: Boolean,
) : Parcelable
