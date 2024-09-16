package com.x8bit.bitwarden.data.autofill.accessibility.manager

import kotlinx.coroutines.flow.StateFlow

/**
 * A container for values specifying whether or not the accessibility service is enabled.
 */
interface AccessibilityEnabledManager {
    /**
     * Whether or not the accessibility service should be considered enabled.
     *
     * Note that changing this does not enable or disable autofill; it is only an indicator that
     * this has occurred elsewhere.
     */
    var isAccessibilityEnabled: Boolean

    /**
     * Emits updates that track [isAccessibilityEnabled] values.
     */
    val isAccessibilityEnabledStateFlow: StateFlow<Boolean>
}
