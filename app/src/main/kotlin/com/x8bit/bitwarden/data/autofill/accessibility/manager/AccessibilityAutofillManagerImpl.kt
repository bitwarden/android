package com.x8bit.bitwarden.data.autofill.accessibility.manager

import com.x8bit.bitwarden.data.autofill.accessibility.model.AccessibilityAction

/**
 * The default implementation for the [AccessibilityAutofillManager].
 */
class AccessibilityAutofillManagerImpl : AccessibilityAutofillManager {
    override var accessibilityAction: AccessibilityAction? = null
}
