package com.x8bit.bitwarden.data.autofill.accessibility.manager

/**
 * A relay manager used to notify the accessibility service to attempt an autofill.
 */
interface AccessibilityAutofillManager {
    /**
     * Indicates that the Autofill tile has been clicked and we attempt an accessibility-based
     * autofill.
     */
    var isAccessibilityTileClicked: Boolean
}
