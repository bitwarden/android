package com.x8bit.bitwarden.authenticator.ui.authenticator.feature.item.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents the item data displayed to users.
 *
 * @property name Name of the account associated to the item.
 * @property alertThresholdSeconds Threshold, in seconds, at which an Item is considered near
 * expiration.
 * @property totpCodeItemData TOTP data for the account.
 */
@Parcelize
data class ItemData(
    val name: String,
    val alertThresholdSeconds: Int,
    val totpCodeItemData: TotpCodeItemData?,
) : Parcelable
