package com.x8bit.bitwarden.ui.vault.feature.vault.model

import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import com.x8bit.bitwarden.ui.platform.base.util.hexToColor
import kotlinx.parcelize.Parcelize

/**
 * Summary information about a user's account.
 *
 * @property 
 */
@Parcelize
data class NotificationSummary(
    val title: String,
    val subtitle: String,
) : Parcelable