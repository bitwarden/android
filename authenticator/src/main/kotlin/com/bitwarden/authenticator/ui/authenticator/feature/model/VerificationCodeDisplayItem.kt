package com.bitwarden.authenticator.ui.authenticator.feature.model

import android.os.Parcelable
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import kotlinx.parcelize.Parcelize

/**
 * The data for the verification code item to display.
 */
@Parcelize
data class VerificationCodeDisplayItem(
    val id: String,
    val title: String,
    val subtitle: String?,
    val timeLeftSeconds: Int,
    val periodSeconds: Int,
    val alertThresholdSeconds: Int,
    val authCode: String,
    val startIcon: IconData = IconData.Local(
        iconRes = BitwardenDrawable.ic_login_item,
        testTag = "BitwardenIcon",
    ),
    val favorite: Boolean,
    val allowLongPressActions: Boolean,
    val showMoveToBitwarden: Boolean,
) : Parcelable
