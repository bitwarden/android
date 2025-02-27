package com.bitwarden.authenticator.data.platform.repository.util

import com.bitwarden.authenticator.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class EnvironmentUrlsDataJsonExtensionsTest {
    @Test
    fun `baseApiUrl should return base if it is present`() {
        Assertions.assertEquals(
            "base/api",
            DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.baseApiUrl,
        )
    }

    @Test
    fun `baseApiUrl should return api value if base is empty`() {
        Assertions.assertEquals(
            "api",
            DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.copy(
                base = "",
            ).baseApiUrl,
        )
    }

    @Test
    fun `baseApiUrl should return default url if base is empty and api is null`() {
        Assertions.assertEquals(
            "https://api.bitwarden.com",
            DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.copy(
                base = "",
                api = null,
            ).baseApiUrl,
        )
    }

    @Test
    fun `baseWebVaultUrlOrNull should return webVault when populated`() {
        val result =
            DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.baseWebVaultUrlOrNull
        Assertions.assertEquals("webVault", result)
    }

    @Test
    fun `baseWebVaultUrlOrNull should return base when webvault is not populated`() {
        val result =
            DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA
                .copy(webVault = null)
                .baseWebVaultUrlOrNull
        Assertions.assertEquals("base", result)
    }

    @Test
    fun `baseWebVaultUrlOrNull should return null when webvault and base are not populated`() {
        val result =
            DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA
                .copy(
                    webVault = null,
                    base = "",
                )
                .baseWebVaultUrlOrNull
        Assertions.assertNull(result)
    }

    @Test
    fun `baseWebVaultUrlOrDefault should return webVault when populated`() {
        val result =
            DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.baseWebVaultUrlOrDefault
        Assertions.assertEquals("webVault", result)
    }

    @Test
    fun `baseWebVaultUrlOrDefault should return base when webvault is not populated`() {
        val result =
            DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA
                .copy(webVault = null)
                .baseWebVaultUrlOrDefault
        Assertions.assertEquals("base", result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `baseWebVaultUrlOrDefault should return the default when webvault and base are not populated`() {
        val result =
            DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA
                .copy(
                    webVault = null,
                    base = "",
                )
                .baseWebVaultUrlOrDefault
        Assertions.assertEquals("https://vault.bitwarden.com", result)
    }

    @Test
    fun `baseWebSendUrl should return the correct result when webVault when populated`() {
        val result =
            DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.baseWebSendUrl
        Assertions.assertEquals("webVault/#/send/", result)
    }

    @Test
    fun `baseWebSendUrl should return the correct result when webvault is not populated`() {
        val result =
            DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA
                .copy(webVault = null)
                .baseWebSendUrl
        Assertions.assertEquals("base/#/send/", result)
    }

    @Test
    fun `baseWebSendUrl should return the default when webvault and base are not populated`() {
        val result =
            DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA
                .copy(
                    webVault = null,
                    base = "",
                )
                .baseWebSendUrl
        Assertions.assertEquals("https://send.bitwarden.com/#", result)
    }

    @Test
    fun `labelOrBaseUrlHost should correctly convert US environment to the correct label`() {
        val environment =
            EnvironmentUrlDataJson.Companion.DEFAULT_US
        Assertions.assertEquals(
            com.bitwarden.authenticator.data.platform.repository.model.Environment.Us.label,
            environment.labelOrBaseUrlHost,
        )
    }

    @Test
    fun `labelOrBaseUrlHost should correctly convert EU environment to the correct label`() {
        val environment =
            EnvironmentUrlDataJson.Companion.DEFAULT_EU
        Assertions.assertEquals(
            com.bitwarden.authenticator.data.platform.repository.model.Environment.Eu.label,
            environment.labelOrBaseUrlHost,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `labelOrBaseUrlHost should correctly convert self hosted environment to the correct label`() {
        val environment =
            EnvironmentUrlDataJson(base = "https://vault.qa.bitwarden.pw")
        Assertions.assertEquals(
            "vault.qa.bitwarden.pw",
            environment.labelOrBaseUrlHost,
        )
    }

    @Test
    fun `toEnvironmentUrls should correctly convert US urls to the expected type`() {
        Assertions.assertEquals(
            com.bitwarden.authenticator.data.platform.repository.model.Environment.Us,
            EnvironmentUrlDataJson.Companion.DEFAULT_US.toEnvironmentUrls(),
        )
    }

    @Test
    fun `toEnvironmentUrls should correctly convert EU urls to the expected type`() {
        Assertions.assertEquals(
            com.bitwarden.authenticator.data.platform.repository.model.Environment.Eu,
            EnvironmentUrlDataJson.Companion.DEFAULT_EU.toEnvironmentUrls(),
        )
    }

    @Test
    fun `toEnvironmentUrls should correctly convert custom urls to the expected type`() {
        Assertions.assertEquals(
            com.bitwarden.authenticator.data.platform.repository.model.Environment.SelfHosted(
                environmentUrlData = DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA,
            ),
            DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.toEnvironmentUrls(),
        )
    }

    @Test
    fun `toEnvironmentUrlsOrDefault should correctly convert US urls to the expected type`() {
        Assertions.assertEquals(
            com.bitwarden.authenticator.data.platform.repository.model.Environment.Us,
            EnvironmentUrlDataJson.Companion.DEFAULT_US.toEnvironmentUrlsOrDefault(),
        )
    }

    @Test
    fun `toEnvironmentUrlsOrDefault should correctly convert EU urls to the expected type`() {
        Assertions.assertEquals(
            com.bitwarden.authenticator.data.platform.repository.model.Environment.Eu,
            EnvironmentUrlDataJson.Companion.DEFAULT_EU.toEnvironmentUrlsOrDefault(),
        )
    }

    @Test
    fun `toEnvironmentUrlsOrDefault should correctly convert custom urls to the expected type`() {
        Assertions.assertEquals(
            com.bitwarden.authenticator.data.platform.repository.model.Environment.SelfHosted(
                environmentUrlData = DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA,
            ),
            DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.toEnvironmentUrlsOrDefault(),
        )
    }

    @Test
    fun `toEnvironmentUrlsOrDefault should convert null types to US values`() {
        Assertions.assertEquals(
            com.bitwarden.authenticator.data.platform.repository.model.Environment.Us,
            (null as EnvironmentUrlDataJson?).toEnvironmentUrlsOrDefault(),
        )
    }

    @Test
    fun `toIconBaseurl should return icon if value is present`() {
        Assertions.assertEquals(
            DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.baseIconUrl,
            "icon",
        )
    }

    @Test
    fun `toIconBaseurl should return base value if icon is null`() {
        Assertions.assertEquals(
            DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA
                .copy(icon = null)
                .baseIconUrl,
            "base/icons",
        )
    }

    @Test
    fun `toIconBaseurl should return default url if base is empty and icon is null`() {
        val expectedUrl = "https://icons.bitwarden.net"

        Assertions.assertEquals(
            expectedUrl,
            DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA
                .copy(
                    base = "",
                    icon = null,
                )
                .baseIconUrl,
        )
    }

    @Test
    fun `toBaseWebVaultImportUrl should return correct url if webVault is empty`() {
        val expectedUrl = "base/#/tools/import"

        Assertions.assertEquals(
            expectedUrl,
            DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.copy(
                webVault = null,
            )
                .toBaseWebVaultImportUrl,
        )
    }

    @Test
    fun `toBaseWebVaultImportUrl should correctly convert to the import url`() {
        val expectedUrl = "webVault/#/tools/import"

        Assertions.assertEquals(
            expectedUrl,
            DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.toBaseWebVaultImportUrl,
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
