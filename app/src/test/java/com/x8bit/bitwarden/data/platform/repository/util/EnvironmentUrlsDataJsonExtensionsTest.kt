package com.x8bit.bitwarden.data.platform.repository.util

import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EnvironmentUrlsDataJsonExtensionsTest {
    @Test
    fun `baseWebVaultUrlOrNull should return webVault when populated`() {
        val result = DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.baseWebVaultUrlOrNull
        assertEquals("webVault", result)
    }

    @Test
    fun `baseWebVaultUrlOrNull should return base when webvault is not populated`() {
        val result = DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA
            .copy(webVault = null)
            .baseWebVaultUrlOrNull
        assertEquals("base", result)
    }

    @Test
    fun `baseWebVaultUrlOrNull should return null when webvault and base are not populated`() {
        val result = DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA
            .copy(
                webVault = null,
                base = "",
            )
            .baseWebVaultUrlOrNull
        assertNull(result)
    }

    @Test
    fun `baseWebVaultUrlOrDefault should return webVault when populated`() {
        val result = DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.baseWebVaultUrlOrDefault
        assertEquals("webVault", result)
    }

    @Test
    fun `baseWebVaultUrlOrDefault should return base when webvault is not populated`() {
        val result = DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA
            .copy(webVault = null)
            .baseWebVaultUrlOrDefault
        assertEquals("base", result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `baseWebVaultUrlOrDefault should return the default when webvault and base are not populated`() {
        val result = DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA
            .copy(
                webVault = null,
                base = "",
            )
            .baseWebVaultUrlOrDefault
        assertEquals("https://vault.bitwarden.com", result)
    }

    @Test
    fun `baseWebSendUrl should return the correct result when webVault when populated`() {
        val result = DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.baseWebSendUrl
        assertEquals("webVault/#/send/", result)
    }

    @Test
    fun `baseWebSendUrl should return the correct result when webvault is not populated`() {
        val result = DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA
            .copy(webVault = null)
            .baseWebSendUrl
        assertEquals("base/#/send/", result)
    }

    @Test
    fun `baseWebSendUrl should return the default when webvault and base are not populated`() {
        val result = DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA
            .copy(
                webVault = null,
                base = "",
            )
            .baseWebSendUrl
        assertEquals("https://send.bitwarden.com/#", result)
    }

    @Test
    fun `labelOrBaseUrlHost should correctly convert US environment to the correct label`() {
        val environment = EnvironmentUrlDataJson.DEFAULT_US
        assertEquals(
            Environment.Us.label,
            environment.labelOrBaseUrlHost,
        )
    }

    @Test
    fun `labelOrBaseUrlHost should correctly convert EU environment to the correct label`() {
        val environment = EnvironmentUrlDataJson.DEFAULT_EU
        assertEquals(
            Environment.Eu.label,
            environment.labelOrBaseUrlHost,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `labelOrBaseUrlHost should correctly convert self hosted environment to the correct label`() {
        val environment = EnvironmentUrlDataJson(base = "https://vault.qa.bitwarden.pw")
        assertEquals(
            "vault.qa.bitwarden.pw",
            environment.labelOrBaseUrlHost,
        )
    }

    @Test
    fun `toEnvironmentUrls should correctly convert US urls to the expected type`() {
        assertEquals(
            Environment.Us,
            EnvironmentUrlDataJson.DEFAULT_US.toEnvironmentUrls(),
        )
    }

    @Test
    fun `toEnvironmentUrls should correctly convert EU urls to the expected type`() {
        assertEquals(
            Environment.Eu,
            EnvironmentUrlDataJson.DEFAULT_EU.toEnvironmentUrls(),
        )
    }

    @Test
    fun `toEnvironmentUrls should correctly convert custom urls to the expected type`() {
        assertEquals(
            Environment.SelfHosted(
                environmentUrlData = DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA,
            ),
            DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.toEnvironmentUrls(),
        )
    }

    @Test
    fun `toEnvironmentUrlsOrDefault should correctly convert US urls to the expected type`() {
        assertEquals(
            Environment.Us,
            EnvironmentUrlDataJson.DEFAULT_US.toEnvironmentUrlsOrDefault(),
        )
    }

    @Test
    fun `toEnvironmentUrlsOrDefault should correctly convert EU urls to the expected type`() {
        assertEquals(
            Environment.Eu,
            EnvironmentUrlDataJson.DEFAULT_EU.toEnvironmentUrlsOrDefault(),
        )
    }

    @Test
    fun `toEnvironmentUrlsOrDefault should correctly convert custom urls to the expected type`() {
        assertEquals(
            Environment.SelfHosted(
                environmentUrlData = DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA,
            ),
            DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.toEnvironmentUrlsOrDefault(),
        )
    }

    @Test
    fun `toEnvironmentUrlsOrDefault should convert null types to US values`() {
        assertEquals(
            Environment.Us,
            (null as EnvironmentUrlDataJson?).toEnvironmentUrlsOrDefault(),
        )
    }

    @Test
    fun `toIconBaseurl should return icon if value is present`() {
        assertEquals(
            DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.baseIconUrl,
            "icon",
        )
    }

    @Test
    fun `toIconBaseurl should return base value if icon is null`() {
        assertEquals(
            DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA
                .copy(icon = null)
                .baseIconUrl,
            "base/icons",
        )
    }

    @Test
    fun `toIconBaseurl should return default url if base is empty and icon is null`() {
        val expectedUrl = "https://icons.bitwarden.net/"

        assertEquals(
            expectedUrl,
            DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA
                .copy(
                    base = "",
                    icon = null,
                )
                .baseIconUrl,
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
