package com.x8bit.bitwarden.data.autofill.model

import com.x8bit.bitwarden.data.autofill.model.browser.BrowserPackage

/**
 * Describes what, if anything, the user still needs to turn on for autofill to work.
 */
sealed class AutofillPromptStatus {
    /**
     * The device has not been checked yet, so no prompt should be shown.
     */
    data object Unknown : AutofillPromptStatus()

    /**
     * Bitwarden is the selected autofill service and the default browser needs no setup.
     */
    data object On : AutofillPromptStatus()

    /**
     * Bitwarden is not the selected autofill service.
     */
    data object AutofillOff : AutofillPromptStatus()

    /**
     * Bitwarden is the selected autofill service, but the default browser reports that third
     * party autofill is off.
     *
     * @property browserPackage The default browser that needs third party autofill turned on.
     */
    data class BrowserOff(
        val browserPackage: BrowserPackage,
    ) : AutofillPromptStatus()
}
