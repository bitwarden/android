package com.x8bit.bitwarden.e2e.robots

import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.e2e.screens.appString

/**
 * Opens the [AccessibilityDisclosureRobot] and runs [block] on it.
 */
fun ComposeTestRule.accessibilityDisclosure(
    block: AccessibilityDisclosureRobot.() -> Unit,
): AccessibilityDisclosureRobot = AccessibilityDisclosureRobot(rule = this).apply(block)

/**
 * Robot for the Accessibility Service Disclosure, shown on every launch of the standard flavor
 * until it has been acknowledged.
 */
class AccessibilityDisclosureRobot(
    rule: ComposeTestRule,
) : BaseRobot(rule) {
    /**
     * Taps "I understand" to acknowledge the disclosure.
     */
    fun tapIUnderstand() {
        // The button has no testTag yet, so it is matched by its localized label.
        waitForNode(
            hasClickAction() and hasAnyDescendant(hasText(appString(BitwardenString.i_understand))),
        ).performClick()
    }
}
