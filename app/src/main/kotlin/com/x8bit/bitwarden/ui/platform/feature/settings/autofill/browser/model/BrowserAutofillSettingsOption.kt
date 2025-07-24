package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.browser.model

import android.os.Parcelable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserPackage
import kotlinx.parcelize.Parcelize

/**
 * Models an option for each type of supported browser version to enable third party autofill. Each
 * [BrowserAutofillSettingsOption] contains the associated [BrowserPackage], the [optionText]
 * to display in any UI component, and whether or not the third party autofill [isEnabled].
 */
@Parcelize
sealed class BrowserAutofillSettingsOption(val isEnabled: Boolean) : Parcelable {
    abstract val browserPackage: BrowserPackage
    abstract val optionText: Text

    /**
     * Represents the Brave release channel.
     */
    @Parcelize
    data class BraveStable(
        val enabled: Boolean,
    ) : BrowserAutofillSettingsOption(isEnabled = enabled) {
        override val browserPackage: BrowserPackage
            get() = BrowserPackage.BRAVE_RELEASE
        override val optionText: Text
            get() = BitwardenString.use_brave_autofill_integration.asText()
    }

    /**
     * Represents the stable Chrome release channel.
     */
    @Parcelize
    data class ChromeStable(
        val enabled: Boolean,
    ) : BrowserAutofillSettingsOption(isEnabled = enabled) {
        override val browserPackage: BrowserPackage
            get() = BrowserPackage.CHROME_STABLE
        override val optionText: Text
            get() = BitwardenString.use_chrome_autofill_integration.asText()
    }

    /**
     * Represents the beta Chrome release channel.
     */
    @Parcelize
    data class ChromeBeta(
        val enabled: Boolean,
    ) : BrowserAutofillSettingsOption(isEnabled = enabled) {
        override val browserPackage: BrowserPackage
            get() = BrowserPackage.CHROME_BETA
        override val optionText: Text
            get() = BitwardenString.use_chrome_beta_autofill_integration.asText()
    }
}
