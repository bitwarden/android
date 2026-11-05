package com.x8bit.bitwarden.e2e.robots

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement

/**
 * Opens the [EnvironmentRobot] and runs [block] on it.
 */
fun ComposeTestRule.environment(
    block: EnvironmentRobot.() -> Unit,
): EnvironmentRobot = EnvironmentRobot(rule = this).apply(block)

/**
 * Robot for the self-hosted Environment settings screen.
 */
class EnvironmentRobot(
    rule: ComposeTestRule,
) : BaseRobot(rule) {
    /**
     * Enters [url] as the server URL.
     */
    fun enterServerUrl(url: String) {
        waitForTag(Tags.SERVER_URL_FIELD).performTextReplacement(url)
    }

    /**
     * Taps "Save" to save the environment.
     */
    fun tapSave() {
        waitForTag(Tags.SAVE_BUTTON).performClick()
    }

    private object Tags {
        const val SERVER_URL_FIELD = "ServerUrlEntry"
        const val SAVE_BUTTON = "SaveButton"
    }
}
