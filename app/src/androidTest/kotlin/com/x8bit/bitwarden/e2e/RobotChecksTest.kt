package com.x8bit.bitwarden.e2e

import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.x8bit.bitwarden.e2e.robots.BaseRobot
import com.x8bit.bitwarden.e2e.robots.accessibilityDisclosure
import com.x8bit.bitwarden.e2e.robots.alertDialog
import com.x8bit.bitwarden.e2e.robots.goToLoginScreen
import com.x8bit.bitwarden.e2e.robots.login
import com.x8bit.bitwarden.e2e.robots.vault
import com.x8bit.bitwarden.e2e.seeder.createFreeUser
import com.x8bit.bitwarden.ui.platform.util.ScreenTags
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Checks that the robots' own checks fail when they should, so a robot test can't pass for the
 * wrong reason. Each test puts the app in a state where a check must fail and expects it to.
 *
 * The failing checks wait out their full timeout (about 30 seconds each) before failing.
 */
@RunWith(AndroidJUnit4::class)
class RobotChecksTest : BaseE2eTest() {
    /**
     * The error alert check must fail after a successful login. The "Logging in…" loading dialog
     * shares the alert's root tag, so a check on the root alone would pass on the spinner.
     */
    @Test
    fun errorAlertCheck_afterCorrectMasterPassword_fails() {
        val account = seeder.createFreeUser()

        with(composeRule) {
            goToLoginScreen(serverUrl = config.serverUrl, email = account.email)

            login {
                enterMasterPassword(password = account.password)
                tapLogInWithMasterPassword()
            }

            assertCheckFails("error alert after a correct password") {
                alertDialog {
                    assertErrorAlertIsDisplayed()
                }
            }

            // The login really succeeded, so the check failed because there was no error.
            vault {
                assertVaultIsDisplayed()
            }
        }
    }

    /**
     * Opening a robot for a screen that isn't shown must fail, naming the screen it expected.
     */
    @Test
    fun openingRobot_forScreenNotShown_failsNamingTheScreen() {
        with(composeRule) {
            // A fresh install opens on the accessibility disclosure, not the Login screen.
            accessibilityDisclosure { }

            val error = assertThrows(AssertionError::class.java) {
                login { }
            }
            assertTrue(
                "Expected the failure to name ${ScreenTags.LOGIN}, got: ${error.message}",
                error.message.orEmpty().contains(ScreenTags.LOGIN),
            )
        }
    }

    /**
     * A robot with a blank screen tag must fail straight away rather than match everything.
     */
    @Test
    fun robot_withBlankScreenTag_failsImmediately() {
        assertThrows(IllegalArgumentException::class.java) {
            BlankScreenTagRobot(rule = composeRule)
        }
    }

    /**
     * Runs [check] and fails the test if it passes. A robot check fails with an [AssertionError]
     * when an assertion fails, or a [ComposeTimeoutException] when a node never appears.
     */
    private fun assertCheckFails(description: String, check: () -> Unit) {
        try {
            check()
        } catch (_: AssertionError) {
            return
        } catch (_: ComposeTimeoutException) {
            return
        }
        fail("Expected the $description check to fail, but it passed.")
    }

    /**
     * A robot with a blank screen tag, which [BaseRobot] must reject.
     */
    private class BlankScreenTagRobot(rule: ComposeTestRule) : BaseRobot(rule, screenTag = " ")
}
