package com.x8bit.bitwarden.data.autofill.manager

import app.cash.turbine.test
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.data.manager.appstate.FakeAppStateManager
import com.bitwarden.data.manager.appstate.model.AppForegroundState
import com.x8bit.bitwarden.data.autofill.manager.browser.BrowserThirdPartyAutofillEnabledManager
import com.x8bit.bitwarden.data.autofill.manager.browser.BrowserThirdPartyAutofillEnabledManagerImpl
import com.x8bit.bitwarden.data.autofill.model.AutofillPromptStatus
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserPackage
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutoFillData
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutofillStatus
import com.x8bit.bitwarden.data.platform.datasource.disk.util.FakeSettingsDiskSource
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class AutofillPromptManagerTest {
    private val autofillEnabledManager: AutofillEnabledManager = AutofillEnabledManagerImpl()
    private val browserThirdPartyAutofillEnabledManager: BrowserThirdPartyAutofillEnabledManager =
        BrowserThirdPartyAutofillEnabledManagerImpl()

    private val fakeAppStateManager = FakeAppStateManager()
    private val fakeSettingsDiskSource = FakeSettingsDiskSource()

    private val autofillPromptManager: AutofillPromptManager = createManager(clock = FIXED_CLOCK)

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

    @Test
    fun `vaultAutofillPromptFlow should be null while the status is Unknown`() {
        assertNull(autofillPromptManager.vaultAutofillPromptFlow.value)
    }

    @Test
    fun `vaultAutofillPromptFlow should be null when autofill is On`() {
        setDeviceStatus(isAutofillEnabled = true, browserStatus = createStatus())

        assertNull(autofillPromptManager.vaultAutofillPromptFlow.value)
    }

    @Test
    fun `vaultAutofillPromptFlow should emit AutofillOff when autofill is off`() {
        setDeviceStatus(isAutofillEnabled = false, browserStatus = createStatus())

        assertEquals(
            AutofillPromptStatus.AutofillOff,
            autofillPromptManager.vaultAutofillPromptFlow.value,
        )
    }

    @Test
    fun `vaultAutofillPromptFlow should emit BrowserOff when the default browser is off`() {
        setDeviceStatus(
            isAutofillEnabled = true,
            browserStatus = createStatus(
                defaultBrowserPackageName = BrowserPackage.CHROME_STABLE.packageName,
                chromeStable = AVAILABLE_DISABLED,
            ),
        )

        assertEquals(
            AutofillPromptStatus.BrowserOff(browserPackage = BrowserPackage.CHROME_STABLE),
            autofillPromptManager.vaultAutofillPromptFlow.value,
        )
    }

    @Test
    fun `snoozeVaultPrompt should store a reshow time 24 hours from now`() {
        autofillPromptManager.snoozeVaultPrompt()

        assertEquals(
            FIXED_CLOCK.instant().plusSeconds(24 * 60 * 60),
            fakeSettingsDiskSource.autofillBannerReshowTime,
        )
    }

    @Test
    fun `snoozeVaultPrompt should hide the vault prompt but not the unfiltered status`() {
        setDeviceStatus(isAutofillEnabled = false, browserStatus = createStatus())

        autofillPromptManager.snoozeVaultPrompt()

        assertNull(autofillPromptManager.vaultAutofillPromptFlow.value)
        assertEquals(
            AutofillPromptStatus.AutofillOff,
            autofillPromptManager.autofillPromptStatusFlow.value,
        )
    }

    @Test
    fun `vaultAutofillPromptFlow should show once the reshow time has been reached`() {
        fakeSettingsDiskSource.autofillBannerReshowTime = FIXED_CLOCK.instant()
        setDeviceStatus(isAutofillEnabled = false, browserStatus = createStatus())

        assertEquals(
            AutofillPromptStatus.AutofillOff,
            autofillPromptManager.vaultAutofillPromptFlow.value,
        )
    }

    @Test
    fun `vaultAutofillPromptFlow should stay hidden while the snooze has not expired`() {
        fakeSettingsDiskSource.autofillBannerReshowTime =
            FIXED_CLOCK.instant().plusSeconds(1)
        setDeviceStatus(isAutofillEnabled = false, browserStatus = createStatus())

        fakeAppStateManager.appForegroundState = AppForegroundState.FOREGROUNDED

        assertNull(autofillPromptManager.vaultAutofillPromptFlow.value)
    }

    @Test
    fun `vaultAutofillPromptFlow should re-check the snooze when the app is foregrounded`() {
        val mutableClock = MutableClock(instant = FIXED_CLOCK.instant())
        val manager = createManager(clock = mutableClock)
        setDeviceStatus(isAutofillEnabled = false, browserStatus = createStatus())
        manager.snoozeVaultPrompt()
        assertNull(manager.vaultAutofillPromptFlow.value)

        // 24 hours pass while nothing else changes, then the user returns to the app.
        mutableClock.instant = FIXED_CLOCK.instant().plusSeconds(24 * 60 * 60)
        fakeAppStateManager.appForegroundState = AppForegroundState.FOREGROUNDED

        assertEquals(AutofillPromptStatus.AutofillOff, manager.vaultAutofillPromptFlow.value)
    }

    @Test
    fun `optOutOfVaultPrompt should store the opt-out and hide the vault prompt`() {
        setDeviceStatus(isAutofillEnabled = false, browserStatus = createStatus())

        autofillPromptManager.optOutOfVaultPrompt()

        assertEquals(true, fakeSettingsDiskSource.isAutofillBannerOptedOut)
        assertNull(autofillPromptManager.vaultAutofillPromptFlow.value)
        assertEquals(
            AutofillPromptStatus.AutofillOff,
            autofillPromptManager.autofillPromptStatusFlow.value,
        )
    }

    private fun createManager(clock: Clock): AutofillPromptManager = AutofillPromptManagerImpl(
        autofillEnabledManager = autofillEnabledManager,
        browserThirdPartyAutofillEnabledManager = browserThirdPartyAutofillEnabledManager,
        appStateManager = fakeAppStateManager,
        settingsDiskSource = fakeSettingsDiskSource,
        clock = clock,
        dispatcherManager = FakeDispatcherManager(),
    )

    private fun setDeviceStatus(
        isAutofillEnabled: Boolean,
        browserStatus: BrowserThirdPartyAutofillStatus,
    ) {
        autofillEnabledManager.isAutofillEnabled = isAutofillEnabled
        browserThirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus = browserStatus
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

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2026-11-12T12:00:00Z"),
    ZoneOffset.UTC,
)

/**
 * A [Clock] whose current time can be changed during a test.
 */
private class MutableClock(var instant: Instant) : Clock() {
    override fun getZone(): ZoneOffset = ZoneOffset.UTC
    override fun withZone(zone: ZoneId?): Clock = this
    override fun instant(): Instant = instant
}
