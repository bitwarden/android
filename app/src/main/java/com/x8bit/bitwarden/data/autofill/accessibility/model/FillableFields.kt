package com.x8bit.bitwarden.data.autofill.accessibility.model

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Represents the fillable fields for accessibility based autofill.
 */
data class FillableFields(
    val usernameFields: List<AccessibilityNodeInfo>,
    val passwordFields: List<AccessibilityNodeInfo>,
)
