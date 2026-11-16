package com.x8bit.bitwarden.data.autofill.model

import android.os.Parcelable
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserPackage
import kotlinx.parcelize.Parcelize

/**
 * Describes what, if anything, the user still needs to turn on for autofill to work.
 */
sealed class AutofillPromptStatus : Parcelable {
    /**
     * The device has not been checked yet, so no prompt should be shown.
     */
    @Parcelize
    data object Unknown : AutofillPromptStatus()

    /**
     * Bitwarden is the selected autofill service and the default browser needs no setup.
     */
    @Parcelize
    data object On : AutofillPromptStatus()

    /**
     * Bitwarden is not the selected autofill service.
     */
    @Parcelize
    data object AutofillOff : AutofillPromptStatus()

    /**
     * Bitwarden is the selected autofill service, but the default browser reports that third
     * party autofill is off.
     *
     * @property browserPackage The default browser that needs third party autofill turned on.
     */
    @Parcelize
    data class BrowserOff(
        val browserPackage: BrowserPackage,
    ) : AutofillPromptStatus()
}
