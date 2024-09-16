package com.x8bit.bitwarden.data.autofill.accessibility.manager

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.LifecycleCoroutineScope
import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.manager.AppForegroundManager
import com.x8bit.bitwarden.data.platform.manager.model.AppForegroundState
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccessibilityActivityManagerTest {
    private val context: Context = mockk {
        every { applicationContext } returns this
        every { packageName } returns "com.x8bit.bitwarden"
        every { contentResolver } returns mockk()
    }
    private val accessibilityEnabledManager: AccessibilityEnabledManager =
        AccessibilityEnabledManagerImpl()
    private val mutableAppForegroundStateFlow = MutableStateFlow(AppForegroundState.BACKGROUNDED)
    private val appForegroundManager: AppForegroundManager = mockk {
        every { appForegroundStateFlow } returns mutableAppForegroundStateFlow
    }
    private val lifecycleScope = mockk<LifecycleCoroutineScope> {
        every { coroutineContext } returns UnconfinedTestDispatcher()
    }

    // We will construct an instance here just to hook the various dependencies together internally
    @Suppress("unused")
    private val autofillActivityManager: AccessibilityActivityManager =
        AccessibilityActivityManagerImpl(
            context = context,
            accessibilityEnabledManager = accessibilityEnabledManager,
            appForegroundManager = appForegroundManager,
            lifecycleScope = lifecycleScope,
        )

    @BeforeEach
    fun setup() {
        mockkStatic(Settings.Secure::getString)
        mockkSettingsSecureGetString(value = null)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Settings.Secure::getString)
    }

    @Test
    fun `changes in app foreground status should update the AutofillEnabledManager as necessary`() =
        runTest {
            accessibilityEnabledManager.isAccessibilityEnabledStateFlow.test {
                assertFalse(awaitItem())

                // An update is received when both the accessibility state and foreground state
                // change
                @Suppress("MaxLineLength")
                mockkSettingsSecureGetString(
                    value = "com.x8bit.bitwarden/com.x8bit.bitwarden.Accessibility.AccessibilityService",
                )
                mutableAppForegroundStateFlow.value = AppForegroundState.FOREGROUNDED
                assertTrue(awaitItem())

                // An update is not received when only the foreground state changes
                mutableAppForegroundStateFlow.value = AppForegroundState.BACKGROUNDED
                expectNoEvents()

                // An update is not received when only the accessibility state changes
                mockkSettingsSecureGetString(value = "com.x8bit.bitwarden/AccessibilityService")
                expectNoEvents()

                // An update is received after both states have changed
                mutableAppForegroundStateFlow.value = AppForegroundState.FOREGROUNDED
                assertFalse(awaitItem())
            }
        }

    private fun mockkSettingsSecureGetString(value: String?) {
        every {
            Settings.Secure.getString(any(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        } returns value
    }
}
