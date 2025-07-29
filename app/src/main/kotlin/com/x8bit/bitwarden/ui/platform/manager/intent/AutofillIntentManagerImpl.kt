package com.x8bit.bitwarden.ui.platform.manager.intent

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserPackage

/**
 * A manager class for handling intents related to autofill settings.
 */
class AutofillIntentManagerImpl(
    private val context: Context,
) : AutofillIntentManager {
    /**
     * Starts the system autofill settings activity.
     *
     * @return `true` if the activity was successfully started, `false` otherwise.
     */
    override fun startSystemAutofillSettingsActivity(): Boolean =
        try {
            val intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE)
                .apply {
                    data = "package:${context.packageName}".toUri()
                }
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }

    /**
     * Attempts to start the system accessibility settings activity.
     */
    override fun startSystemAccessibilitySettingsActivity() {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    /**
     * Starts the browser-specific autofill settings activity.
     *
     * @param browserPackage The package information of the browser.
     * @return `true` if the activity was successfully started, `false` otherwise.
     */
    override fun startBrowserAutofillSettingsActivity(
        browserPackage: BrowserPackage,
    ): Boolean = try {
        val intent = Intent(Intent.ACTION_APPLICATION_PREFERENCES)
            .apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                addCategory(Intent.CATEGORY_APP_BROWSER)
                addCategory(Intent.CATEGORY_PREFERENCE)
                setPackage(browserPackage.packageName)
            }
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
