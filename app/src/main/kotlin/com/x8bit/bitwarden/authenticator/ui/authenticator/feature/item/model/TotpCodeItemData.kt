package com.x8bit.bitwarden.authenticator.ui.authenticator.feature.item.model

import android.os.Parcelable
import com.x8bit.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemType
import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.itemlisting.ItemListingAction
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.Text
import kotlinx.parcelize.Parcelize

/**
 * The data relating to the verification code.
 *
 * @property periodSeconds The period for the verification code.
 * @property timeLeftSeconds The time left for the verification timer.
 * @property verificationCode The verification code for the item.
 * @property totpCode The totp code for the item.
 * @property issuer Name of the item provider.
 * @property alertThresholdSeconds Threshold, in seconds, at which an Item is considered near
 * expiration.
 */
@Parcelize
data class TotpCodeItemData(
    val periodSeconds: Int,
    val timeLeftSeconds: Int,
    val verificationCode: Text,
    val totpCode: Text,
    val type: AuthenticatorItemType,
    val username: Text?,
    val issuer: Text,
    val alertThresholdSeconds: Int
) : Parcelable
