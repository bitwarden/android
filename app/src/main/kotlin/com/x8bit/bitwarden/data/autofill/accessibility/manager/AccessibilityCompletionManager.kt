package com.x8bit.bitwarden.data.autofill.accessibility.manager

import android.app.Activity
import com.bitwarden.vault.CipherView

/**
 * A manager for completing the accessibility-based autofill process after the user has made a
 * selection.
 */
interface AccessibilityCompletionManager {
    /**
     * Completes the accessibility-based autofill flow originating with the given [activity] using
     * the selected [cipherView].
     */
    fun completeAccessibilityAutofill(activity: Activity, cipherView: CipherView)
}
