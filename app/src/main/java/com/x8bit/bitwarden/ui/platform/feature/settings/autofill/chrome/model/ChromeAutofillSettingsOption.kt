package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.chrome.model

import android.os.Parcelable
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.autofill.model.chrome.ChromeReleaseChannel
import kotlinx.parcelize.Parcelize

/**
 * Models an option for an option for each type of supported version of Chrome to enable
 * third party autofill. Each [ChromeAutofillSettingsOption] contains the associated
 * [ChromeReleaseChannel], the [optionText] to display in any UI component, and
 * whether or not the third party autofill [isEnabled].
 */
@Parcelize
sealed class ChromeAutofillSettingsOption(val isEnabled: Boolean) : Parcelable {
    abstract val chromeReleaseChannel: ChromeReleaseChannel
    abstract val optionText: Text

    /**
     * Represents the stable Chrome release channel.
     */
    @Parcelize
    data class Stable(val enabled: Boolean) : ChromeAutofillSettingsOption(isEnabled = enabled) {
        override val chromeReleaseChannel: ChromeReleaseChannel
            get() = ChromeReleaseChannel.STABLE
        override val optionText: Text
            get() = R.string.use_chrome_autofill_integration.asText()
    }

    /**
     * Represents the beta Chrome release channel.
     */
    @Parcelize
    data class Beta(val enabled: Boolean) : ChromeAutofillSettingsOption(isEnabled = enabled) {
        override val chromeReleaseChannel: ChromeReleaseChannel
            get() = ChromeReleaseChannel.BETA
        override val optionText: Text
            get() = R.string.use_chrome_beta_autofill_integration.asText()
    }
}
