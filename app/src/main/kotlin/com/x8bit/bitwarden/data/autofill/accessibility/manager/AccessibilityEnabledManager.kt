package com.x8bit.bitwarden.data.autofill.accessibility.manager

import kotlinx.coroutines.flow.StateFlow

/**
 * Tracks whether the accessibility service is enabled.
 */
interface AccessibilityEnabledManager {
    /**
     * Emits updates tracking whether the accessibility service is enabled.
     */
    val isAccessibilityEnabledStateFlow: StateFlow<Boolean>

    /**
     * Reports whether the accessibility service is connected without changing system settings.
     */
    fun setAccessibilityServiceConnected(isConnected: Boolean)
}
