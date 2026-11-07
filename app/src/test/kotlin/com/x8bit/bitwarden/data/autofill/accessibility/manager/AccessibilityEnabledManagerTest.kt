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

    private lateinit var accessibilityEnabledManager: AccessibilityEnabledManager

    @BeforeEach
    fun setUp() {
        mockkStatic(Context::isAccessibilityServiceEnabled)
        every { context.isAccessibilityServiceEnabled } returns false
        accessibilityEnabledManager = AccessibilityEnabledManagerImpl(context)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `isAccessibilityEnabled is seeded from the platform when it does not report the service`() =
        runTest {
            val result = accessibilityEnabledManager.isAccessibilityEnabledStateFlow.value

            assertFalse(result)
        }

    @Test
    fun `isAccessibilityEnabled is seeded from the platform when it reports the service`() =
        runTest {
            every { context.isAccessibilityServiceEnabled } returns true

            val result = AccessibilityEnabledManagerImpl(context)
                .isAccessibilityEnabledStateFlow
                .value

            assertTrue(result)
        }

    @Test
    fun `isAccessibilityEnabled is true when the service reports it has connected`() = runTest {
        accessibilityEnabledManager.setAccessibilityServiceConnected(isConnected = true)

        assertTrue(accessibilityEnabledManager.isAccessibilityEnabledStateFlow.value)
    }

    @Test
    fun `isAccessibilityEnabled is false when the service reports it has disconnected`() =
        runTest {
            accessibilityEnabledManager.setAccessibilityServiceConnected(isConnected = true)
            accessibilityEnabledManager.setAccessibilityServiceConnected(isConnected = false)

            assertFalse(accessibilityEnabledManager.isAccessibilityEnabledStateFlow.value)
        }

    @Test
    fun `isAccessibilityEnabledStateFlow emits when the service connection state changes`() =
        runTest {
            accessibilityEnabledManager.isAccessibilityEnabledStateFlow.test {
                assertFalse(awaitItem())

                accessibilityEnabledManager.setAccessibilityServiceConnected(isConnected = true)
                assertTrue(awaitItem())

                accessibilityEnabledManager.setAccessibilityServiceConnected(isConnected = false)
                assertFalse(awaitItem())
            }
        }

    @Test
    fun `service connection updates override the platform reported enabled state`() = runTest {
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
