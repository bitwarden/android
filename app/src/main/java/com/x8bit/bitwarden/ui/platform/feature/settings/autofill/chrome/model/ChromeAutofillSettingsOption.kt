package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.chrome.model

import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.autofill.model.chrome.ChromeReleaseChannel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText

/**
 * Models an option for an option for each type of supported version of Chrome to enable
 * third party autofill. Each [ChromeAutofillSettingsOption] contains the associated
 * [ChromeReleaseChannel], the [optionText] to display in any UI component, and
 * whether or not the third party autofill [isEnabled].
 */
sealed class ChromeAutofillSettingsOption(val isEnabled: Boolean) {
    abstract val chromeReleaseChannel: ChromeReleaseChannel
    abstract val optionText: Text

    /**
     * Represents the stable Chrome release channel.
     */
    data class Stable(val enabled: Boolean) : ChromeAutofillSettingsOption(isEnabled = enabled) {
        override val chromeReleaseChannel: ChromeReleaseChannel = ChromeReleaseChannel.STABLE
        override val optionText: Text = R.string.use_chrome_autofill_integration.asText()
    }

    /**
     * Represents the beta Chrome release channel.
     */
    data class Beta(val enabled: Boolean) : ChromeAutofillSettingsOption(isEnabled = enabled) {
        override val chromeReleaseChannel: ChromeReleaseChannel = ChromeReleaseChannel.BETA
        override val optionText: Text = R.string.use_chrome_beta_autofill_integration.asText()
    }
}
