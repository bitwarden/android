package com.x8bit.bitwarden.data.autofill.accessibility.processor

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * A class to handle accessibility event processing. This only includes fill requests.
 */
interface BitwardenAccessibilityProcessor {
    /**
     * Processes the [AccessibilityEvent] for autofill options and grant access to the current
     * [AccessibilityNodeInfo] via the [rootAccessibilityNodeInfoProvider] (note that calling the
     * `rootAccessibilityNodeInfoProvider` is expensive).
     */
    fun processAccessibilityEvent(
        event: AccessibilityEvent,
        rootAccessibilityNodeInfoProvider: () -> AccessibilityNodeInfo?,
    )
}
