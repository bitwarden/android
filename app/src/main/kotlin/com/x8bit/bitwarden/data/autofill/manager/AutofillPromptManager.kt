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
     */
    val autofillPromptStatusFlow: StateFlow<AutofillPromptStatus>
}
