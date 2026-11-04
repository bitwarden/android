package com.x8bit.bitwarden.ui.auth

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.x8bit.bitwarden.MainActivity
import com.x8bit.bitwarden.robot.landing
import com.x8bit.bitwarden.robot.logInWithMasterPassword
import com.x8bit.bitwarden.robot.login
import com.x8bit.bitwarden.robot.vault
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sample end-to-end login tests written with the robot pattern.
 *
 * Each block names the screen its steps happen on. No test tags or Compose APIs show up here.
 *
 * Test names use underscores rather than backtick sentences: instrumented tests are dexed with
 * the app's `minSdk` (29), and spaces in method names need API 30+.
 */
@RunWith(AndroidJUnit4::class)
class LoginTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun continueButton_shouldBeDisabled_untilAnEmailIsEntered() {
        with(composeTestRule) {
            landing {
                assertLandingScreenIsDisplayed()
                assertContinueButtonIsEnabled(isEnabled = false)

                enterEmail(email = TEST_EMAIL)
                assertContinueButtonIsEnabled(isEnabled = true)
            }
        }
    }

    @Test
    fun tappingContinue_shouldNavigateToLoginScreen_forTheEnteredEmail() {
        with(composeTestRule) {
            landing {
                enterEmail(email = TEST_EMAIL)
                tapContinue()
            }
            login {
                assertLoginScreenIsDisplayed()
                assertLoggingInAs(email = TEST_EMAIL)
            }
        }
    }

    @Test
    fun tappingNotYou_shouldReturnToLandingScreen() {
        with(composeTestRule) {
            landing {
                enterEmail(email = TEST_EMAIL)
                tapContinue()
            }
            login {
                tapNotYou()
            }
            landing {
                assertLandingScreenIsDisplayed()
            }
        }
    }

    @Test
    fun loggingIn_withAnInvalidMasterPassword_shouldShowAnErrorAlert() {
        with(composeTestRule) {
            logInWithMasterPassword(email = TEST_EMAIL, password = "wrong-password")

            login {
                assertErrorAlertIsDisplayed()
            }
        }
    }

    @Test
    fun loggingIn_withAValidMasterPassword_shouldNavigateToTheVault() {
        with(composeTestRule) {
            logInWithMasterPassword(email = TEST_EMAIL, password = TEST_PASSWORD)

            vault {
                assertVaultIsDisplayed()
            }
        }
    }

    private companion object {
        // Placeholder credentials. Real runs should read these from instrumentation
        // arguments or point at a fake server, never hard-coded secrets.
        const val TEST_EMAIL = "robot-test@bitwarden.com"
        const val TEST_PASSWORD = "placeholder-password"
    }
}
