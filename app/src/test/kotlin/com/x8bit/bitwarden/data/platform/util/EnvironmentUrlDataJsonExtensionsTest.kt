package com.x8bit.bitwarden.data.platform.util

import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.ui.platform.manager.intent.model.AuthTabData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EnvironmentUrlDataJsonExtensionsTest {

    @Test
    fun `duoAuthTabData should return the correct AuthTabData for all environments`() {
        assertEquals(
            AuthTabData.HttpsScheme(
                host = "bitwarden.com",
                path = "duo-callback",
            ),
            EnvironmentUrlDataJson.DEFAULT_US.duoAuthTabData,
        )
        assertEquals(
            AuthTabData.HttpsScheme(
                host = "bitwarden.eu",
                path = "duo-callback",
            ),
            EnvironmentUrlDataJson.DEFAULT_EU.duoAuthTabData,
        )
        assertEquals(
            AuthTabData.HttpsScheme(
                host = "bitwarden.pw",
                path = "duo-callback",
            ),
            DEFAULT_INTERNAL_ENVIRONMENT_URL_DATA.duoAuthTabData,
        )
        assertEquals(
            AuthTabData.CustomScheme(
                callbackUrl = "bitwarden://duo-callback",
                callbackScheme = "bitwarden",
            ),
            DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.duoAuthTabData,
        )
    }

    @Test
    fun `webAuthnAuthTabData should return the correct AuthTabData for all environments`() {
        assertEquals(
            AuthTabData.HttpsScheme(
                host = "bitwarden.com",
                path = "webauthn-callback",
            ),
            EnvironmentUrlDataJson.DEFAULT_US.webAuthnAuthTabData,
        )
        assertEquals(
            AuthTabData.HttpsScheme(
                host = "bitwarden.eu",
                path = "webauthn-callback",
            ),
            EnvironmentUrlDataJson.DEFAULT_EU.webAuthnAuthTabData,
        )
        assertEquals(
            AuthTabData.HttpsScheme(
                host = "bitwarden.pw",
                path = "webauthn-callback",
            ),
            DEFAULT_INTERNAL_ENVIRONMENT_URL_DATA.webAuthnAuthTabData,
        )
        assertEquals(
            AuthTabData.CustomScheme(
                callbackUrl = "bitwarden://webauthn-callback",
                callbackScheme = "bitwarden",
            ),
            DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.webAuthnAuthTabData,
        )
    }

    @Test
    fun `ssoAuthTabData should return the correct AuthTabData for all environments`() {
        assertEquals(
            AuthTabData.HttpsScheme(
                host = "bitwarden.com",
                path = "sso-callback",
            ),
            EnvironmentUrlDataJson.DEFAULT_US.ssoAuthTabData,
        )
        assertEquals(
            AuthTabData.HttpsScheme(
                host = "bitwarden.eu",
                path = "sso-callback",
            ),
            EnvironmentUrlDataJson.DEFAULT_EU.ssoAuthTabData,
        )
        assertEquals(
            AuthTabData.HttpsScheme(
                host = "bitwarden.pw",
                path = "sso-callback",
            ),
            DEFAULT_INTERNAL_ENVIRONMENT_URL_DATA.ssoAuthTabData,
        )
        assertEquals(
            AuthTabData.CustomScheme(
                callbackUrl = "bitwarden://sso-callback",
                callbackScheme = "bitwarden",
            ),
            DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.ssoAuthTabData,
        )
    }
}

private val DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA = EnvironmentUrlDataJson(
    base = "base",
    api = "api",
    identity = "identity",
    icon = "icon",
    notifications = "notifications",
    webVault = "webVault",
    events = "events",
)

private val DEFAULT_INTERNAL_ENVIRONMENT_URL_DATA = DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.copy(
    base = "qa.vault.bitwarden.pw",
)
