package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.privilegedapps.about

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AboutPrivilegedAppsScreenTest : BitwardenComposeTest() {

    private var onNavigateBackCalled = false

    @Before
    fun setUp() {
        setContent {
            AboutPrivilegedAppsScreen(
                onNavigateBack = { onNavigateBackCalled = true },
            )
        }
    }

    @Test
    fun `on NavigateBack should call onNavigateBack`() {
        composeTestRule
            .onNodeWithContentDescription("Back")
            .performClick()

        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `content is displayed correctly`() {
        composeTestRule
            .onNodeWithText(
                "To protect users from phishing attempts, by default, Bitwarden only completes " +
                    "passkey operations through applications or web browsers trusted by Google " +
                    "or the Bitwarden community.",
            )
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Trusted by You")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(
                "These are applications or browsers that Bitwarden does not trust by default, " +
                    "but you trust to perform passkey operations.",
            )
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Trusted by the Community")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(
                "These are applications not included in the Google Play Store, but Bitwarden " +
                    "trusts to perform passkey operations after community members use and report " +
                    "them as safe.",
            )
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Trusted by Google")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(
                "These are applications Google considers safe and are available in Google’s " +
                    "Play Store.",
            )
            .assertIsDisplayed()
    }
}
