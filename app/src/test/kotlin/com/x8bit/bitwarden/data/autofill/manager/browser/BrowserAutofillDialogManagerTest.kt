package com.x8bit.bitwarden.data.autofill.manager.browser

import com.x8bit.bitwarden.data.autofill.manager.AutofillEnabledManager
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutoFillData
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutofillStatus
import com.x8bit.bitwarden.data.platform.datasource.disk.util.FakeSettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class BrowserAutofillDialogManagerTest {

    private val autofillEnabledManager: AutofillEnabledManager = mockk()
    private val firstTimeActionManager: FirstTimeActionManager = mockk()
    private val thirdPartyAutofillEnabledManager: BrowserThirdPartyAutofillEnabledManager = mockk()
    private val fakeSettingsDiskSource: FakeSettingsDiskSource = FakeSettingsDiskSource()

    private val manager: BrowserAutofillDialogManager = BrowserAutofillDialogManagerImpl(
        autofillEnabledManager = autofillEnabledManager,
        browserThirdPartyAutofillEnabledManager = thirdPartyAutofillEnabledManager,
        clock = FIXED_CLOCK,
        firstTimeActionManager = firstTimeActionManager,
        settingsDiskSource = fakeSettingsDiskSource,
    )

    @Test
    fun `browserCount should return the browser count`() {
        val count = 0
        every {
            thirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus
        } returns mockk { every { availableCount } returns count }

        assertEquals(count, manager.browserCount)

        verify(exactly = 1) {
            thirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus
        }
    }

    @Test
    fun `shouldShowDialog should be false when autofill is disabled`() {
        every { autofillEnabledManager.isAutofillEnabled } returns false

        assertFalse(manager.shouldShowDialog)

        verify(exactly = 1) {
            autofillEnabledManager.isAutofillEnabled
        }
        verify(exactly = 0) {
            thirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus
        }
    }

    @Test
    fun `shouldShowDialog should be false when no browsers are available`() {
        val browserThirdPartyAutofillStatus = BrowserThirdPartyAutofillStatus(
            braveStableStatusData = BrowserThirdPartyAutoFillData(
                isAvailable = false,
                isThirdPartyEnabled = false,
            ),
            chromeStableStatusData = BrowserThirdPartyAutoFillData(
                isAvailable = false,
                isThirdPartyEnabled = false,
            ),
            chromeBetaChannelStatusData = BrowserThirdPartyAutoFillData(
                isAvailable = false,
                isThirdPartyEnabled = false,
            ),
        )
        every { autofillEnabledManager.isAutofillEnabled } returns true
        every {
            thirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus
        } returns browserThirdPartyAutofillStatus

        assertFalse(manager.shouldShowDialog)

        verify(exactly = 1) {
            autofillEnabledManager.isAutofillEnabled
            thirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus
        }
    }

    @Test
    fun `shouldShowDialog should be false when all browsers are configured`() {
        val browserThirdPartyAutofillStatus = BrowserThirdPartyAutofillStatus(
            braveStableStatusData = BrowserThirdPartyAutoFillData(
                isAvailable = true,
                isThirdPartyEnabled = true,
            ),
            chromeStableStatusData = BrowserThirdPartyAutoFillData(
                isAvailable = true,
                isThirdPartyEnabled = true,
            ),
            chromeBetaChannelStatusData = BrowserThirdPartyAutoFillData(
                isAvailable = true,
                isThirdPartyEnabled = true,
            ),
        )
        every { autofillEnabledManager.isAutofillEnabled } returns true
        every {
            thirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus
        } returns browserThirdPartyAutofillStatus
        every {
            firstTimeActionManager.currentOrDefaultUserFirstTimeState
        } returns FirstTimeState(showSetupBrowserAutofillCard = false)

        assertFalse(manager.shouldShowDialog)

        verify(exactly = 1) {
            autofillEnabledManager.isAutofillEnabled
            thirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus
        }
    }

    @Test
    fun `shouldShowDialog should be false browser autofill badge is being displayed`() {
        val browserThirdPartyAutofillStatus = BrowserThirdPartyAutofillStatus(
            braveStableStatusData = BrowserThirdPartyAutoFillData(
                isAvailable = true,
                isThirdPartyEnabled = false,
            ),
            chromeStableStatusData = BrowserThirdPartyAutoFillData(
                isAvailable = true,
                isThirdPartyEnabled = false,
            ),
            chromeBetaChannelStatusData = BrowserThirdPartyAutoFillData(
                isAvailable = true,
                isThirdPartyEnabled = false,
            ),
        )
        every { autofillEnabledManager.isAutofillEnabled } returns true
        every {
            thirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus
        } returns browserThirdPartyAutofillStatus
        every {
            firstTimeActionManager.currentOrDefaultUserFirstTimeState
        } returns FirstTimeState(showSetupBrowserAutofillCard = true)

        assertFalse(manager.shouldShowDialog)

        verify(exactly = 1) {
            autofillEnabledManager.isAutofillEnabled
            thirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus
        }
    }

    @Test
    fun `shouldShowDialog should be false when 24 hours have not passed since previous dialog`() {
        val browserThirdPartyAutofillStatus = BrowserThirdPartyAutofillStatus(
            braveStableStatusData = BrowserThirdPartyAutoFillData(
                isAvailable = true,
                isThirdPartyEnabled = false,
            ),
            chromeStableStatusData = BrowserThirdPartyAutoFillData(
                isAvailable = true,
                isThirdPartyEnabled = false,
            ),
            chromeBetaChannelStatusData = BrowserThirdPartyAutoFillData(
                isAvailable = true,
                isThirdPartyEnabled = false,
            ),
        )
        every { autofillEnabledManager.isAutofillEnabled } returns true
        every {
            thirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus
        } returns browserThirdPartyAutofillStatus
        every {
            firstTimeActionManager.currentOrDefaultUserFirstTimeState
        } returns FirstTimeState(showSetupBrowserAutofillCard = false)
        fakeSettingsDiskSource.browserAutofillDialogReshowTime = FIXED_CLOCK.instant()

        assertFalse(manager.shouldShowDialog)

        verify(exactly = 1) {
            autofillEnabledManager.isAutofillEnabled
            thirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus
        }
    }

    @Test
    fun `shouldShowDialog should be true when all conditions are met`() {
        val browserThirdPartyAutofillStatus = BrowserThirdPartyAutofillStatus(
            braveStableStatusData = BrowserThirdPartyAutoFillData(
                isAvailable = true,
                isThirdPartyEnabled = false,
            ),
            chromeStableStatusData = BrowserThirdPartyAutoFillData(
                isAvailable = true,
                isThirdPartyEnabled = false,
            ),
            chromeBetaChannelStatusData = BrowserThirdPartyAutoFillData(
                isAvailable = true,
                isThirdPartyEnabled = false,
            ),
        )
        every { autofillEnabledManager.isAutofillEnabled } returns true
        every {
            thirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus
        } returns browserThirdPartyAutofillStatus
        every {
            firstTimeActionManager.currentOrDefaultUserFirstTimeState
        } returns FirstTimeState(showSetupBrowserAutofillCard = false)
        fakeSettingsDiskSource.browserAutofillDialogReshowTime = null

        assertTrue(manager.shouldShowDialog)

        verify(exactly = 1) {
            autofillEnabledManager.isAutofillEnabled
            thirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus
        }
    }

    @Test
    fun `delayDialog should set the correct time on the disk source`() {
        fakeSettingsDiskSource.assertBrowserAutofillDialogReshowTime(null)
        manager.delayDialog()
        fakeSettingsDiskSource.assertBrowserAutofillDialogReshowTime(
            FIXED_CLOCK.instant().plus(1L, ChronoUnit.DAYS),
        )
    }
}

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2023-10-27T12:00:00Z"),
    ZoneOffset.UTC,
)
