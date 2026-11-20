package com.x8bit.bitwarden.ui.platform.util

import androidx.annotation.StringRes
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserPackage

/**
 * The string resource for the browser's product name for the given [BrowserPackage].
 */
@get:StringRes
val BrowserPackage.displayLabelRes: Int
    get() = when (this) {
        BrowserPackage.BRAVE_RELEASE -> BitwardenString.brave
        BrowserPackage.CHROME_STABLE -> BitwardenString.chrome
        BrowserPackage.CHROME_BETA -> BitwardenString.chrome_beta
        BrowserPackage.VIVALDI_STABLE -> BitwardenString.vivaldi
    }
