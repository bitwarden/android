package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.browser.util

import com.bitwarden.core.util.persistentListOfNotNull
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserPackage
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutofillStatus
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.browser.model.BrowserAutofillSettingsOption
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Converts a [BrowserThirdPartyAutofillStatus] to a list of [BrowserAutofillSettingsOption].
 */
@Suppress("MaxLineLength")
fun BrowserThirdPartyAutofillStatus.toBrowserAutoFillSettingsOptions(): ImmutableList<BrowserAutofillSettingsOption> =
    persistentListOfNotNull(
        BrowserAutofillSettingsOption
            .BraveStable(
                isEnabled = braveStableStatusData.isThirdPartyEnabled,
                isDefault = defaultBrowserPackage == BrowserPackage.BRAVE_RELEASE,
            )
            .takeIf { this.braveStableStatusData.isAvailable },
        BrowserAutofillSettingsOption
            .ChromeStable(
                isEnabled = chromeStableStatusData.isThirdPartyEnabled,
                isDefault = defaultBrowserPackage == BrowserPackage.CHROME_STABLE,
            )
            .takeIf { this.chromeStableStatusData.isAvailable },
        BrowserAutofillSettingsOption
            .ChromeBeta(
                isEnabled = chromeBetaChannelStatusData.isThirdPartyEnabled,
                isDefault = defaultBrowserPackage == BrowserPackage.CHROME_BETA,
            )
            .takeIf { this.chromeBetaChannelStatusData.isAvailable },
        BrowserAutofillSettingsOption
            .VivaldiStable(
                isEnabled = vivaldiStableChannelStatusData.isThirdPartyEnabled,
                isDefault = defaultBrowserPackage == BrowserPackage.VIVALDI_STABLE,
            )
            .takeIf { this.vivaldiStableChannelStatusData.isAvailable },
    )
        .sortedByDescending { it.isDefault }
        .toImmutableList()
