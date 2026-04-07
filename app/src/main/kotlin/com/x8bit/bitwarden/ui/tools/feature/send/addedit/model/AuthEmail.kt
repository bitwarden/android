package com.x8bit.bitwarden.ui.tools.feature.send.addedit.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * Represents an authentication email with a unique identifier.
 *
 * @property id A unique identifier for this email entry.
 * @property value The email address value.
 */
@Parcelize
data class AuthEmail(
    val id: String = UUID.randomUUID().toString(),
    val value: String,
) : Parcelable
