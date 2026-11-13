package com.x8bit.bitwarden.data.autofill.manager

import com.x8bit.bitwarden.data.autofill.model.AutofillPromptStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Provides the [AutofillPromptStatus] used to decide whether to prompt the user to turn on
 * autofill.
 */
interface AutofillPromptManager {
    /**
     * Emits the current [AutofillPromptStatus]. This is [AutofillPromptStatus.Unknown] until the
     * device has been checked, and updates each time the device is checked again.
     *
     * This ignores [snoozeVaultPrompt] and [optOutOfVaultPrompt].
     */
    val autofillPromptStatusFlow: StateFlow<AutofillPromptStatus>

    /**
     * Emits the prompt to show on the vault: either [AutofillPromptStatus.AutofillOff] or
     * [AutofillPromptStatus.BrowserOff]. Emits `null` when no prompt should be shown, because
     * nothing is off, the device has not been checked yet, the prompt is snoozed, or the user
     * has opted out.
     */
    val vaultAutofillPromptFlow: StateFlow<AutofillPromptStatus?>

    /**
     * Hides the vault prompt for 24 hours, for every account on the device.
     */
    fun snoozeVaultPrompt()

    /**
     * Permanently hides the vault prompt, for every account on the device.
     */
    fun optOutOfVaultPrompt()
}
