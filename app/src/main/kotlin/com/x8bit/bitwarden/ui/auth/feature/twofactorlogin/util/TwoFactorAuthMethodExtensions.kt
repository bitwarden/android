package com.x8bit.bitwarden.ui.auth.feature.twofactorlogin.util

import androidx.annotation.DrawableRes
import com.bitwarden.network.model.TwoFactorAuthMethod
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.concat

/**
 * Get the title for the given auth method.
 */
val TwoFactorAuthMethod.title: Text
    get() = when (this) {
        TwoFactorAuthMethod.AUTHENTICATOR_APP -> BitwardenString.authenticator_app_title.asText()
        TwoFactorAuthMethod.DUO -> BitwardenString.duo_title.asText()
        TwoFactorAuthMethod.DUO_ORGANIZATION -> BitwardenString.duo_org_title.asText(
            BitwardenString.organization.asText(),
        )

        TwoFactorAuthMethod.EMAIL -> BitwardenString.email.asText()
        TwoFactorAuthMethod.RECOVERY_CODE -> BitwardenString.recovery_code_title.asText()
        TwoFactorAuthMethod.WEB_AUTH -> BitwardenString.fido2_authenticate_web_authn.asText()
        TwoFactorAuthMethod.YUBI_KEY -> BitwardenString.yubi_key_title.asText()
        else -> "".asText()
    }

/**
 * Get the description for the given auth method.
 */
fun TwoFactorAuthMethod.description(email: String): Text = when (this) {
    TwoFactorAuthMethod.AUTHENTICATOR_APP -> BitwardenString.enter_verification_code_app.asText()
    TwoFactorAuthMethod.DUO -> {
        BitwardenString.follow_the_steps_from_duo_to_finish_logging_in.asText()
    }
    TwoFactorAuthMethod.DUO_ORGANIZATION -> {
        BitwardenString.duo_two_step_login_is_required_for_your_account
            .asText()
            .concat(" ".asText())
            .concat(BitwardenString.follow_the_steps_from_duo_to_finish_logging_in.asText())
    }

    TwoFactorAuthMethod.EMAIL -> BitwardenString.enter_verification_code_email.asText(email)
    TwoFactorAuthMethod.WEB_AUTH -> {
        BitwardenString.continue_to_complete_web_authn_verification.asText()
    }
    TwoFactorAuthMethod.YUBI_KEY -> BitwardenString.yubi_key_instruction.asText()
    else -> "".asText()
}

/**
 * Get the button label for the given auth method.
 */
val TwoFactorAuthMethod.button: Text
    get() = when (this) {
        TwoFactorAuthMethod.DUO,
        TwoFactorAuthMethod.DUO_ORGANIZATION,
            -> BitwardenString.launch_duo.asText()

        TwoFactorAuthMethod.AUTHENTICATOR_APP,
        TwoFactorAuthMethod.EMAIL,
        TwoFactorAuthMethod.YUBI_KEY,
        TwoFactorAuthMethod.U2F,
        TwoFactorAuthMethod.REMEMBER,
        TwoFactorAuthMethod.RECOVERY_CODE,
            -> BitwardenString.continue_text.asText()

        TwoFactorAuthMethod.WEB_AUTH -> BitwardenString.launch_web_authn.asText()
    }

/**
 * Gets a boolean indicating if the given auth method has the continue button enabled by default.
 */
val TwoFactorAuthMethod.isContinueButtonEnabled: Boolean
    get() = when (this) {
        TwoFactorAuthMethod.DUO,
        TwoFactorAuthMethod.DUO_ORGANIZATION,
        TwoFactorAuthMethod.WEB_AUTH,
            -> true

        TwoFactorAuthMethod.AUTHENTICATOR_APP,
        TwoFactorAuthMethod.EMAIL,
        TwoFactorAuthMethod.YUBI_KEY,
        TwoFactorAuthMethod.U2F,
        TwoFactorAuthMethod.REMEMBER,
        TwoFactorAuthMethod.RECOVERY_CODE,
            -> false
    }

/**
 * Gets a boolean indicating if the given auth method should display the password input field.
 */
val TwoFactorAuthMethod.showPasswordInput: Boolean
    get() = when (this) {
        TwoFactorAuthMethod.DUO,
        TwoFactorAuthMethod.DUO_ORGANIZATION,
        TwoFactorAuthMethod.WEB_AUTH,
            -> false

        TwoFactorAuthMethod.AUTHENTICATOR_APP,
        TwoFactorAuthMethod.EMAIL,
        TwoFactorAuthMethod.YUBI_KEY,
        TwoFactorAuthMethod.U2F,
        TwoFactorAuthMethod.REMEMBER,
        TwoFactorAuthMethod.RECOVERY_CODE,
            -> true
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
        TwoFactorAuthMethod.YUBI_KEY -> BitwardenDrawable.img_yubi_key
        TwoFactorAuthMethod.EMAIL -> BitwardenDrawable.ill_new_device_verification
        TwoFactorAuthMethod.AUTHENTICATOR_APP -> BitwardenDrawable.ill_authenticator
        else -> null
    }
