package com.x8bit.bitwarden.ui.vault.feature.exportitems.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Represents a list item for selecting an account to export items from.
 */
@Serializable
@Parcelize
data class AccountSelectionListItem(
    val userId: String,
    val isItemRestricted: Boolean,
    val avatarColorHex: String,
    val initials: String,
    val email: String,
) : Parcelable
