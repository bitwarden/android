package com.x8bit.bitwarden.data.autofill.accessibility.manager

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import app.cash.turbine.test
import com.x8bit.bitwarden.data.autofill.accessibility.util.isAccessibilityServiceEnabled
import com.x8bit.bitwarden.data.platform.manager.AppStateManager
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
    private val context: Context = mockk()
    private val accessibilityEnabledManager: AccessibilityEnabledManager =
        AccessibilityEnabledManagerImpl()
    private val mutableAppForegroundStateFlow = MutableStateFlow(AppForegroundState.BACKGROUNDED)
    private val appStateManager: AppStateManager = mockk {
        every { appForegroundStateFlow } returns mutableAppForegroundStateFlow
    }
    private val lifecycleScope = mockk<LifecycleCoroutineScope> {
        every { coroutineContext } returns UnconfinedTestDispatcher()
    }

    // We will construct an instance here just to hook the various dependencies together internally
    private lateinit var autofillActivityManager: AccessibilityActivityManager

    @BeforeEach
    fun setup() {
        mockkStatic(Context::isAccessibilityServiceEnabled)
        every { context.isAccessibilityServiceEnabled } returns false
        autofillActivityManager = AccessibilityActivityManagerImpl(
            context = context,
            accessibilityEnabledManager = accessibilityEnabledManager,
            appStateManager = appStateManager,
            lifecycleScope = lifecycleScope,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Context::isAccessibilityServiceEnabled)
    }

    @Test
    fun `changes in app foreground status should update the AutofillEnabledManager as necessary`() =
        runTest {
            accessibilityEnabledManager.isAccessibilityEnabledStateFlow.test {
                assertFalse(awaitItem())

                // An update is received when both the accessibility state and foreground state
                // change
                every { context.isAccessibilityServiceEnabled } returns true
                mutableAppForegroundStateFlow.value = AppForegroundState.FOREGROUNDED
                assertTrue(awaitItem())

                // An update is not received when only the foreground state changes
                mutableAppForegroundStateFlow.value = AppForegroundState.BACKGROUNDED
                expectNoEvents()

                // An update is not received when only the accessibility state changes
                every { context.isAccessibilityServiceEnabled } returns false
                expectNoEvents()

                // An update is received after both states have changed
                mutableAppForegroundStateFlow.value = AppForegroundState.FOREGROUNDED
                assertFalse(awaitItem())
            }
        }
}
