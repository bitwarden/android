package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.browser.util

import com.bitwarden.core.util.persistentListOfNotNull
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutofillStatus
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.browser.model.BrowserAutofillSettingsOption
import kotlinx.collections.immutable.ImmutableList

/**
 * Converts a [BrowserThirdPartyAutofillStatus] to a list of [BrowserAutofillSettingsOption].
 */
@Suppress("MaxLineLength")
fun BrowserThirdPartyAutofillStatus.toBrowserAutoFillSettingsOptions(): ImmutableList<BrowserAutofillSettingsOption> =
    persistentListOfNotNull(
        BrowserAutofillSettingsOption.BraveStable(braveStableStatusData.isThirdPartyEnabled)
            .takeIf { this.braveStableStatusData.isAvailable },
        BrowserAutofillSettingsOption.ChromeStable(chromeStableStatusData.isThirdPartyEnabled)
            .takeIf { this.chromeStableStatusData.isAvailable },
        BrowserAutofillSettingsOption.ChromeBeta(chromeBetaChannelStatusData.isThirdPartyEnabled)
            .takeIf { this.chromeBetaChannelStatusData.isAvailable },
    )
