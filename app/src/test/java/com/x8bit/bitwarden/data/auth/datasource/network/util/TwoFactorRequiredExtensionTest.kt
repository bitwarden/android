package com.x8bit.bitwarden.data.auth.datasource.network.util

import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.TwoFactorAuthMethod
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TwoFactorRequiredExtensionTest {
    @Test
    fun `availableAuthMethods returns the expected value`() {
        val subject = GetTokenResponseJson.TwoFactorRequired(
            authMethodsData = mapOf(
                TwoFactorAuthMethod.EMAIL to JsonObject(
                    mapOf("Email" to JsonPrimitive("ex***@email.com")),
                ),
                TwoFactorAuthMethod.AUTHENTICATOR_APP to JsonObject(mapOf("Email" to JsonNull)),
            ),
            captchaToken = null,
            ssoToken = null,
            twoFactorProviders = null,
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
    fun `twoFactorDuoAuthUrl returns the expected value when auth method is DUO`() {
        val subject = GetTokenResponseJson.TwoFactorRequired(
            authMethodsData = mapOf(
                TwoFactorAuthMethod.DUO to JsonObject(
                    mapOf("AuthUrl" to JsonPrimitive("Bitwarden")),
                ),
                TwoFactorAuthMethod.AUTHENTICATOR_APP to JsonObject(mapOf("AuthUrl" to JsonNull)),
            ),
            captchaToken = null,
            ssoToken = null,
            twoFactorProviders = null,
        )
        assertEquals("Bitwarden", subject.twoFactorDuoAuthUrl)
    }

    @Test
    fun `twoFactorDuoAuthUrl returns the expected value when auth method is DUO_ORGANIZATION`() {
        val subject = GetTokenResponseJson.TwoFactorRequired(
            authMethodsData = mapOf(
                TwoFactorAuthMethod.DUO_ORGANIZATION to JsonObject(
                    mapOf("AuthUrl" to JsonPrimitive("Bitwarden")),
                ),
                TwoFactorAuthMethod.AUTHENTICATOR_APP to JsonObject(mapOf("AuthUrl" to JsonNull)),
            ),
            captchaToken = null,
            ssoToken = null,
            twoFactorProviders = null,
        )
        assertEquals("Bitwarden", subject.twoFactorDuoAuthUrl)
    }

    @Test
    fun `twoFactorDuoAuthUrl returns null when no DUO AuthUrl is present`() {
        val subject = GetTokenResponseJson.TwoFactorRequired(
            authMethodsData = mapOf(
                TwoFactorAuthMethod.AUTHENTICATOR_APP to JsonObject(mapOf("AuthUrl" to JsonNull)),
            ),
            captchaToken = null,
            ssoToken = null,
            twoFactorProviders = null,
        )
        assertNull(subject.twoFactorDuoAuthUrl)
    }

    @Test
    fun `twoFactorDisplayEmail returns the expected value`() {
        val subject = GetTokenResponseJson.TwoFactorRequired(
            authMethodsData = mapOf(
                TwoFactorAuthMethod.EMAIL to JsonObject(
                    mapOf("Email" to JsonPrimitive("ex***@email.com")),
                ),
                TwoFactorAuthMethod.AUTHENTICATOR_APP to JsonObject(mapOf("Email" to JsonNull)),
            ),
            captchaToken = null,
            ssoToken = null,
            twoFactorProviders = null,
        )
        assertEquals("ex***@email.com", subject.twoFactorDisplayEmail)
    }

    @Test
    fun `twoFactorDisplayEmail returns the expected value when null`() {
        val subject = GetTokenResponseJson.TwoFactorRequired(
            authMethodsData = mapOf(
                TwoFactorAuthMethod.AUTHENTICATOR_APP to JsonObject(mapOf("Email" to JsonNull)),
            ),
            captchaToken = null,
            ssoToken = null,
            twoFactorProviders = null,
        )
        assertEquals("", subject.twoFactorDisplayEmail)
    }

    @Test
    fun `preferredAuthMethod returns the expected value`() {
        val subject = GetTokenResponseJson.TwoFactorRequired(
            authMethodsData = mapOf(
                TwoFactorAuthMethod.EMAIL to JsonObject(
                    mapOf("Email" to JsonPrimitive("ex***@email.com")),
                ),
                TwoFactorAuthMethod.AUTHENTICATOR_APP to JsonObject(mapOf("Email" to JsonNull)),
            ),
            captchaToken = null,
            ssoToken = null,
            twoFactorProviders = null,
        )
        assertEquals(TwoFactorAuthMethod.AUTHENTICATOR_APP, subject.preferredAuthMethod)
    }

    @Test
    fun `twoFactorDuoAuthUrl returns the expected value for DUO`() {
        val authUrl = "vault.bitwarden.com"
        val subject = GetTokenResponseJson.TwoFactorRequired(
            authMethodsData = mapOf(
                TwoFactorAuthMethod.DUO to JsonObject(
                    mapOf("AuthUrl" to JsonPrimitive(authUrl)),
                ),
            ),
            captchaToken = null,
            ssoToken = null,
            twoFactorProviders = null,
        )
        assertEquals(authUrl, subject.twoFactorDuoAuthUrl)
    }

    @Test
    fun `twoFactorDuoAuthUrl returns the expected value for DUO_ORGANIZATION`() {
        val authUrl = "vault.bitwarden.com"
        val subject = GetTokenResponseJson.TwoFactorRequired(
            authMethodsData = mapOf(
                TwoFactorAuthMethod.DUO_ORGANIZATION to JsonObject(
                    mapOf("AuthUrl" to JsonPrimitive(authUrl)),
                ),
            ),
            captchaToken = null,
            ssoToken = null,
            twoFactorProviders = null,
        )
        assertEquals(authUrl, subject.twoFactorDuoAuthUrl)
    }

    @Test
    fun `webAuthRpId returns the expected value`() {
        val rpId = "vault.bitwarden.com"
        val subject = GetTokenResponseJson.TwoFactorRequired(
            authMethodsData = mapOf(
                TwoFactorAuthMethod.WEB_AUTH to JsonObject(
                    mapOf("rpId" to JsonPrimitive(rpId)),
                ),
            ),
            captchaToken = null,
            ssoToken = null,
            twoFactorProviders = null,
        )
        assertEquals(rpId, subject.webAuthRpId)
    }

    @Test
    fun `webAuthUserVerification returns the expected value`() {
        val userVerification = "discouraged"
        val subject = GetTokenResponseJson.TwoFactorRequired(
            authMethodsData = mapOf(
                TwoFactorAuthMethod.WEB_AUTH to JsonObject(
                    mapOf("userVerification" to JsonPrimitive(userVerification)),
                ),
            ),
            captchaToken = null,
            ssoToken = null,
            twoFactorProviders = null,
        )
        assertEquals(userVerification, subject.webAuthUserVerification)
    }

    @Test
    fun `webAuthChallenge returns the expected value`() {
        val challenge = "987t34478t9rxq7t8n"
        val subject = GetTokenResponseJson.TwoFactorRequired(
            authMethodsData = mapOf(
                TwoFactorAuthMethod.WEB_AUTH to JsonObject(
                    mapOf("challenge" to JsonPrimitive(challenge)),
                ),
            ),
            captchaToken = null,
            ssoToken = null,
            twoFactorProviders = null,
        )
        assertEquals(challenge, subject.webAuthChallenge)
    }

    @Test
    fun `webAuthAllowCredentials returns the expected value`() {
        val credential = "98426435782"
        val subject = GetTokenResponseJson.TwoFactorRequired(
            authMethodsData = mapOf(
                TwoFactorAuthMethod.WEB_AUTH to JsonObject(
                    mapOf(
                        "allowCredentials" to JsonArray(
                            listOf(
                                JsonObject(
                                    mapOf(
                                        "type" to JsonPrimitive("public-key"),
                                        "id" to JsonPrimitive(credential),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            captchaToken = null,
            ssoToken = null,
            twoFactorProviders = null,
        )
        assertNotNull(subject.webAuthAllowCredentials)
    }
}
