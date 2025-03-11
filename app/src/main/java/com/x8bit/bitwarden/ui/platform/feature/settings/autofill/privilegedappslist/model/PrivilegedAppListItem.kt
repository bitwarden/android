package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.privilegedappslist.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents a single item in the list of trusted privileged apps.
 *
 * @param packageName The package name of the privileged app.
 * @param signature The signature of the privileged app.
 */
@Parcelize
data class PrivilegedAppListItem(
    val packageName: String,
    val signature: String,
) : Parcelable
