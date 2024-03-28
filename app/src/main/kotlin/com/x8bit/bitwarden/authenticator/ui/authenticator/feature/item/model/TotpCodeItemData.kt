package com.x8bit.bitwarden.authenticator.ui.authenticator.feature.item.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * The data relating to the verification code.
 *
 * @property periodSeconds The period for the verification code.
 * @property timeLeftSeconds The time left for the verification timer.
 * @property verificationCode The verification code for the item.
 * @property totpCode The totp code for the item.
 */
@Parcelize
data class TotpCodeItemData(
    val periodSeconds: Int,
    val timeLeftSeconds: Int,
    val verificationCode: String,
    val totpCode: String,
) : Parcelable
