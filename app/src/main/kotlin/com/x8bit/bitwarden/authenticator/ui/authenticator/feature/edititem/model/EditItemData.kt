package com.x8bit.bitwarden.authenticator.ui.authenticator.feature.edititem.model

import android.os.Parcelable
import com.x8bit.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemAlgorithm
import com.x8bit.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemType
import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.edititem.AuthenticatorRefreshPeriodOption
import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.edititem.VerificationCodeDigitsOption
import kotlinx.parcelize.Parcelize

/**
 * The data relating to the verification code.
 *
 * @property refreshPeriod The period for the verification code.
 * @property totpCode The totp code for the item.
 * @property accountName Account or username for this item.
 * @property issuer Name of the item provider.
 * @property algorithm Hashing algorithm used with the item.
 * @property digits Number of digits in the verification code.
 */
@Parcelize
data class EditItemData(
    val refreshPeriod: AuthenticatorRefreshPeriodOption,
    val totpCode: String,
    val type: AuthenticatorItemType,
    val accountName: String,
    val issuer: String?,
    val algorithm: AuthenticatorItemAlgorithm,
    val digits: VerificationCodeDigitsOption,
) : Parcelable
