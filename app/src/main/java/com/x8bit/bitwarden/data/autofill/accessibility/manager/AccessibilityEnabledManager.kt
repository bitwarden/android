package com.x8bit.bitwarden.data.autofill.accessibility.manager

import kotlinx.coroutines.flow.StateFlow

/**
 * A container for values specifying whether or not the accessibility service is enabled.
 */
interface AccessibilityEnabledManager {
    /**
     * Emits updates that track whether the accessibility autofill service is enabled..
     */
    val isAccessibilityEnabledStateFlow: StateFlow<Boolean>
}
