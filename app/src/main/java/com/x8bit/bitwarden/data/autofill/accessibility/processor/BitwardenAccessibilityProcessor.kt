package com.x8bit.bitwarden.data.autofill.accessibility.processor

import android.view.accessibility.AccessibilityNodeInfo

/**
 * A class to handle accessibility event processing. This only includes fill requests.
 */
interface BitwardenAccessibilityProcessor {
    /**
     * Processes the [AccessibilityNodeInfo] for autofill options.
     */
    fun processAccessibilityEvent(rootAccessibilityNodeInfo: AccessibilityNodeInfo?)
}
