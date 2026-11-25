package com.x8bit.bitwarden.e2e.screens

import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import com.bitwarden.ui.platform.resource.BitwardenString

/**
 * Accessibility Service Disclosure, shown on every launch of the standard flavor until it has
 * been acknowledged.
 */
class AccessibilityDisclosureScreen(private val rule: ComposeTestRule) {
    /**
     * Acknowledges the disclosure, revealing the welcome screen.
     */
    fun acknowledge(): WelcomeScreen {
        // The button has no testTag yet, so it is matched by its localized label.
        rule
            .waitForNode(
                hasClickAction() and
                    hasAnyDescendant(hasText(appString(BitwardenString.i_understand))),
            )
            .performClick()
        return WelcomeScreen(rule)
    }
}
