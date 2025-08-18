@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.platform.manager.utils

import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import com.bitwarden.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserPackage
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager

/**
 * Starts the system autofill settings activity.
 *
 * @param context The context from which to start the activity.
 * @return `true` if the activity was started successfully, `false` otherwise.
 */
fun IntentManager.startSystemAutofillSettingsActivity(): Boolean = startActivity(
    intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE)
        .setData("package:$packageName".toUri()),
)

/**
 * Attempts to start the system accessibility settings activity.
 */
fun IntentManager.startSystemAccessibilitySettingsActivity() {
    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
}

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

/**
 * Starts the application's settings activity.
 */
fun IntentManager.startApplicationDetailsSettingsActivity() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    intent.data = "package:$packageName".toUri()
    startActivity(intent = intent)
}

/**
 * Open the default email app on device.
 */
fun IntentManager.startDefaultEmailApplication() {
    val intent = Intent(Intent.ACTION_MAIN)
    intent.addCategory(Intent.CATEGORY_APP_EMAIL)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}
