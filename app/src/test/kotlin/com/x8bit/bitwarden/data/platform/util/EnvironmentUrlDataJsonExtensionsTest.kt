package com.x8bit.bitwarden.data.platform.util

import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.ui.platform.manager.intent.model.AuthTabData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EnvironmentUrlDataJsonExtensionsTest {

    @Test
    fun `duoAuthTabData should return the correct AuthTabData for all environments`() {
        // TODO: PM-26577 Update these to use a "HttpsScheme"
        assertEquals(
            AuthTabData.CustomScheme(
                callbackUrl = "bitwarden://duo-callback",
                callbackScheme = "bitwarden",
            ),
            EnvironmentUrlDataJson.DEFAULT_US.duoAuthTabData,
        )
        assertEquals(
            AuthTabData.CustomScheme(
                callbackUrl = "bitwarden://duo-callback",
                callbackScheme = "bitwarden",
            ),
            EnvironmentUrlDataJson.DEFAULT_EU.duoAuthTabData,
        )
        assertEquals(
            AuthTabData.CustomScheme(
                callbackUrl = "bitwarden://duo-callback",
                callbackScheme = "bitwarden",
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
        // TODO: PM-26577 Update these to use a "HttpsScheme"
        assertEquals(
            AuthTabData.CustomScheme(
                callbackUrl = "bitwarden://webauthn-callback",
                callbackScheme = "bitwarden",
            ),
            EnvironmentUrlDataJson.DEFAULT_US.webAuthnAuthTabData,
        )
        assertEquals(
            AuthTabData.CustomScheme(
                callbackUrl = "bitwarden://webauthn-callback",
                callbackScheme = "bitwarden",
            ),
            EnvironmentUrlDataJson.DEFAULT_EU.webAuthnAuthTabData,
        )
        assertEquals(
            AuthTabData.CustomScheme(
                callbackUrl = "bitwarden://webauthn-callback",
                callbackScheme = "bitwarden",
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
        // TODO: PM-26577 Update these to use a "HttpsScheme"
        assertEquals(
            AuthTabData.CustomScheme(
                callbackUrl = "bitwarden://sso-callback",
                callbackScheme = "bitwarden",
            ),
            EnvironmentUrlDataJson.DEFAULT_US.ssoAuthTabData,
        )
        assertEquals(
            AuthTabData.CustomScheme(
                callbackUrl = "bitwarden://sso-callback",
                callbackScheme = "bitwarden",
            ),
            EnvironmentUrlDataJson.DEFAULT_EU.ssoAuthTabData,
        )
        assertEquals(
            AuthTabData.CustomScheme(
                callbackUrl = "bitwarden://sso-callback",
                callbackScheme = "bitwarden",
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
