package com.x8bit.bitwarden.data.auth.datasource.network.util

import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.TwoFactorAuthMethod
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class TwoFactorRequiredExtensionTest {
    @Test
    fun `availableAuthMethods returns the expected value`() {
        val subject = GetTokenResponseJson.TwoFactorRequired(
            authMethodsData = mapOf(
                TwoFactorAuthMethod.EMAIL to mapOf("Email" to "ex***@email.com"),
                TwoFactorAuthMethod.AUTHENTICATOR_APP to mapOf("Email" to null),
            ),
            captchaToken = null,
            ssoToken = null,
        )
        assertEquals(
            listOf(
                TwoFactorAuthMethod.EMAIL,
                TwoFactorAuthMethod.AUTHENTICATOR_APP,
                TwoFactorAuthMethod.RECOVERY_CODE,
            ),
            subject.availableAuthMethods,
        )
    }

    @Test
    fun `twoFactorDisplayEmail returns the expected value`() {
        val subject = GetTokenResponseJson.TwoFactorRequired(
            authMethodsData = mapOf(
                TwoFactorAuthMethod.EMAIL to mapOf("Email" to "ex***@email.com"),
                TwoFactorAuthMethod.AUTHENTICATOR_APP to mapOf("Email" to null),
            ),
            captchaToken = null,
            ssoToken = null,
        )
        assertEquals("ex***@email.com", subject.twoFactorDisplayEmail)
    }

    @Test
    fun `twoFactorDisplayEmail returns the expected value when null`() {
        val subject = GetTokenResponseJson.TwoFactorRequired(
            authMethodsData = mapOf(
                TwoFactorAuthMethod.AUTHENTICATOR_APP to mapOf("Email" to null),
            ),
            captchaToken = null,
            ssoToken = null,
        )
        assertEquals("", subject.twoFactorDisplayEmail)
    }

    @Test
    fun `preferredAuthMethod returns the expected value`() {
        val subject = GetTokenResponseJson.TwoFactorRequired(
            authMethodsData = mapOf(
                TwoFactorAuthMethod.EMAIL to mapOf("Email" to "ex***@email.com"),
                TwoFactorAuthMethod.AUTHENTICATOR_APP to mapOf("Email" to null),
            ),
            captchaToken = null,
            ssoToken = null,
        )
        assertEquals(TwoFactorAuthMethod.AUTHENTICATOR_APP, subject.preferredAuthMethod)
    }
}
