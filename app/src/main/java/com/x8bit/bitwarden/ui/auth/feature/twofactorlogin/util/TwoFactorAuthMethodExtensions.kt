package com.x8bit.bitwarden.ui.auth.feature.twofactorlogin.util

import androidx.annotation.DrawableRes
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
        TwoFactorAuthMethod.DUO -> R.string.duo_title.asText()
        TwoFactorAuthMethod.DUO_ORGANIZATION -> R.string.duo_org_title.asText(
            R.string.organization.asText(),
        )

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
    TwoFactorAuthMethod.DUO -> R.string.follow_the_steps_from_duo_to_finish_logging_in.asText()
    TwoFactorAuthMethod.DUO_ORGANIZATION -> {
        R.string.duo_two_step_login_is_required_for_your_account
            .asText()
            .concat(" ".asText())
            .concat(R.string.follow_the_steps_from_duo_to_finish_logging_in.asText())
    }

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

/**
 * Gets a drawable resource for the image to be displayed or `null` if nothing should be displayed.
 */
@get:DrawableRes
val TwoFactorAuthMethod.imageRes: Int?
    get() = when (this) {
        TwoFactorAuthMethod.YUBI_KEY -> R.drawable.yubi_key
        else -> null
    }
