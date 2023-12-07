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
}
