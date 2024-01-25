package com.x8bit.bitwarden.ui.auth.feature.twofactorlogin.util

import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.network.model.TwoFactorAuthMethod
import com.x8bit.bitwarden.ui.platform.base.util.asText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TwoFactorAuthMethodExtensionTest {
    @Test
    fun `title returns the expected value`() {
        mapOf(
            TwoFactorAuthMethod.AUTHENTICATOR_APP to R.string.authenticator_app_title.asText(),
            TwoFactorAuthMethod.EMAIL to R.string.email.asText(),
            TwoFactorAuthMethod.DUO to "".asText(),
            TwoFactorAuthMethod.YUBI_KEY to "".asText(),
            TwoFactorAuthMethod.U2F to "".asText(),
            TwoFactorAuthMethod.REMEMBER to "".asText(),
            TwoFactorAuthMethod.DUO_ORGANIZATION to "".asText(),
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
            TwoFactorAuthMethod.DUO to "".asText(),
            TwoFactorAuthMethod.YUBI_KEY to "".asText(),
            TwoFactorAuthMethod.U2F to "".asText(),
            TwoFactorAuthMethod.REMEMBER to "".asText(),
            TwoFactorAuthMethod.DUO_ORGANIZATION to "".asText(),
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
}
