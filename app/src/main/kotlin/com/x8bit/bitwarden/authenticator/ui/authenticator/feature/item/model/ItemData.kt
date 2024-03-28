package com.x8bit.bitwarden.authenticator.ui.authenticator.feature.item.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents the item data displayed to users.
 *
 * @property accountName Name of the account associated to the item.
 * @property totpCodeItemData TOTP data for the account.
 */
@Parcelize
data class ItemData(
    val accountName: String,
    val totpCodeItemData: TotpCodeItemData?,
) : Parcelable
