@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.platform.manager.utils

import android.content.Intent
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.manager.IntentManager
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserPackage

/**
 * Starts the browser autofill settings activity for the provided [browserPackage].
 */
fun IntentManager.startBrowserAutofillSettingsActivity(
    browserPackage: BrowserPackage,
): Boolean {
    val intent = Intent(Intent.ACTION_APPLICATION_PREFERENCES)
        .apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addCategory(Intent.CATEGORY_APP_BROWSER)
            addCategory(Intent.CATEGORY_PREFERENCE)
            setPackage(browserPackage.packageName)
        }
    return startActivity(intent)
}
