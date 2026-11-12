package com.x8bit.bitwarden.data.autofill.manager

import kotlinx.coroutines.flow.StateFlow

/**
 * A container for values specifying whether autofill is enabled. These values should be
 * filled by an [AutofillActivityManager].
 */
interface AutofillEnabledManager {
    /**
     * Whether autofill should be considered enabled.
     *
     * Note that changing this does not enable or disable autofill; it is only an indicator that
     * this has occurred elsewhere.
     */
    var isAutofillEnabled: Boolean

    /**
     * Emits updates that track [isAutofillEnabled] values.
     */
    val isAutofillEnabledStateFlow: StateFlow<Boolean>

    /**
     * Emits `true` once [isAutofillEnabled] has been set at least once. Until then,
     * [isAutofillEnabled] holds a placeholder `false` that does not reflect the device.
     *
     * Callers that prompt the user based on [isAutofillEnabled] (such as the autofill banner)
     * should wait for this to be `true`. Otherwise the placeholder reads as "disabled" and the
     * prompt can briefly appear on launch for users whose autofill is already on.
     */
    val isAutofillEnabledCheckedFlow: StateFlow<Boolean>
}
