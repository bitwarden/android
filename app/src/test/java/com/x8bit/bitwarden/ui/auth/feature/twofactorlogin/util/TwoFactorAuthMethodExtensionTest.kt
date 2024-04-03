package com.x8bit.bitwarden.ui.auth.feature.twofactorlogin.util

import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.network.model.TwoFactorAuthMethod
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TwoFactorAuthMethodExtensionTest {
    @Test
    fun `title returns the expected value`() {
        mapOf(
            TwoFactorAuthMethod.AUTHENTICATOR_APP to R.string.authenticator_app_title.asText(),
            TwoFactorAuthMethod.EMAIL to R.string.email.asText(),
            TwoFactorAuthMethod.DUO to R.string.duo_title.asText(),
            TwoFactorAuthMethod.YUBI_KEY to R.string.yubi_key_title.asText(),
            TwoFactorAuthMethod.U2F to "".asText(),
            TwoFactorAuthMethod.REMEMBER to "".asText(),
            TwoFactorAuthMethod.DUO_ORGANIZATION to R.string.duo_org_title.asText(
                R.string.organization.asText(),
            ),
            TwoFactorAuthMethod.FIDO_2_WEB_APP to "".asText(),
            TwoFactorAuthMethod.RECOVERY_CODE to R.string.recovery_code_title.asText(),
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
                R.string.enter_verification_code_app.asText(),
            TwoFactorAuthMethod.EMAIL to
                R.string.enter_verification_code_email.asText("ex***@email.com"),
            TwoFactorAuthMethod.DUO to
                R.string.follow_the_steps_from_duo_to_finish_logging_in.asText(),
            TwoFactorAuthMethod.YUBI_KEY to R.string.yubi_key_instruction.asText(),
            TwoFactorAuthMethod.U2F to "".asText(),
            TwoFactorAuthMethod.REMEMBER to "".asText(),
            TwoFactorAuthMethod.DUO_ORGANIZATION to
                R.string.duo_two_step_login_is_required_for_your_account
                    .asText()
                    .concat(" ".asText())
                    .concat(R.string.follow_the_steps_from_duo_to_finish_logging_in.asText()),
            TwoFactorAuthMethod.FIDO_2_WEB_APP to "".asText(),
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
    fun `isDuo returns the expected value`() {
        mapOf(
            TwoFactorAuthMethod.AUTHENTICATOR_APP to false,
            TwoFactorAuthMethod.EMAIL to false,
            TwoFactorAuthMethod.DUO to true,
            TwoFactorAuthMethod.YUBI_KEY to false,
            TwoFactorAuthMethod.U2F to false,
            TwoFactorAuthMethod.REMEMBER to false,
            TwoFactorAuthMethod.DUO_ORGANIZATION to true,
            TwoFactorAuthMethod.FIDO_2_WEB_APP to false,
            TwoFactorAuthMethod.RECOVERY_CODE to false,
        )
            .forEach { (type, isDuo) ->
                assertEquals(isDuo, type.isDuo)
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
            TwoFactorAuthMethod.FIDO_2_WEB_APP to false,
            TwoFactorAuthMethod.RECOVERY_CODE to false,
        )
            .forEach { (type, shouldUseNfc) ->
                assertEquals(shouldUseNfc, type.shouldUseNfc)
            }
    }

    @Test
    fun `imageRes returns the expected value`() {
        mapOf(
            TwoFactorAuthMethod.AUTHENTICATOR_APP to null,
            TwoFactorAuthMethod.EMAIL to null,
            TwoFactorAuthMethod.DUO to null,
            TwoFactorAuthMethod.YUBI_KEY to R.drawable.yubi_key,
            TwoFactorAuthMethod.U2F to null,
            TwoFactorAuthMethod.REMEMBER to null,
            TwoFactorAuthMethod.DUO_ORGANIZATION to null,
            TwoFactorAuthMethod.FIDO_2_WEB_APP to null,
            TwoFactorAuthMethod.RECOVERY_CODE to null,
        )
            .forEach { (type, imageRes) ->
                assertEquals(imageRes, type.imageRes)
            }
    }
}