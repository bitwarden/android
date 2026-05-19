package com.x8bit.bitwarden.data.auth.repository.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.Instant

/**
 * Domain model for a pending auth request associated with a device.
 *
 * @property id The unique identifier of the pending auth request.
 * @property creationDate The date and time on which this auth request was created.
 */
@Parcelize
data class DevicePendingAuthRequest(
    val id: String,
    val creationDate: Instant,
) : Parcelable
