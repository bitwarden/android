package com.x8bit.bitwarden.data.platform.manager.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Required data for passwordless requests.
 *
 * @property loginRequestId The login request ID.
 * @property userId The user ID.
 */
@Parcelize
data class PasswordlessRequestData(
    val loginRequestId: String,
    val userId: String,
) : Parcelable
