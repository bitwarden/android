package com.x8bit.bitwarden.android.authenticator

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.authenticator.ui.platform.feature.tutorial.TutorialScreen
import com.bitwarden.authenticator.ui.platform.feature.tutorial.TutorialViewModel
import com.bitwarden.authenticator.ui.platform.theme.AuthenticatorTheme
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.locale.LocaleTestRule

class ExampleInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun screenshotTutorialSlides_Dark() {
        val viewModel = TutorialViewModel()

        composeTestRule.setContent {
            AuthenticatorTheme(theme = AppTheme.DARK) {
                TutorialScreen(
                    viewModel = viewModel,
                    onTutorialFinished = {},
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Continue")
            .isDisplayed()

        composeTestRule
            .onNodeWithText("Secure your accounts with Bitwarden Authenticator")
            .isDisplayed()

        Screengrab.screenshot("IntroSlide_Dark")

        composeTestRule
            .onNodeWithText("Continue")
            .performClick()

        composeTestRule
            .onNodeWithText("Use your device camera to scan codes")
            .assertIsDisplayed()

        Screengrab.screenshot("QrCodeSlide_Dark")

        composeTestRule
            .onNodeWithText("Continue")
            .performClick()

        composeTestRule
            .onNodeWithText("Sign in using unique codes")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Continue")
            .assertDoesNotExist()

        composeTestRule
            .onNodeWithText("Get started")
            .isDisplayed()

        Screengrab.screenshot("UniqueCodesSlide_Dark")
    }

    @Test
    fun screenshotTutorialSlides_Light() {
        val viewModel = TutorialViewModel()

        composeTestRule.setContent {
            AuthenticatorTheme(theme = AppTheme.LIGHT) {
                TutorialScreen(
                    viewModel = viewModel,
                    onTutorialFinished = {},
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Continue")
            .isDisplayed()

        composeTestRule
            .onNodeWithText("Secure your accounts with Bitwarden Authenticator")
            .isDisplayed()

        Screengrab.screenshot("IntroSlide_Light")

        composeTestRule
            .onNodeWithText("Continue")
            .performClick()

        composeTestRule
            .onNodeWithText("Use your device camera to scan codes")
            .assertIsDisplayed()

        Screengrab.screenshot("QrCodeSlide_Light")

        composeTestRule
            .onNodeWithText("Continue")
            .performClick()

        composeTestRule
            .onNodeWithText("Sign in using unique codes")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Continue")
            .assertDoesNotExist()

        composeTestRule
            .onNodeWithText("Get started")
            .isDisplayed()

        Screengrab.screenshot("UniqueCodesSlide_Light")
    }

    @Suppress("UndocumentedPublicClass")
    companion object {
        @JvmField
        @ClassRule
        val localeTestRule: LocaleTestRule = LocaleTestRule()
    }
}
