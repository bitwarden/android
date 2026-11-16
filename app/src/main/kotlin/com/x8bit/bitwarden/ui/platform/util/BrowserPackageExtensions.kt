package com.x8bit.bitwarden.ui.platform.util

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserPackage

/**
 * Provides the browser's product name for the given [BrowserPackage].
 */
val BrowserPackage.displayLabel: Text
    get() = when (this) {
        BrowserPackage.BRAVE_RELEASE -> BitwardenString.brave
        BrowserPackage.CHROME_STABLE -> BitwardenString.chrome
        BrowserPackage.CHROME_BETA -> BitwardenString.chrome_beta
        BrowserPackage.VIVALDI_STABLE -> BitwardenString.vivaldi
    }
        .asText()
