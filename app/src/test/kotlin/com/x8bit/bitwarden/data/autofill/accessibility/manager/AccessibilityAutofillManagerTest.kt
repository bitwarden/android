package com.x8bit.bitwarden.data.autofill.accessibility.manager

import com.x8bit.bitwarden.data.autofill.accessibility.model.AccessibilityAction
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AccessibilityAutofillManagerTest {
    private val accessibilityAutofillManager: AccessibilityAutofillManager =
        AccessibilityAutofillManagerImpl()

    @Test
    fun `isAccessibilityTileClicked should simply hold the state it is provided`() {
        val attemptParseUri = AccessibilityAction.AttemptParseUri
        val attemptFill = AccessibilityAction.AttemptFill(cipherView = mockk(), uri = mockk())
        assertNull(accessibilityAutofillManager.accessibilityAction)
        accessibilityAutofillManager.accessibilityAction = attemptParseUri
        assertEquals(attemptParseUri, accessibilityAutofillManager.accessibilityAction)
        accessibilityAutofillManager.accessibilityAction = attemptFill
        assertEquals(attemptFill, accessibilityAutofillManager.accessibilityAction)
        accessibilityAutofillManager.accessibilityAction = null
        assertNull(accessibilityAutofillManager.accessibilityAction)
    }
}
