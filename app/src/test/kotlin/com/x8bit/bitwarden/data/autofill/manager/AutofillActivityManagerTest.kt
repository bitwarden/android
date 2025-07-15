package com.x8bit.bitwarden.data.autofill.manager

import android.view.autofill.AutofillManager
import androidx.lifecycle.LifecycleCoroutineScope
import app.cash.turbine.test
import com.x8bit.bitwarden.data.autofill.manager.browser.BrowserThirdPartyAutofillEnabledManager
import com.x8bit.bitwarden.data.autofill.manager.browser.BrowserThirdPartyAutofillEnabledManagerImpl
import com.x8bit.bitwarden.data.autofill.manager.browser.BrowserThirdPartyAutofillManager
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutoFillData
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutofillStatus
import com.x8bit.bitwarden.data.platform.manager.AppStateManager
import com.x8bit.bitwarden.data.platform.manager.model.AppForegroundState
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutofillActivityManagerTest {
    private val autofillManager: AutofillManager = mockk {
        every { hasEnabledAutofillServices() } answers { isAutofillEnabledAndSupported }
        every { isAutofillSupported } answers { isAutofillEnabledAndSupported }
        every { isEnabled } answers { isAutofillEnabledAndSupported }
        every { disableAutofillServices() } just runs
    }
    private val autofillEnabledManager: AutofillEnabledManager = AutofillEnabledManagerImpl()

    private val mutableAppForegroundStateFlow = MutableStateFlow(AppForegroundState.BACKGROUNDED)
    private val appStateManager: AppStateManager = mockk {
        every { appForegroundStateFlow } returns mutableAppForegroundStateFlow
    }
    private val lifecycleScope = mockk<LifecycleCoroutineScope> {
        every { coroutineContext } returns UnconfinedTestDispatcher()
    }
    private val browserThirdPartyAutofillManager = mockk<BrowserThirdPartyAutofillManager> {
        every { stableBraveAutofillStatus } returns DEFAULT_BROWSER_AUTOFILL_DATA
        every { stableChromeAutofillStatus } returns DEFAULT_BROWSER_AUTOFILL_DATA
        every { betaChromeAutofillStatus } returns DEFAULT_BROWSER_AUTOFILL_DATA
    }

    private val browserThirdPartyAutofillEnabledManager: BrowserThirdPartyAutofillEnabledManager =
        BrowserThirdPartyAutofillEnabledManagerImpl()

    // We will construct an instance here just to hook the various dependencies together internally
    @Suppress("unused")
    private val autofillActivityManager: AutofillActivityManager = AutofillActivityManagerImpl(
        autofillManager = autofillManager,
        appStateManager = appStateManager,
        autofillEnabledManager = autofillEnabledManager,
        lifecycleScope = lifecycleScope,
        browserThirdPartyAutofillManager = browserThirdPartyAutofillManager,
        browserThirdPartyAutofillEnabledManager = browserThirdPartyAutofillEnabledManager,
    )

    private var isAutofillEnabledAndSupported = false

    @Test
    fun `changes in app foreground status should update the AutofillEnabledManager as necessary`() =
        runTest {
            autofillEnabledManager.isAutofillEnabledStateFlow.test {
                assertFalse(awaitItem())

                // An update is received when both the autofill state and foreground state change
                isAutofillEnabledAndSupported = true
                mutableAppForegroundStateFlow.value = AppForegroundState.FOREGROUNDED
                assertTrue(awaitItem())

                // An update is not received when only the foreground state changes
                mutableAppForegroundStateFlow.value = AppForegroundState.BACKGROUNDED
                expectNoEvents()

                // An update is not received when only the autofill state changes
                isAutofillEnabledAndSupported = false
                expectNoEvents()

                // An update is received after both states have changed
                mutableAppForegroundStateFlow.value = AppForegroundState.FOREGROUNDED
                assertFalse(awaitItem())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `changes in app foreground status should update the BrowserThirdPartyAutofillEnabledManager as necessary`() =
        runTest {
            val updatedBetaState = DEFAULT_BROWSER_AUTOFILL_DATA.copy(isAvailable = true)
            browserThirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatusFlow.test {
                assertEquals(
                    DEFAULT_EXPECTED_AUTOFILL_STATUS,
                    awaitItem(),
                )
                every { browserThirdPartyAutofillManager.betaChromeAutofillStatus } returns
                    updatedBetaState
                mutableAppForegroundStateFlow.value = AppForegroundState.FOREGROUNDED
                assertEquals(
                    DEFAULT_EXPECTED_AUTOFILL_STATUS.copy(
                        chromeBetaChannelStatusData = updatedBetaState,
                    ),
                    awaitItem(),
                )
            }
        }
}

private val DEFAULT_BROWSER_AUTOFILL_DATA = BrowserThirdPartyAutoFillData(
    isAvailable = false,
    isThirdPartyEnabled = false,
)

private val DEFAULT_EXPECTED_AUTOFILL_STATUS = BrowserThirdPartyAutofillStatus(
    braveStableStatusData = DEFAULT_BROWSER_AUTOFILL_DATA,
    chromeStableStatusData = DEFAULT_BROWSER_AUTOFILL_DATA,
    chromeBetaChannelStatusData = DEFAULT_BROWSER_AUTOFILL_DATA,
)
