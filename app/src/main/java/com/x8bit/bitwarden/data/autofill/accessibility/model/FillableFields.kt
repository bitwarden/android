package com.x8bit.bitwarden.data.autofill.accessibility.model

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Represents the fillable fields for accessibility based autofill.
 */
data class FillableFields(
    val usernameField: AccessibilityNodeInfo?,
    val passwordFields: List<AccessibilityNodeInfo>,
) {
    val hasFields: Boolean = usernameField != null || passwordFields.isNotEmpty()
}
