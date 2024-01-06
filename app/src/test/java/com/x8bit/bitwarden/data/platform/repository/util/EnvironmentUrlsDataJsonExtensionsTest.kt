package com.x8bit.bitwarden.data.platform.repository.util

import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EnvironmentUrlsDataJsonExtensionsTest {
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
        val environmentUrlData = EnvironmentUrlDataJson(
            base = "base",
            api = "api",
            identity = "identity",
            icon = "icon",
            notifications = "notifications",
            webVault = "webVault",
            events = "events",
        )
        assertEquals(
            Environment.SelfHosted(
                environmentUrlData = environmentUrlData,
            ),
            environmentUrlData.toEnvironmentUrls(),
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
        val environmentUrlData = EnvironmentUrlDataJson(
            base = "base",
            api = "api",
            identity = "identity",
            icon = "icon",
            notifications = "notifications",
            webVault = "webVault",
            events = "events",
        )
        assertEquals(
            Environment.SelfHosted(
                environmentUrlData = environmentUrlData,
            ),
            environmentUrlData.toEnvironmentUrlsOrDefault(),
        )
    }

    @Test
    fun `toEnvironmentUrlsOrDefault should convert null types to US values`() {
        assertEquals(
            Environment.Us,
            (null as EnvironmentUrlDataJson?).toEnvironmentUrlsOrDefault(),
        )
    }
}
