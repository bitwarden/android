package com.x8bit.bitwarden.data.autofill.accessibility.manager

import android.view.accessibility.AccessibilityManager
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AccessibilityEnabledManagerTest {

    private val accessibilityStateChangeListener =
        slot<AccessibilityManager.AccessibilityStateChangeListener>()
    private val accessibilityManager = mockk<AccessibilityManager> {
        every {
            addAccessibilityStateChangeListener(capture(accessibilityStateChangeListener))
        } returns true
    }

    private val accessibilityEnabledManager: AccessibilityEnabledManager =
        AccessibilityEnabledManagerImpl(
            accessibilityManager = accessibilityManager,
        )

    @Suppress("MaxLineLength")
    @Test
    fun `isAccessibilityEnabledStateFlow should emit whenever accessibilityStateChangeListener emits a unique value`() =
        runTest {
            accessibilityEnabledManager.isAccessibilityEnabledStateFlow.test {
                assertFalse(awaitItem())

                accessibilityStateChangeListener.captured.onAccessibilityStateChanged(true)
                assertTrue(awaitItem())

                accessibilityStateChangeListener.captured.onAccessibilityStateChanged(true)
                expectNoEvents()

                accessibilityStateChangeListener.captured.onAccessibilityStateChanged(false)
                assertFalse(awaitItem())
            }
        }
}
