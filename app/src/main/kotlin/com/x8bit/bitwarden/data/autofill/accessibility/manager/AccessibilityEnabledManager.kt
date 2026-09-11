package com.x8bit.bitwarden.data.autofill.accessibility.manager

import kotlinx.coroutines.flow.StateFlow

/**
 * A container for values specifying whether the accessibility service is enabled.
 */
interface AccessibilityEnabledManager {
    /**
     * Emits updates that track whether the accessibility autofill service is enabled..
     */
    val isAccessibilityEnabledStateFlow: StateFlow<Boolean>

    /**
     * Whether this app's accessibility service is currently connected.
     *
     * The service reports its own connection state because the platform cannot be asked: from
     * Android 16 onwards neither `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` nor
     * `AccessibilityManager.getEnabledAccessibilityServiceList` reveals this app's own service to
     * the app itself, so any check based on them always answers false.
     */
    var isAccessibilityServiceConnected: Boolean
}
