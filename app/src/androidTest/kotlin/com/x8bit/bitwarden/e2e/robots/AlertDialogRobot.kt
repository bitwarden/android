package com.x8bit.bitwarden.e2e.robots

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

    private object Tags {
        const val OPTION = "AlertRadioButtonOption"
    }
}
