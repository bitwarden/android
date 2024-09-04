package com.x8bit.bitwarden.data.autofill.manager

import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityAutofillManager
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityAutofillManagerImpl
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AccessibilityAutofillManagerTest {
    private val accessibilityAutofillManager: AccessibilityAutofillManager =
        AccessibilityAutofillManagerImpl()

    @Test
    fun `isAccessibilityTileClicked should simply hold the state it is provided`() {
        assertFalse(accessibilityAutofillManager.isAccessibilityTileClicked)
        accessibilityAutofillManager.isAccessibilityTileClicked = true
        assertTrue(accessibilityAutofillManager.isAccessibilityTileClicked)
        accessibilityAutofillManager.isAccessibilityTileClicked = false
        assertFalse(accessibilityAutofillManager.isAccessibilityTileClicked)
    }
}
