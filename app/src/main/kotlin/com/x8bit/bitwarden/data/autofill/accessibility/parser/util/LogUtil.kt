package com.x8bit.bitwarden.data.autofill.accessibility.parser.util

import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.VisibleForTesting
import com.bitwarden.annotation.OmitFromCoverage
import timber.log.Timber

/**
 * Indicates whether Accessibility Autofill verbose logging is enabled or not. This should be
 * `false` unless you are actively debugging.
 */
@VisibleForTesting
internal const val ENABLE_ACCESSIBILITY_LOGS: Boolean = false

/**
 * Logs data about a [AccessibilityNodeInfo] that is currently being parsed.
 */
@OmitFromCoverage
internal fun AccessibilityNodeInfo.logAccessibilityNodeInfo(depth: Int = 0) {
    if (!ENABLE_ACCESSIBILITY_LOGS) return
    Timber
        .tag(tag = "BitwardenAccessibilityAutofill")
        .v(message = "${" ".repeat(n = depth)} $this")
    for (i in 0..<this.childCount) {
        this.getChild(i)?.logAccessibilityNodeInfo(depth = depth + 1)
    }
}
