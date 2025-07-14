package e2e.pageObjects

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick

/**
 * Base class for all page objects in the Bitwarden app.
 * Provides a shared ComposeTestRule instance for UI testing.
 */
abstract class Page(protected val composeTestRule: ComposeTestRule) {
    companion object {
        val TIMEOUT_MILLIS = 30000L
    }

    /**
     * Waits for an element with the specified test tag to be present and returns its SemanticsNodeInteraction.
     * @param testTag The test tag of the element to wait for
     * @return SemanticsNodeInteraction for the found element
     * @throws AssertionError if the element is not found within the timeout period
     */
    protected fun getElement(testTag: String): SemanticsNodeInteraction {
        waitForIdle()
        waitUntil(TIMEOUT_MILLIS) {
            try {
                composeTestRule.onNodeWithTag(testTag).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        return composeTestRule.onNodeWithTag(testTag)
    }

    protected fun getElementByText(text: String): SemanticsNodeInteraction {
        waitForIdle()
        waitUntil(TIMEOUT_MILLIS) {
            try {
                composeTestRule.onNodeWithText(text).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        return composeTestRule.onNodeWithText(text)
    }

    /**
     * Waits for the app to be idle before proceeding with any UI interactions.
     * This helps prevent flaky tests by ensuring the UI is stable.
     */
    protected fun waitForIdle() {
        composeTestRule.waitForIdle()
    }

    /**
     * Waits for a specific condition to be true before proceeding.
     * @param timeoutMillis Maximum time to wait in milliseconds
     * @param condition The condition to wait for
     */
    protected fun waitUntil(
        timeoutMillis: Long,
        condition: () -> Boolean,
    ) {
        composeTestRule.waitUntil(timeoutMillis) { condition() }
    }

    /**
     * Performs a click action on a node with the given test tag.
     * @param testTag The test tag of the node to click
     */
    protected fun clickOnNodeWithTag(testTag: String) {
        getElement(testTag).performClick()
    }

    /**
     * Verifies that a node with the given test tag is displayed.
     * @param testTag The test tag of the node to verify
     */
    protected fun verifyNodeWithTagIsDisplayed(testTag: String) {
        getElement(testTag).assertIsDisplayed()
    }

    /**
     * Verifies that a node with the given test tag is not displayed.
     * @param testTag The test tag of the node to verify
     */
    protected fun verifyNodeWithTagIsNotDisplayed(testTag: String) {
        composeTestRule.onNodeWithTag(testTag).assertDoesNotExist()
    }

    /**
     * Verifies that a node with the given test tag is enabled.
     * @param testTag The test tag of the node to verify
     */
    protected fun verifyNodeWithTagIsEnabled(testTag: String) {
        getElement(testTag).assertIsEnabled()
    }

    /**
     * Verifies that a node with the given test tag is disabled.
     * @param testTag The test tag of the node to verify
     */
    protected fun verifyNodeWithTagIsDisabled(testTag: String) {
        getElement(testTag).assertIsNotEnabled()
    }

    /**
     * Verifies that a node with the given test tag has the expected text.
     * @param testTag The test tag of the node to verify
     * @param expectedText The expected text content
     */
    protected fun verifyNodeWithTagHasText(testTag: String, expectedText: String) {
        getElement(testTag).assertTextEquals(expectedText)
    }
}
