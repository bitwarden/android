package com.x8bit.bitwarden.data.autofill.accessibility.manager

import android.app.Activity
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.autofill.accessibility.model.AccessibilityAction
import com.x8bit.bitwarden.data.autofill.accessibility.util.toUriOrNull
import com.x8bit.bitwarden.data.autofill.manager.AutofillTotpManager
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.autofill.util.getAutofillSelectionDataOrNull
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Default implementation for the [AccessibilityCompletionManager].
 */
class AccessibilityCompletionManagerImpl(
    private val accessibilityAutofillManager: AccessibilityAutofillManager,
    private val totpManager: AutofillTotpManager,
    dispatcherManager: DispatcherManager,
) : AccessibilityCompletionManager {
    private val mainScope = CoroutineScope(dispatcherManager.main)

    override fun completeAccessibilityAutofill(activity: Activity, cipherView: CipherView) {
        val autofillSelectionData = activity
            .intent
            ?.getAutofillSelectionDataOrNull()
            ?: run {
                activity.finishAndRemoveTask()
                return
            }
        if (autofillSelectionData.framework != AutofillSelectionData.Framework.ACCESSIBILITY) {
            activity.finishAndRemoveTask()
            return
        }
        val uri = autofillSelectionData
            .uri
            ?.toUriOrNull()
            ?: run {
                activity.finishAndRemoveTask()
                return
            }

        accessibilityAutofillManager.accessibilityAction = AccessibilityAction.AttemptFill(
            cipherView = cipherView,
            uri = uri,
        )
        mainScope.launch {
            totpManager.tryCopyTotpToClipboard(cipherView = cipherView)
        }
        activity.finishAndRemoveTask()
    }
}
