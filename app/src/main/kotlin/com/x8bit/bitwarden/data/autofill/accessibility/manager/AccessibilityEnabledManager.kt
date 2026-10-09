package com.x8bit.bitwarden.data.autofill.accessibility.manager

import kotlinx.coroutines.flow.StateFlow

/**
 * Tracks whether accessibility autofill should be considered enabled.
 */
interface AccessibilityEnabledManager {
    /**
     * Initially reflects the platform-reported enabled state, then follows connection updates
     * reported by the accessibility service.
     */
    val isAccessibilityEnabledStateFlow: StateFlow<Boolean>

    /**
     * Updates the enabled state to reflect whether the accessibility service is connected.
     *
     * Connection callbacks remain usable when platform queries return empty results despite the
     * service being enabled and bound, as observed in affected Android 16 builds.
     * This only reports the connection state; it does not enable or disable the service.
     */
    fun setAccessibilityServiceConnected(isConnected: Boolean)
}
