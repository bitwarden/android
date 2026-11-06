package com.x8bit.bitwarden.e2e.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick

/**
 * Opens the [AlertDialogRobot] and runs [block] on it.
 */
fun ComposeTestRule.alertDialog(
    block: AlertDialogRobot.() -> Unit,
): AlertDialogRobot = AlertDialogRobot(rule = this).apply(block)

/**
 * Robot for the app's dialogs (`BitwardenBasicDialog`, `BitwardenSelectionDialog`, etc.), which
 * all tag their root "AlertPopup". A dialog opens in its own window outside any screen root, so
 * screen robots never see its nodes and this robot never sees the screen's.
 *
 * The loading dialog uses the same root tag, so assertions wait for content only a finished
 * dialog has (e.g. the OK button) rather than the root alone.
 */
class AlertDialogRobot(
    rule: ComposeTestRule,
) : BaseRobot(rule = rule, screenTag = "AlertPopup") {
    /**
     * Selects the option labelled [text] in a selection dialog.
     */
    fun selectOption(text: String) {
        waitForNode(hasTestTag(Tags.OPTION) and hasAnyDescendant(hasText(text))).performClick()
    }

    /**
     * Taps the dialog's OK button.
     */
    fun tapOk() {
        waitForTag(Tags.OK_BUTTON).performClick()
    }

    /**
     * Asserts an alert with an OK button is shown, e.g. an error. Waits past the loading dialog,
     * which shares the root tag but has no button.
     */
    fun assertAlertIsDisplayed() {
        waitForTag(Tags.OK_BUTTON).assertIsDisplayed()
    }

    private object Tags {
        const val OPTION = "AlertRadioButtonOption"
        const val OK_BUTTON = "AcceptAlertButton"
    }
}
