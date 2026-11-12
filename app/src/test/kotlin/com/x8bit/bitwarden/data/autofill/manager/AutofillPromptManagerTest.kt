package com.x8bit.bitwarden.data.autofill.manager

import app.cash.turbine.test
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.x8bit.bitwarden.data.autofill.manager.browser.BrowserThirdPartyAutofillEnabledManager
import com.x8bit.bitwarden.data.autofill.manager.browser.BrowserThirdPartyAutofillEnabledManagerImpl
import com.x8bit.bitwarden.data.autofill.model.AutofillPromptStatus
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserPackage
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutoFillData
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutofillStatus
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AutofillPromptManagerTest {
    private val autofillEnabledManager: AutofillEnabledManager = AutofillEnabledManagerImpl()
    private val browserThirdPartyAutofillEnabledManager: BrowserThirdPartyAutofillEnabledManager =
        BrowserThirdPartyAutofillEnabledManagerImpl()

    private val autofillPromptManager: AutofillPromptManager = AutofillPromptManagerImpl(
        autofillEnabledManager = autofillEnabledManager,
        browserThirdPartyAutofillEnabledManager = browserThirdPartyAutofillEnabledManager,
        dispatcherManager = FakeDispatcherManager(),
    )

    @Test
    fun `autofillPromptStatusFlow should be Unknown before anything is checked`() {
        assertEquals(
            AutofillPromptStatus.Unknown,
            autofillPromptManager.autofillPromptStatusFlow.value,
        )
    }

    @Test
    fun `autofillPromptStatusFlow should be Unknown when only autofill has been checked`() {
        autofillEnabledManager.isAutofillEnabled = false

        assertEquals(
            AutofillPromptStatus.Unknown,
            autofillPromptManager.autofillPromptStatusFlow.value,
        )
    }

    @Test
    fun `autofillPromptStatusFlow should be Unknown when only the browser has been checked`() {
        browserThirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus = createStatus(
            defaultBrowserPackageName = BrowserPackage.CHROME_STABLE.packageName,
            chromeStable = AVAILABLE_DISABLED,
        )

        assertEquals(
            AutofillPromptStatus.Unknown,
            autofillPromptManager.autofillPromptStatusFlow.value,
        )
    }

    @Test
    fun `autofillPromptStatusFlow should be AutofillOff when autofill is disabled`() {
        autofillEnabledManager.isAutofillEnabled = false
        browserThirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus = createStatus()

        assertEquals(
            AutofillPromptStatus.AutofillOff,
            autofillPromptManager.autofillPromptStatusFlow.value,
        )
    }

    @Test
    fun `autofillPromptStatusFlow should be AutofillOff over BrowserOff when both are off`() {
        autofillEnabledManager.isAutofillEnabled = false
        browserThirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus = createStatus(
            defaultBrowserPackageName = BrowserPackage.CHROME_STABLE.packageName,
            chromeStable = AVAILABLE_DISABLED,
        )

        assertEquals(
            AutofillPromptStatus.AutofillOff,
            autofillPromptManager.autofillPromptStatusFlow.value,
        )
    }

    @Test
    fun `autofillPromptStatusFlow should be BrowserOff when the default browser is off`() {
        autofillEnabledManager.isAutofillEnabled = true
        browserThirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus = createStatus(
            defaultBrowserPackageName = BrowserPackage.BRAVE_RELEASE.packageName,
            brave = AVAILABLE_DISABLED,
        )

        assertEquals(
            AutofillPromptStatus.BrowserOff(browserPackage = BrowserPackage.BRAVE_RELEASE),
            autofillPromptManager.autofillPromptStatusFlow.value,
        )
    }

    @Test
    fun `autofillPromptStatusFlow should be On when only a non-default browser is off`() {
        autofillEnabledManager.isAutofillEnabled = true
        browserThirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus = createStatus(
            defaultBrowserPackageName = BrowserPackage.BRAVE_RELEASE.packageName,
            brave = AVAILABLE_ENABLED,
            chromeStable = AVAILABLE_DISABLED,
        )

        assertEquals(
            AutofillPromptStatus.On,
            autofillPromptManager.autofillPromptStatusFlow.value,
        )
    }

    @Test
    fun `autofillPromptStatusFlow should be On when the default browser does not report`() {
        autofillEnabledManager.isAutofillEnabled = true
        browserThirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus = createStatus(
            defaultBrowserPackageName = "org.mozilla.firefox",
            chromeStable = AVAILABLE_DISABLED,
        )

        assertEquals(
            AutofillPromptStatus.On,
            autofillPromptManager.autofillPromptStatusFlow.value,
        )
    }

    @Test
    fun `autofillPromptStatusFlow should update as autofill and browser status change`() =
        runTest {
            autofillPromptManager.autofillPromptStatusFlow.test {
                assertEquals(AutofillPromptStatus.Unknown, awaitItem())

                // Browser checked first: still Unknown until autofill is checked too.
                browserThirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus =
                    createStatus(
                        defaultBrowserPackageName = BrowserPackage.CHROME_STABLE.packageName,
                        chromeStable = AVAILABLE_DISABLED,
                    )
                expectNoEvents()

                autofillEnabledManager.isAutofillEnabled = false
                assertEquals(AutofillPromptStatus.AutofillOff, awaitItem())

                autofillEnabledManager.isAutofillEnabled = true
                assertEquals(
                    AutofillPromptStatus.BrowserOff(
                        browserPackage = BrowserPackage.CHROME_STABLE,
                    ),
                    awaitItem(),
                )

                browserThirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus =
                    createStatus(
                        defaultBrowserPackageName = BrowserPackage.CHROME_STABLE.packageName,
                        chromeStable = AVAILABLE_ENABLED,
                    )
                assertEquals(AutofillPromptStatus.On, awaitItem())
            }
        }

    private fun createStatus(
        defaultBrowserPackageName: String? = null,
        brave: BrowserThirdPartyAutoFillData = UNAVAILABLE,
        chromeStable: BrowserThirdPartyAutoFillData = UNAVAILABLE,
    ): BrowserThirdPartyAutofillStatus = BrowserThirdPartyAutofillStatus(
        braveStableStatusData = brave,
        chromeStableStatusData = chromeStable,
        chromeBetaChannelStatusData = UNAVAILABLE,
        vivaldiStableChannelStatusData = UNAVAILABLE,
        defaultBrowserPackageName = defaultBrowserPackageName,
    )
}

private val UNAVAILABLE = BrowserThirdPartyAutoFillData(
    isAvailable = false,
    isThirdPartyEnabled = false,
)
private val AVAILABLE_DISABLED = BrowserThirdPartyAutoFillData(
    isAvailable = true,
    isThirdPartyEnabled = false,
)
private val AVAILABLE_ENABLED = BrowserThirdPartyAutoFillData(
    isAvailable = true,
    isThirdPartyEnabled = true,
)
