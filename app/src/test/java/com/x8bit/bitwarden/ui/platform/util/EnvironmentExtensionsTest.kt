package com.x8bit.bitwarden.ui.platform.util

import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.ui.platform.base.util.asText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EnvironmentExtensionsTest {
    @Test
    fun `labelOrBaseUrlHost should correctly convert US environment to the correct label`() {
        val environment = Environment.Us
        assertEquals(
            environment.label,
            environment.labelOrBaseUrlHost,
        )
    }

    @Test
    fun `labelOrBaseUrlHost should correctly convert EU environment to the correct label`() {
        val environment = Environment.Eu
        assertEquals(
            environment.label,
            environment.labelOrBaseUrlHost,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `labelOrBaseUrlHost should correctly convert self hosted environment to the correct label`() {
        val environment = Environment.SelfHosted(
            environmentUrlData = EnvironmentUrlDataJson(base = "https://vault.bitwarden.com"),
        )
        assertEquals(
            "vault.bitwarden.com".asText(),
            environment.labelOrBaseUrlHost,
        )
    }
}
