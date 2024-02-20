package com.x8bit.bitwarden.ui.auth.feature.twofactorlogin.util

import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.network.model.TwoFactorAuthMethod
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat

/**
 * Get the title for the given auth method.
 */
val TwoFactorAuthMethod.title: Text
    get() = when (this) {
        TwoFactorAuthMethod.AUTHENTICATOR_APP -> R.string.authenticator_app_title.asText()
        TwoFactorAuthMethod.DUO -> "Duo".asText() // TODO BIT-1927 replace with string resource
        TwoFactorAuthMethod.DUO_ORGANIZATION -> "Duo (".asText()
            .concat(R.string.organization.asText())
            .concat(")".asText()) // TODO BIT-1927 replace with string resource
        TwoFactorAuthMethod.EMAIL -> R.string.email.asText()
        TwoFactorAuthMethod.RECOVERY_CODE -> R.string.recovery_code_title.asText()
        TwoFactorAuthMethod.YUBI_KEY -> R.string.yubi_key_title.asText()
        else -> "".asText()
    }

/**
 * Get the description for the given auth method.
 */
fun TwoFactorAuthMethod.description(email: String): Text = when (this) {
    TwoFactorAuthMethod.AUTHENTICATOR_APP -> R.string.enter_verification_code_app.asText()
    TwoFactorAuthMethod.DUO,
    TwoFactorAuthMethod.DUO_ORGANIZATION,
    -> "Follow the steps from Duo to finish logging in."
        .asText() // TODO BIT-1927 replace with string resource
    TwoFactorAuthMethod.EMAIL -> R.string.enter_verification_code_email.asText(email)
    TwoFactorAuthMethod.YUBI_KEY -> R.string.yubi_key_instruction.asText()
    else -> "".asText()
}

/**
 * Gets a boolean indicating if the given auth method uses Duo.
 */
val TwoFactorAuthMethod.isDuo: Boolean
    get() = when (this) {
        TwoFactorAuthMethod.DUO,
        TwoFactorAuthMethod.DUO_ORGANIZATION,
        -> true

        else -> false
    }

/**
 * Gets a boolean indicating if the given auth method uses NFC.
 */
val TwoFactorAuthMethod.shouldUseNfc: Boolean
    get() = when (this) {
        TwoFactorAuthMethod.YUBI_KEY -> true
        else -> false
    }
