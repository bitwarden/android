package com.x8bit.bitwarden.data.autofill.manager

import android.app.Activity
import com.bitwarden.vault.CipherView

/**
 * A manager for completing the autofill process after the user has made a selection.
 */
interface AutofillCompletionManager {

    /**
     * Completes the autofill flow originating with the given [activity] using the selected
     * [cipherView].
     */
    fun completeAutofill(
        activity: Activity,
        cipherView: CipherView,
    )
}
