package com.x8bit.bitwarden.data.platform.manager.log

import com.bitwarden.data.datasource.disk.model.FlightRecorderDataSet
import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityEnabledManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillEnabledManager
import com.x8bit.bitwarden.data.autofill.manager.browser.BrowserThirdPartyAutofillEnabledManager
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutoFillData
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutofillStatus
import com.x8bit.bitwarden.data.platform.manager.LogsManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.ClearClipboardFrequency
import com.x8bit.bitwarden.data.platform.repository.model.UriMatchType
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppLanguage
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SettingsLogManagerTest {
    private val settingsRepository = mockk<SettingsRepository> {
        every { isUnlockWithBiometricsEnabled } returns false
        every { isUnlockWithPinEnabled } returns false
        every { isPasswordOnRestartRequiredWithPin } returns false
        every { isAuthenticatorSyncEnabled } returns false
        every { vaultTimeout } returns VaultTimeout.FifteenMinutes
        every { vaultTimeoutAction } returns VaultTimeoutAction.LOCK
        every { isInlineAutofillEnabled } returns false
        every { isFillAssistEnabled } returns false
        every { isAutoCopyTotpDisabled } returns true
        every { isAutofillSavePromptDisabled } returns true
        every { defaultUriMatchType } returns UriMatchType.DOMAIN
        every { appLanguage } returns AppLanguage.DEFAULT
        every { appTheme } returns AppTheme.DEFAULT
        every { isDynamicColorsEnabled } returns false
        every { isIconLoadingDisabled } returns true
        every { getPullToRefreshEnabledFlow() } returns MutableStateFlow(false)
        every { clearClipboardFrequency } returns ClearClipboardFrequency.NEVER
        every { isScreenCaptureAllowed } returns false
        every { flightRecorderData } returns FlightRecorderDataSet(data = emptySet())
    }
    private val logsManager = mockk<LogsManager> {
        every { isEnabled } returns false
    }
    private val autofillEnabledManager = mockk<AutofillEnabledManager> {
        every { isAutofillEnabled } returns false
    }
    private val accessibilityEnabledManager = mockk<AccessibilityEnabledManager> {
        every { isAccessibilityEnabledStateFlow } returns MutableStateFlow(false)
    }
    private val browserThirdPartyAutofillEnabledManager =
        mockk<BrowserThirdPartyAutofillEnabledManager> {
            every { browserThirdPartyAutofillStatus } returns UNAVAILABLE_BROWSER_AUTOFILL_STATUS
        }

    private val settingsLogManager: SettingsLogManager = SettingsLogManagerImpl(
        settingsRepository = settingsRepository,
        logsManager = logsManager,
        autofillEnabledManager = autofillEnabledManager,
        accessibilityEnabledManager = accessibilityEnabledManager,
        browserThirdPartyAutofillEnabledManager = browserThirdPartyAutofillEnabledManager,
    )

    @Test
    fun `data should report every setting as disabled when nothing is enabled`() {
        assertEquals(
            """
            === Bitwarden Settings ===
            Account Security:
              Unlock with Biometrics: Disabled
              Unlock with PIN: Disabled
              Allow authenticator sync: Disabled
              Session timeout: FifteenMinutes
              Session timeout action: LOCK
            Autofill:
              Autofill Service: Disabled
              Use accessibility: Disabled
              Autofill assist: Disabled
              Copy TOTP automatically: Disabled
              Ask to add item: Disabled
              Default URI match detection: DOMAIN
            Appearance:
              Language: DEFAULT
              Theme: DEFAULT
              Use dynamic colors: Disabled
              Show website icons: Disabled
            Other:
              Allow sync on refresh: Disabled
              Clear clipboard: NEVER
              Allow screen capture: Disabled
            About:
              Submit crash logs: Disabled
              Flight recorder: Off

            """.trimIndent(),
            settingsLogManager.data,
        )
    }

    @Test
    fun `data should report every setting as enabled when everything is enabled`() {
        every { settingsRepository.isUnlockWithBiometricsEnabled } returns true
        every { settingsRepository.isUnlockWithPinEnabled } returns true
        every { settingsRepository.isPasswordOnRestartRequiredWithPin } returns true
        every { settingsRepository.isAuthenticatorSyncEnabled } returns true
        every { settingsRepository.vaultTimeout } returns VaultTimeout.Never
        every { settingsRepository.vaultTimeoutAction } returns VaultTimeoutAction.LOGOUT
        every { settingsRepository.isInlineAutofillEnabled } returns true
        every { settingsRepository.isFillAssistEnabled } returns true
        every { settingsRepository.isAutoCopyTotpDisabled } returns false
        every { settingsRepository.isAutofillSavePromptDisabled } returns false
        every { settingsRepository.defaultUriMatchType } returns UriMatchType.EXACT
        every { settingsRepository.appLanguage } returns AppLanguage.ENGLISH
        every { settingsRepository.appTheme } returns AppTheme.DARK
        every { settingsRepository.isDynamicColorsEnabled } returns true
        every { settingsRepository.isIconLoadingDisabled } returns false
        every { settingsRepository.getPullToRefreshEnabledFlow() } returns MutableStateFlow(true)
        every {
            settingsRepository.clearClipboardFrequency
        } returns ClearClipboardFrequency.TEN_SECONDS
        every { settingsRepository.isScreenCaptureAllowed } returns true
        every { settingsRepository.flightRecorderData } returns ACTIVE_FLIGHT_RECORDER_DATA_SET
        every { logsManager.isEnabled } returns true
        every { autofillEnabledManager.isAutofillEnabled } returns true
        every {
            accessibilityEnabledManager.isAccessibilityEnabledStateFlow
        } returns MutableStateFlow(true)
        every {
            browserThirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus
        } returns ENABLED_BROWSER_AUTOFILL_STATUS

        assertEquals(
            """
            === Bitwarden Settings ===
            Account Security:
              Unlock with Biometrics: Enabled
              Unlock with PIN: Enabled
                Requires MP on restart: Enabled
              Allow authenticator sync: Enabled
              Session timeout: Never
              Session timeout action: LOGOUT
            Autofill:
              Autofill Service: Enabled
                Display autofill suggestions: Inline
              Browser integration:
                BRAVE_RELEASE: On
                CHROME_STABLE: On
                CHROME_BETA: On
                VIVALDI_STABLE: On
              Use accessibility: Enabled
              Autofill assist: Enabled
              Copy TOTP automatically: Enabled
              Ask to add item: Enabled
              Default URI match detection: EXACT
            Appearance:
              Language: ENGLISH
              Theme: DARK
              Use dynamic colors: Enabled
              Show website icons: Enabled
            Other:
              Allow sync on refresh: Enabled
              Clear clipboard: TEN_SECONDS
              Allow screen capture: Enabled
            About:
              Submit crash logs: Enabled
              Flight recorder: On

            """.trimIndent(),
            settingsLogManager.data,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `data should report requires MP on restart as disabled when PIN unlock does not require it`() {
        every { settingsRepository.isUnlockWithPinEnabled } returns true
        every { settingsRepository.isPasswordOnRestartRequiredWithPin } returns false

        assertEquals(
            """
            === Bitwarden Settings ===
            Account Security:
              Unlock with Biometrics: Disabled
              Unlock with PIN: Enabled
                Requires MP on restart: Disabled
              Allow authenticator sync: Disabled
              Session timeout: FifteenMinutes
              Session timeout action: LOCK
            Autofill:
              Autofill Service: Disabled
              Use accessibility: Disabled
              Autofill assist: Disabled
              Copy TOTP automatically: Disabled
              Ask to add item: Disabled
              Default URI match detection: DOMAIN
            Appearance:
              Language: DEFAULT
              Theme: DEFAULT
              Use dynamic colors: Disabled
              Show website icons: Disabled
            Other:
              Allow sync on refresh: Disabled
              Clear clipboard: NEVER
              Allow screen capture: Disabled
            About:
              Submit crash logs: Disabled
              Flight recorder: Off

            """.trimIndent(),
            settingsLogManager.data,
        )
    }

    @Test
    fun `data should omit the requires MP on restart entry when PIN unlock is disabled`() {
        every { settingsRepository.isUnlockWithPinEnabled } returns false
        every { settingsRepository.isPasswordOnRestartRequiredWithPin } returns true

        assertEquals(
            """
            === Bitwarden Settings ===
            Account Security:
              Unlock with Biometrics: Disabled
              Unlock with PIN: Disabled
              Allow authenticator sync: Disabled
              Session timeout: FifteenMinutes
              Session timeout action: LOCK
            Autofill:
              Autofill Service: Disabled
              Use accessibility: Disabled
              Autofill assist: Disabled
              Copy TOTP automatically: Disabled
              Ask to add item: Disabled
              Default URI match detection: DOMAIN
            Appearance:
              Language: DEFAULT
              Theme: DEFAULT
              Use dynamic colors: Disabled
              Show website icons: Disabled
            Other:
              Allow sync on refresh: Disabled
              Clear clipboard: NEVER
              Allow screen capture: Disabled
            About:
              Submit crash logs: Disabled
              Flight recorder: Off

            """.trimIndent(),
            settingsLogManager.data,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `data should report the autofill suggestion display as popup when inline autofill is disabled`() {
        every { autofillEnabledManager.isAutofillEnabled } returns true
        every { settingsRepository.isInlineAutofillEnabled } returns false

        assertEquals(
            """
            === Bitwarden Settings ===
            Account Security:
              Unlock with Biometrics: Disabled
              Unlock with PIN: Disabled
              Allow authenticator sync: Disabled
              Session timeout: FifteenMinutes
              Session timeout action: LOCK
            Autofill:
              Autofill Service: Enabled
                Display autofill suggestions: Popup
              Use accessibility: Disabled
              Autofill assist: Disabled
              Copy TOTP automatically: Disabled
              Ask to add item: Disabled
              Default URI match detection: DOMAIN
            Appearance:
              Language: DEFAULT
              Theme: DEFAULT
              Use dynamic colors: Disabled
              Show website icons: Disabled
            Other:
              Allow sync on refresh: Disabled
              Clear clipboard: NEVER
              Allow screen capture: Disabled
            About:
              Submit crash logs: Disabled
              Flight recorder: Off

            """.trimIndent(),
            settingsLogManager.data,
        )
    }

    @Test
    fun `data should only list the browsers that have third party autofill available`() {
        every {
            browserThirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus
        } returns BrowserThirdPartyAutofillStatus(
            braveStableStatusData = BrowserThirdPartyAutoFillData(
                isAvailable = true,
                isThirdPartyEnabled = false,
            ),
            chromeStableStatusData = BrowserThirdPartyAutoFillData(
                isAvailable = true,
                isThirdPartyEnabled = true,
            ),
            chromeBetaChannelStatusData = UNAVAILABLE_BROWSER_AUTOFILL_DATA,
            vivaldiStableChannelStatusData = UNAVAILABLE_BROWSER_AUTOFILL_DATA,
            defaultBrowserPackageName = null,
        )

        assertEquals(
            """
            === Bitwarden Settings ===
            Account Security:
              Unlock with Biometrics: Disabled
              Unlock with PIN: Disabled
              Allow authenticator sync: Disabled
              Session timeout: FifteenMinutes
              Session timeout action: LOCK
            Autofill:
              Autofill Service: Disabled
              Browser integration:
                BRAVE_RELEASE: Off
                CHROME_STABLE: On
              Use accessibility: Disabled
              Autofill assist: Disabled
              Copy TOTP automatically: Disabled
              Ask to add item: Disabled
              Default URI match detection: DOMAIN
            Appearance:
              Language: DEFAULT
              Theme: DEFAULT
              Use dynamic colors: Disabled
              Show website icons: Disabled
            Other:
              Allow sync on refresh: Disabled
              Clear clipboard: NEVER
              Allow screen capture: Disabled
            About:
              Submit crash logs: Disabled
              Flight recorder: Off

            """.trimIndent(),
            settingsLogManager.data,
        )
    }
}

private val UNAVAILABLE_BROWSER_AUTOFILL_DATA = BrowserThirdPartyAutoFillData(
    isAvailable = false,
    isThirdPartyEnabled = false,
)

private val ENABLED_BROWSER_AUTOFILL_DATA = BrowserThirdPartyAutoFillData(
    isAvailable = true,
    isThirdPartyEnabled = true,
)

private val UNAVAILABLE_BROWSER_AUTOFILL_STATUS = BrowserThirdPartyAutofillStatus(
    braveStableStatusData = UNAVAILABLE_BROWSER_AUTOFILL_DATA,
    chromeStableStatusData = UNAVAILABLE_BROWSER_AUTOFILL_DATA,
    chromeBetaChannelStatusData = UNAVAILABLE_BROWSER_AUTOFILL_DATA,
    vivaldiStableChannelStatusData = UNAVAILABLE_BROWSER_AUTOFILL_DATA,
    defaultBrowserPackageName = null,
)

private val ENABLED_BROWSER_AUTOFILL_STATUS = BrowserThirdPartyAutofillStatus(
    braveStableStatusData = ENABLED_BROWSER_AUTOFILL_DATA,
    chromeStableStatusData = ENABLED_BROWSER_AUTOFILL_DATA,
    chromeBetaChannelStatusData = ENABLED_BROWSER_AUTOFILL_DATA,
    vivaldiStableChannelStatusData = ENABLED_BROWSER_AUTOFILL_DATA,
    defaultBrowserPackageName = null,
)

private val ACTIVE_FLIGHT_RECORDER_DATA_SET = FlightRecorderDataSet(
    data = setOf(
        FlightRecorderDataSet.FlightRecorderData(
            id = "id",
            fileName = "fileName",
            startTimeMs = 0L,
            durationMs = 100L,
            isActive = true,
        ),
    ),
)
