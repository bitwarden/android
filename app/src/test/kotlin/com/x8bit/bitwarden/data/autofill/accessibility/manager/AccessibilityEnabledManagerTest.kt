package com.x8bit.bitwarden.data.autofill.accessibility.manager

import android.content.Context
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
    fun `isAccessibilityEnabled returns false when setting does not contain our service`() =
        runTest {
            every { context.isAccessibilityServiceEnabled } returns false
            accessibilityEnabledManager.refreshAccessibilityEnabledFromSettings()
            val result = accessibilityEnabledManager.isAccessibilityEnabledStateFlow.value
            assertFalse(result)
        }

    @Test
    fun `isAccessibilityEnabled returns true when setting contains the defined service`() =
        runTest {
            every { context.isAccessibilityServiceEnabled } returns true
            accessibilityEnabledManager.refreshAccessibilityEnabledFromSettings()
            val result = accessibilityEnabledManager.isAccessibilityEnabledStateFlow.value
            assertTrue(result)
        }
}
