package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.browser.util

import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutoFillData
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutofillStatus
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.browser.model.BrowserAutofillSettingsOption
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BrowserThirdPartyAutofillStatusExtensionsTest {
    @Test
    fun `toBrowserAutoFillSettingsOptions should be empty if no options are available`() {
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

        val result = browserThirdPartyAutofillStatus.toBrowserAutoFillSettingsOptions()

        assertTrue(result.isEmpty())
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toBrowserAutoFillSettingsOptions should contain all options if all options are available`() {
        val browserThirdPartyAutofillStatus = BrowserThirdPartyAutofillStatus(
            braveStableStatusData = BrowserThirdPartyAutoFillData(
                isAvailable = true,
                isThirdPartyEnabled = false,
            ),
            chromeStableStatusData = BrowserThirdPartyAutoFillData(
                isAvailable = true,
                isThirdPartyEnabled = true,
            ),
            chromeBetaChannelStatusData = BrowserThirdPartyAutoFillData(
                isAvailable = true,
                isThirdPartyEnabled = false,
            ),
        )

        val result = browserThirdPartyAutofillStatus.toBrowserAutoFillSettingsOptions()

        assertEquals(
            persistentListOf(
                BrowserAutofillSettingsOption.BraveStable(enabled = false),
                BrowserAutofillSettingsOption.ChromeStable(enabled = true),
                BrowserAutofillSettingsOption.ChromeBeta(enabled = false),
            ),
            result,
        )
    }
}
