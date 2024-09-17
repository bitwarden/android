package com.x8bit.bitwarden.data.autofill.accessibility.util

import android.content.Context
import android.provider.Settings
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ContextExtensionsTest {

    @BeforeEach
    fun setup() {
        mockkStatic(Settings.Secure::getString)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Settings.Secure::getString)
    }

    @Test
    fun `isAccessibilityServiceEnabled with null package name returns false`() {
        val context: Context = mockk {
            every { applicationContext } returns this
            every { packageName } returns null
        }

        assertFalse(context.isAccessibilityServiceEnabled)
    }

    @Test
    fun `isAccessibilityServiceEnabled with null secure string returns false`() {
        val context: Context = mockk {
            every { applicationContext } returns this
            every { packageName } returns "com.x8bit.bitwarden"
            every { contentResolver } returns mockk()
        }
        mockkSettingsSecureGetString(value = null)

        assertFalse(context.isAccessibilityServiceEnabled)
    }

    @Test
    fun `isAccessibilityServiceEnabled with incorrect secure string returns false`() {
        val context: Context = mockk {
            every { applicationContext } returns this
            every { packageName } returns "com.x8bit.bitwarden"
            every { contentResolver } returns mockk()
        }
        @Suppress("MaxLineLength")
        mockkSettingsSecureGetString(
            value = "com.x8bit.bitwarden.dev/com.x8bit.bitwarden.Accessibility.AccessibilityService",
        )

        assertFalse(context.isAccessibilityServiceEnabled)
    }

    @Test
    fun `isAccessibilityServiceEnabled with correct secure string returns true`() {
        val context: Context = mockk {
            every { applicationContext } returns this
            every { packageName } returns "com.x8bit.bitwarden"
            every { contentResolver } returns mockk()
        }
        mockkSettingsSecureGetString(
            value = "com.x8bit.bitwarden/com.x8bit.bitwarden.Accessibility.AccessibilityService",
        )

        assertTrue(context.isAccessibilityServiceEnabled)
    }

    private fun mockkSettingsSecureGetString(value: String?) {
        every {
            Settings.Secure.getString(any(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        } returns value
    }
}
