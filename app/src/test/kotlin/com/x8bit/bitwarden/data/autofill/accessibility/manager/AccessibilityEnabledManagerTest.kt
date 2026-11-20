package com.x8bit.bitwarden.data.autofill.accessibility.manager

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AccessibilityEnabledManagerTest {
    private val accessibilityEnabledManager: AccessibilityEnabledManager =
        AccessibilityEnabledManagerImpl()

    @Test
    fun `isAccessibilityEnabledStateFlow defaults to false and emits connection updates`() =
        runTest {
            accessibilityEnabledManager.isAccessibilityEnabledStateFlow.test {
                assertFalse(awaitItem())

                accessibilityEnabledManager.setAccessibilityServiceConnected(isConnected = true)
                assertTrue(awaitItem())

                accessibilityEnabledManager.setAccessibilityServiceConnected(isConnected = false)
                assertFalse(awaitItem())
            }
        }
}
