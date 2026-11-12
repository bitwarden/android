package com.x8bit.bitwarden.data.autofill.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Primary implementation of [AutofillEnabledManager].
 */
class AutofillEnabledManagerImpl : AutofillEnabledManager {
    private val mutableIsAutofillEnabledStateFlow = MutableStateFlow(false)

    // Flipped to true by the first set of isAutofillEnabled and never reset, so callers can tell
    // the placeholder false apart from a real value.
    private val mutableIsAutofillEnabledCheckedStateFlow = MutableStateFlow(false)

    override var isAutofillEnabled: Boolean
        get() = mutableIsAutofillEnabledStateFlow.value
        set(value) {
            mutableIsAutofillEnabledStateFlow.value = value
            mutableIsAutofillEnabledCheckedStateFlow.value = true
        }

    override val isAutofillEnabledStateFlow: StateFlow<Boolean>
        get() = mutableIsAutofillEnabledStateFlow.asStateFlow()

    override val isAutofillEnabledCheckedFlow: StateFlow<Boolean>
        get() = mutableIsAutofillEnabledCheckedStateFlow.asStateFlow()
}
