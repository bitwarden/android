package com.x8bit.bitwarden.data.autofill.accessibility.manager

import android.content.Context
import app.cash.turbine.test
import com.x8bit.bitwarden.data.autofill.accessibility.util.isAccessibilityServiceEnabled
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AccessibilityEnabledManagerTest {
    private val context: Context = mockk()

    @BeforeEach
    fun setUp() {
        mockkStatic(Context::isAccessibilityServiceEnabled)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `isAccessibilityEnabledStateFlow starts false and emits connection updates`() = runTest {
        every { context.isAccessibilityServiceEnabled } returns false
        val manager = AccessibilityEnabledManagerImpl(context)

        manager.isAccessibilityEnabledStateFlow.test {
            assertFalse(awaitItem())

            manager.setAccessibilityServiceConnected(isConnected = true)
            assertTrue(awaitItem())

            manager.setAccessibilityServiceConnected(isConnected = false)
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `isAccessibilityEnabledStateFlow starts true and emits connection updates`() = runTest {
        every { context.isAccessibilityServiceEnabled } returns true
        val manager = AccessibilityEnabledManagerImpl(context)

        manager.isAccessibilityEnabledStateFlow.test {
            assertTrue(awaitItem())

            manager.setAccessibilityServiceConnected(isConnected = false)
            assertFalse(awaitItem())

            manager.setAccessibilityServiceConnected(isConnected = true)
            assertTrue(awaitItem())
        }
    }
}
