package com.x8bit.bitwarden.ui.autofill.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContextExtensionsTest {
    private val testConfiguration: Configuration = mockk()
    private val testResources: Resources = mockk {
        every { this@mockk.configuration } returns testConfiguration
    }
    private val context: Context = mockk {
        every { this@mockk.resources } returns testResources
    }

    @Test
    fun `isSystemDarkMode should return true when UI_MODE_NIGHT_YES`() {
        // Setup
        testConfiguration.uiMode = Configuration.UI_MODE_NIGHT_YES

        // Test & Verify
        assertTrue(context.isSystemDarkMode)
    }

    @Test
    fun `isSystemDarkMode should return false when UI_MODE_NIGHT_NO`() {
        // Setup
        testConfiguration.uiMode = Configuration.UI_MODE_NIGHT_NO

        // Test & Verify
        assertFalse(context.isSystemDarkMode)
    }
}
