package com.x8bit.bitwarden.data.autofill.accessibility.manager

import android.content.Context
import android.provider.Settings
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AccessibilityEnabledManagerTest {
    private val testPackageName = "com.x8bit.bitwarden.test"
    private val testAccessibilityService =
        "$testPackageName/com.x8bit.bitwarden.Accessibility.AccessibilityService"

    private val context: Context = mockk {
        every { applicationContext } returns this
        every { contentResolver } returns mockk()
        every { packageName } returns testPackageName
    }

    private val accessibilityEnabledManager = AccessibilityEnabledManagerImpl(context)

    @BeforeEach
    fun setup() {
        mockkStatic(Settings.Secure::getString)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Settings.Secure::getString)
    }

    @Test
    fun `isAccessibilityEnabled returns false when setting is null`() = runTest {
        mockkSettingsSecureGetString(null)

        accessibilityEnabledManager.refreshAccessibilityEnabledFromSettings()
        val result = accessibilityEnabledManager.isAccessibilityEnabledStateFlow.value
        Assertions.assertFalse(result)
    }

    @Test
    fun `isAccessibilityEnabled returns false when setting does not contain our service`() =
        runTest {
            mockkSettingsSecureGetString(
                value = "some.other.service/SomeOtherService:" +
                    "another.service/AnotherService",
            )

            accessibilityEnabledManager.refreshAccessibilityEnabledFromSettings()
            val result = accessibilityEnabledManager.isAccessibilityEnabledStateFlow.value

            Assertions.assertFalse(result)
        }

    @Test
    fun `isAccessibilityEnabled returns true when setting contains the defined service`() =
        runTest {
            mockkSettingsSecureGetString(
                value = "some.other.service/SomeOtherService:" +
                    "$testAccessibilityService:" +
                    "another.service/AnotherService",
            )

            accessibilityEnabledManager.refreshAccessibilityEnabledFromSettings()
            val result = accessibilityEnabledManager.isAccessibilityEnabledStateFlow.value

            Assertions.assertTrue(result)
        }

    @Test
    fun `updateAccessibilityEnabledStateFlow updates the state flow`() = runTest {
        accessibilityEnabledManager.updateAccessibilityEnabledStateFlow(true)

        accessibilityEnabledManager.isAccessibilityEnabledStateFlow.value
        val result = accessibilityEnabledManager.isAccessibilityEnabledStateFlow.value

        Assertions.assertTrue(result)
    }

    private fun mockkSettingsSecureGetString(value: String?) {
        every {
            Settings.Secure.getString(any(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        } returns value
    }
}
