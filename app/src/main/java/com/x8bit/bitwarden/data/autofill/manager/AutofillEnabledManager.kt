package com.x8bit.bitwarden.data.autofill.manager

import kotlinx.coroutines.flow.StateFlow

/**
 * A container for values specifying whether or not autofill is enabled. These values should be
 * filled by an [AutofillActivityManager].
 */
interface AutofillEnabledManager {
    /**
     * Whether or not autofill should be considered enabled.
     *
     * Note that changing this does not enable or disable autofill; it is only an indicator that
     * this has occurred elsewhere.
     */
    var isAutofillEnabled: Boolean

    /**
     * Emits updates that track [isAutofillEnabled] values.
     */
    val isAutofillEnabledStateFlow: StateFlow<Boolean>
}
