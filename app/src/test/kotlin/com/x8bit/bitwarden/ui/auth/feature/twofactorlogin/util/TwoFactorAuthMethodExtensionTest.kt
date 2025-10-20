package com.x8bit.bitwarden.ui.auth.feature.twofactorlogin.util

import com.bitwarden.network.model.TwoFactorAuthMethod
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.concat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TwoFactorAuthMethodExtensionTest {
    @Test
    fun `title returns the expected value`() {
        mapOf(
            TwoFactorAuthMethod.AUTHENTICATOR_APP to BitwardenString.authenticator_app_title
                .asText(),
            TwoFactorAuthMethod.EMAIL to BitwardenString.email.asText(),
            TwoFactorAuthMethod.DUO to BitwardenString.duo_title.asText(),
            TwoFactorAuthMethod.YUBI_KEY to BitwardenString.yubi_key_title.asText(),
            TwoFactorAuthMethod.U2F to "".asText(),
            TwoFactorAuthMethod.REMEMBER to "".asText(),
            TwoFactorAuthMethod.DUO_ORGANIZATION to BitwardenString.duo_org_title.asText(
                BitwardenString.organization.asText(),
            ),
            TwoFactorAuthMethod.WEB_AUTH to BitwardenString.fido2_authenticate_web_authn.asText(),
            TwoFactorAuthMethod.RECOVERY_CODE to BitwardenString.recovery_code_title.asText(),
        )
            .forEach { (type, title) ->
                assertEquals(
                    title,
                    type.title,
                )
            }
    }

    @Test
    fun `description returns the expected value`() {
        mapOf(
            TwoFactorAuthMethod.AUTHENTICATOR_APP to
                BitwardenString.enter_verification_code_app.asText(),
            TwoFactorAuthMethod.EMAIL to
                BitwardenString.enter_verification_code_email.asText("ex***@email.com"),
            TwoFactorAuthMethod.DUO to
                BitwardenString.follow_the_steps_from_duo_to_finish_logging_in.asText(),
            TwoFactorAuthMethod.YUBI_KEY to BitwardenString.yubi_key_instruction.asText(),
            TwoFactorAuthMethod.U2F to "".asText(),
            TwoFactorAuthMethod.REMEMBER to "".asText(),
            TwoFactorAuthMethod.DUO_ORGANIZATION to
                BitwardenString.duo_two_step_login_is_required_for_your_account
                    .asText()
                    .concat(" ".asText())
                    .concat(
                        BitwardenString.follow_the_steps_from_duo_to_finish_logging_in.asText(),
                    ),
            TwoFactorAuthMethod.WEB_AUTH to
                BitwardenString.continue_to_complete_web_authn_verification.asText(),
            TwoFactorAuthMethod.RECOVERY_CODE to "".asText(),
        )
            .forEach { (type, title) ->
                assertEquals(
                    title,
                    type.description("ex***@email.com"),
                )
            }
    }

    @Test
    fun `button returns the expected value`() {
        mapOf(
            TwoFactorAuthMethod.AUTHENTICATOR_APP to BitwardenString.continue_text.asText(),
            TwoFactorAuthMethod.EMAIL to BitwardenString.continue_text.asText(),
            TwoFactorAuthMethod.DUO to BitwardenString.launch_duo.asText(),
            TwoFactorAuthMethod.YUBI_KEY to BitwardenString.continue_text.asText(),
            TwoFactorAuthMethod.U2F to BitwardenString.continue_text.asText(),
            TwoFactorAuthMethod.REMEMBER to BitwardenString.continue_text.asText(),
            TwoFactorAuthMethod.DUO_ORGANIZATION to BitwardenString.launch_duo.asText(),
            TwoFactorAuthMethod.WEB_AUTH to BitwardenString.launch_web_authn.asText(),
            TwoFactorAuthMethod.RECOVERY_CODE to BitwardenString.continue_text.asText(),
        )
            .forEach { (type, buttonLabel) ->
                assertEquals(buttonLabel, type.button)
            }
    }

    @Test
    fun `isContinueButtonEnabled returns the expected value`() {
        mapOf(
            TwoFactorAuthMethod.AUTHENTICATOR_APP to false,
            TwoFactorAuthMethod.EMAIL to false,
            TwoFactorAuthMethod.DUO to true,
            TwoFactorAuthMethod.YUBI_KEY to false,
            TwoFactorAuthMethod.U2F to false,
            TwoFactorAuthMethod.REMEMBER to false,
            TwoFactorAuthMethod.DUO_ORGANIZATION to true,
            TwoFactorAuthMethod.WEB_AUTH to true,
            TwoFactorAuthMethod.RECOVERY_CODE to false,
        )
            .forEach { (type, isContinueButtonEnabled) ->
                assertEquals(isContinueButtonEnabled, type.isContinueButtonEnabled)
            }
    }

    @Test
    fun `showPasswordInput returns the expected value`() {
        mapOf(
            TwoFactorAuthMethod.AUTHENTICATOR_APP to true,
            TwoFactorAuthMethod.EMAIL to true,
            TwoFactorAuthMethod.DUO to false,
            TwoFactorAuthMethod.YUBI_KEY to true,
            TwoFactorAuthMethod.U2F to true,
            TwoFactorAuthMethod.REMEMBER to true,
            TwoFactorAuthMethod.DUO_ORGANIZATION to false,
            TwoFactorAuthMethod.WEB_AUTH to false,
            TwoFactorAuthMethod.RECOVERY_CODE to true,
        )
            .forEach { (type, showPasswordInput) ->
                assertEquals(showPasswordInput, type.showPasswordInput)
            }
    }

    @Test
    fun `shouldUseNfc returns the expected value`() {
        mapOf(
            TwoFactorAuthMethod.AUTHENTICATOR_APP to false,
            TwoFactorAuthMethod.EMAIL to false,
            TwoFactorAuthMethod.DUO to false,
            TwoFactorAuthMethod.YUBI_KEY to true,
            TwoFactorAuthMethod.U2F to false,
            TwoFactorAuthMethod.REMEMBER to false,
            TwoFactorAuthMethod.DUO_ORGANIZATION to false,
            TwoFactorAuthMethod.WEB_AUTH to false,
            TwoFactorAuthMethod.RECOVERY_CODE to false,
        )
            .forEach { (type, shouldUseNfc) ->
                assertEquals(shouldUseNfc, type.shouldUseNfc)
            }
    }

    @Test
    fun `imageRes returns the expected value`() {
        mapOf(
            TwoFactorAuthMethod.AUTHENTICATOR_APP to BitwardenDrawable.ill_authenticator,
            TwoFactorAuthMethod.EMAIL to BitwardenDrawable.ill_new_device_verification,
            TwoFactorAuthMethod.DUO to null,
            TwoFactorAuthMethod.YUBI_KEY to BitwardenDrawable.img_yubi_key,
            TwoFactorAuthMethod.U2F to null,
            TwoFactorAuthMethod.REMEMBER to null,
            TwoFactorAuthMethod.DUO_ORGANIZATION to null,
            TwoFactorAuthMethod.WEB_AUTH to null,
            TwoFactorAuthMethod.RECOVERY_CODE to null,
        )
            .forEach { (type, imageRes) ->
                assertEquals(imageRes, type.imageRes)
            }
    }
}
