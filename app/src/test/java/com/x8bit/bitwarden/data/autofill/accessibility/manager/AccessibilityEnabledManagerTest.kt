package com.x8bit.bitwarden.data.autofill.accessibility.manager

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AccessibilityEnabledManagerTest {

    private val accessibilityEnabledManager: AccessibilityEnabledManager =
        AccessibilityEnabledManagerImpl()

    @Suppress("MaxLineLength")
    @Test
    fun `isAccessibilityEnabledStateFlow should emit whenever isAccessibilityEnabled is set to a unique value`() =
        runTest {
            accessibilityEnabledManager.isAccessibilityEnabledStateFlow.test {
                assertFalse(awaitItem())

                accessibilityEnabledManager.isAccessibilityEnabled = true
                assertTrue(awaitItem())

                accessibilityEnabledManager.isAccessibilityEnabled = true
                expectNoEvents()

                accessibilityEnabledManager.isAccessibilityEnabled = false
                assertFalse(awaitItem())
            }
        }
}
