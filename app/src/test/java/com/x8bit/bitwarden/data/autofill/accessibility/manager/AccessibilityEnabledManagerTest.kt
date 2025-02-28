package com.x8bit.bitwarden.data.autofill.accessibility.manager

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AccessibilityEnabledManagerTest {
    private val accessibilityEnabledManager = FakeAccessibilityEnabledManager()

    @Test
    fun `isAccessibilityEnabled returns false when setting is null`() = runTest {
        accessibilityEnabledManager.isAccessibilityEnabled = false
        accessibilityEnabledManager.refreshAccessibilityEnabledFromSettings()
        val result = accessibilityEnabledManager.isAccessibilityEnabledStateFlow.value
        assertFalse(result)
    }

    @Test
    fun `isAccessibilityEnabled returns false when setting does not contain our service`() =
        runTest {
            accessibilityEnabledManager.isAccessibilityEnabled = false
            accessibilityEnabledManager.refreshAccessibilityEnabledFromSettings()
            val result = accessibilityEnabledManager.isAccessibilityEnabledStateFlow.value
            assertFalse(result)
        }

    @Test
    fun `isAccessibilityEnabled returns true when setting contains the defined service`() =
        runTest {
            accessibilityEnabledManager.isAccessibilityEnabled = true
            accessibilityEnabledManager.refreshAccessibilityEnabledFromSettings()
            val result = accessibilityEnabledManager.isAccessibilityEnabledStateFlow.value
            assertTrue(result)
        }
}
