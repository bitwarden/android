package com.x8bit.bitwarden.data.autofill.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Primary implementation of [AutofillEnabledManager].
 */
class AutofillEnabledManagerImpl : AutofillEnabledManager {
    private val mutableIsAutofillEnabledStateFlow = MutableStateFlow(false)

    override var isAutofillEnabled: Boolean
        get() = mutableIsAutofillEnabledStateFlow.value
        set(value) {
            mutableIsAutofillEnabledStateFlow.value = value
        }

    override val isAutofillEnabledStateFlow: StateFlow<Boolean>
        get() = mutableIsAutofillEnabledStateFlow.asStateFlow()
}
