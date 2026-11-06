package com.x8bit.bitwarden.e2e.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.e2e.screens.appString

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
 * The loading dialog ("Logging in…") uses the same root tag, so assertions must match content
 * specific to the dialog they expect (e.g. its title), never the root or a generic button alone.
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
     * Asserts the error alert is shown, matched by its "An error has occurred" title. Matching the
     * title rather than the root tag or OK button keeps the loading dialog, or any other dialog,
     * from passing this check.
     */
    fun assertErrorAlertIsDisplayed() {
        waitForNode(
            hasTestTag(Tags.TITLE) and hasText(appString(BitwardenString.an_error_has_occurred)),
        ).assertIsDisplayed()
    }

    private object Tags {
        const val OPTION = "AlertRadioButtonOption"
        const val OK_BUTTON = "AcceptAlertButton"
        const val TITLE = "AlertTitleText"
    }
}
