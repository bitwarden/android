package com.x8bit.bitwarden.ui.platform.manager.intent

import com.x8bit.bitwarden.data.autofill.model.browser.BrowserPackage

/**
 * A manager interface for handling intents related to autofill.
 */
interface AutofillIntentManager {
    /**
     * Starts the system autofill settings activity.
     *
     * @return `true` if the activity was successfully started, `false` otherwise.
     */
    fun startSystemAutofillSettingsActivity(): Boolean

    /**
     * Attempts to start the system accessibility settings activity.
     */
    fun startSystemAccessibilitySettingsActivity()

    /**
     * Starts the browser-specific autofill settings activity.
     *
     * @param browserPackage The package information of the browser.
     * @return `true` if the activity was successfully started, `false` otherwise.
     */
    fun startBrowserAutofillSettingsActivity(
        browserPackage: BrowserPackage,
    ): Boolean
}
