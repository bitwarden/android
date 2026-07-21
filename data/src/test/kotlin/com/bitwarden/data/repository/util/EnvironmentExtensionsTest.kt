package com.bitwarden.data.repository.util

import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.data.repository.model.Environment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EnvironmentExtensionsTest {
    @Test
    fun `baseApiUrl should return api if it is present`() {
        assertEquals(
            "api",
            Environment.SelfHosted(DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA).baseApiUrl,
        )
    }

    @Test
    fun `baseApiUrl should return base value if api is empty`() {
        assertEquals(
            "base/api",
            Environment.SelfHosted(DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.copy(api = "")).baseApiUrl,
        )
    }

    @Test
    fun `baseApiUrl should return default url if base is empty and api is null`() {
        assertEquals(
            "https://api.bitwarden.com",
            Environment
                .SelfHosted(DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.copy(base = "", api = ""))
                .baseApiUrl,
        )
    }

    @Test
    fun `baseEventsUrl should return events if it is present`() {
        assertEquals(
            "events",
            Environment.SelfHosted(DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA).baseEventsUrl,
        )
    }

    @Test
    fun `baseEventsUrl should return base value if events is empty`() {
        assertEquals(
            "base/events",
            Environment
                .SelfHosted(DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.copy(events = ""))
                .baseEventsUrl,
        )
    }

    @Test
    fun `baseEventsUrl should return default url if base is empty and events is null`() {
        assertEquals(
            "https://events.bitwarden.com",
            Environment
                .SelfHosted(DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.copy(base = "", events = ""))
                .baseEventsUrl,
        )
    }

    @Test
    fun `baseIdentityUrl should return identity if value is present`() {
        assertEquals(
            "identity",
            Environment.SelfHosted(DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA).baseIdentityUrl,
        )
    }

    @Test
    fun `baseIdentityUrl should return base value if identity is null`() {
        assertEquals(
            "base/identity",
            Environment
                .SelfHosted(DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.copy(identity = null))
                .baseIdentityUrl,
        )
    }

    @Test
    fun `baseIdentityUrl should return default url if base is empty and identity is null`() {
        val expectedUrl = "https://identity.bitwarden.com"

        assertEquals(
            expectedUrl,
            Environment
                .SelfHosted(DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.copy(base = "", identity = null))
                .baseIdentityUrl,
        )
    }

    @Test
    fun `baseWebVaultUrlOrNull should return webVault when populated`() {
        val result = Environment
            .SelfHosted(DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA)
            .baseWebVaultUrlOrNull
        assertEquals("webVault", result)
    }

    @Test
    fun `baseWebVaultUrlOrNull should return base when webvault is not populated`() {
        val result = Environment
            .SelfHosted(DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.copy(webVault = null))
            .baseWebVaultUrlOrNull
        assertEquals("base", result)
    }

    @Test
    fun `baseWebVaultUrlOrNull should return null when webvault and base are not populated`() {
        val result = Environment
            .SelfHosted(DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.copy(base = "", webVault = null))
            .baseWebVaultUrlOrNull
        assertNull(result)
    }

    @Test
    fun `baseWebVaultUrlOrDefault should return webVault when populated`() {
        val result = Environment
            .SelfHosted(DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA)
            .baseWebVaultUrlOrDefault
        assertEquals("webVault", result)
    }

    @Test
    fun `baseWebVaultUrlOrDefault should return base when webvault is not populated`() {
        val result = Environment
            .SelfHosted(DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.copy(webVault = null))
            .baseWebVaultUrlOrDefault
        assertEquals("base", result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `baseWebVaultUrlOrDefault should return the default when webvault and base are not populated`() {
        val result = Environment
            .SelfHosted(DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.copy(base = "", webVault = null))
            .baseWebVaultUrlOrDefault
        assertEquals("https://vault.bitwarden.com", result)
    }

    @Test
    fun `baseWebSendUrl should return the correct result when webVault when populated`() {
        val result = Environment.SelfHosted(DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA).baseWebSendUrl
        assertEquals("webVault/#/send/", result)
    }

    @Test
    fun `baseWebSendUrl should return the correct result when webvault is not populated`() {
        val result = Environment
            .SelfHosted(DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.copy(webVault = null))
            .baseWebSendUrl
        assertEquals("base/#/send/", result)
    }

    @Test
    fun `baseWebSendUrl should return the default when webvault and base are not populated`() {
        val result = Environment
            .SelfHosted(DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.copy(base = "", webVault = null))
            .baseWebSendUrl
        assertEquals("https://send.bitwarden.com/#", result)
    }

    @Test
    fun `baseWebSendUrl should return the modified webvault when not in the US`() {
        val result = Environment
            .SelfHosted(
                environmentUrlData = DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.copy(
                    base = "",
                    webVault = "https://vault.bitwarden.com",
                ),
            )
            .baseWebSendUrl
        assertEquals("https://vault.bitwarden.com/#/send/", result)
    }

    @Test
    fun `baseIconUrl should return icon if value is present`() {
        assertEquals(
            Environment.SelfHosted(DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA).baseIconUrl,
            "icon",
        )
    }

    @Test
    fun `baseIconUrl should return base value if icon is null`() {
        assertEquals(
            Environment
                .SelfHosted(DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.copy(icon = null))
                .baseIconUrl,
            "base/icons",
        )
    }

    @Test
    fun `baseIconUrl should return default url if base is empty and icon is null`() {
        val expectedUrl = "https://icons.bitwarden.net"

        assertEquals(
            expectedUrl,
            Environment
                .SelfHosted(DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.copy(base = "", icon = null))
                .baseIconUrl,
        )
    }

    @Test
    fun `toBaseWebVaultImportUrl should return correct url if webVault is empty`() {
        val expectedUrl = "base/#/tools/import"

        assertEquals(
            expectedUrl,
            Environment
                .SelfHosted(DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA.copy(webVault = null))
                .toBaseWebVaultImportUrl,
        )
    }

    @Test
    fun `toBaseWebVaultImportUrl should correctly convert to the import url`() {
        val expectedUrl = "webVault/#/tools/import"

        assertEquals(
            expectedUrl,
            Environment
                .SelfHosted(DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA)
                .toBaseWebVaultImportUrl,
        )
    }

    @Test
    fun `labelOrBaseUrlHost should correctly convert US environment to the correct label`() {
        assertEquals(
            "bitwarden.com",
            Environment.Prod.Us.labelOrBaseUrlHost,
        )
    }

    @Test
    fun `labelOrBaseUrlHost should correctly convert EU environment to the correct label`() {
        assertEquals(
            "bitwarden.eu",
            Environment.Prod.Eu.labelOrBaseUrlHost,
        )
    }

    @Test
    fun `labelOrBaseUrlHost should correctly convert FedRAMP environment to the correct label`() {
        assertEquals(
            "bitwarden-gov.com",
            Environment.Prod.FedRamp.labelOrBaseUrlHost,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `labelOrBaseUrlHost should correctly convert self hosted environment to the correct label`() {
        val environment = Environment.SelfHosted(
            environmentUrlData = EnvironmentUrlDataJson(base = "https://vault.qa.bitwarden.pw"),
        )
        assertEquals(
            "vault.qa.bitwarden.pw",
            environment.labelOrBaseUrlHost,
        )
    }
}

private val DEFAULT_CUSTOM_ENVIRONMENT_URL_DATA: EnvironmentUrlDataJson = EnvironmentUrlDataJson(
    base = "base",
    api = "api",
    identity = "identity",
    icon = "icon",
    notifications = "notifications",
    webVault = "webVault",
    events = "events",
    keyUri = null,
)
