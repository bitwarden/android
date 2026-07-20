package com.bitwarden.data.repository.util

import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.data.repository.model.Environment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EnvironmentUrlsDataJsonExtensionsTest {
    @Test
    fun `toEnvironmentUrls should correctly convert US urls to the expected type`() {
        assertEquals(
            Environment.Prod.Us,
            EnvironmentUrlDataJson.DEFAULT_US.toEnvironmentUrls(),
        )
    }

    @Test
    fun `toEnvironmentUrls should correctly convert EU urls to the expected type`() {
        assertEquals(
            Environment.Prod.Eu,
            EnvironmentUrlDataJson.DEFAULT_EU.toEnvironmentUrls(),
        )
    }

    @Test
    fun `toEnvironmentUrls should correctly convert FedRAMP urls to the expected type`() {
        assertEquals(
            Environment.Prod.FedRamp,
            EnvironmentUrlDataJson.DEFAULT_FED_RAMP.toEnvironmentUrls(),
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
            Environment.Prod.Us,
            EnvironmentUrlDataJson.DEFAULT_US.toEnvironmentUrlsOrDefault(),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toEnvironmentUrlsOrDefault should correctly convert legacy US urls to the expected type`() {
        assertEquals(
            Environment.Prod.Us,
            EnvironmentUrlDataJson.DEFAULT_LEGACY_US.toEnvironmentUrlsOrDefault(),
        )
    }

    @Test
    fun `toEnvironmentUrlsOrDefault should correctly convert EU urls to the expected type`() {
        assertEquals(
            Environment.Prod.Eu,
            EnvironmentUrlDataJson.DEFAULT_EU.toEnvironmentUrlsOrDefault(),
        )
    }

    @Test
    fun `toEnvironmentUrlsOrDefault should correctly convert FedRAMP urls to the expected type`() {
        assertEquals(
            Environment.Prod.FedRamp,
            EnvironmentUrlDataJson.DEFAULT_FED_RAMP.toEnvironmentUrlsOrDefault(),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toEnvironmentUrlsOrDefault should correctly convert legacy EU urls to the expected type`() {
        assertEquals(
            Environment.Prod.Eu,
            EnvironmentUrlDataJson.DEFAULT_LEGACY_EU.toEnvironmentUrlsOrDefault(),
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
            Environment.Prod.Us,
            (null as EnvironmentUrlDataJson?).toEnvironmentUrlsOrDefault(),
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
