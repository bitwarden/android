@file:OmitFromCoverage

package com.bitwarden.ui.platform.manager.util

import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.manager.IntentManager

/**
 * Starts the system autofill settings activity.
 *
 * @param context The context from which to start the activity.
 * @return `true` if the activity was started successfully, `false` otherwise.
 */
fun IntentManager.startSystemAutofillSettingsActivity(): Boolean {
    val intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE)
        .setData("package:$packageName".toUri())
    return startActivity(intent = intent)
}

/**
 * Attempts to start the system accessibility settings activity.
 */
fun IntentManager.startSystemAccessibilitySettingsActivity(): Boolean {
    return startActivity(intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
}

/**
 * Starts the application's settings activity.
 */
fun IntentManager.startAppSettingsActivity(): Boolean {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData("package:$packageName".toUri())
    return startActivity(intent = intent)
}

/**
 * Open the default email app on device.
 */
fun IntentManager.startDefaultEmailApplication(): Boolean {
    val intent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_APP_EMAIL)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return startActivity(intent = intent)
}
