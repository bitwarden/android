package com.x8bit.bitwarden.e2e

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.x8bit.bitwarden.MainActivity
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RealDeviceE2ETests {

    @Before
    fun setup() {
        // Clear any existing state
        InstrumentationRegistry.getInstrumentation().targetContext.packageManager
            .clearPackagePreferredActivities(InstrumentationRegistry.getInstrumentation().targetContext.packageName)
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testVaultLockUnlockFlow() {
        // 1. Update environment URL to test.com
        composeTestRule.onNodeWithTag("ChooseLoginButton")
            .performClick()

        composeTestRule.onNodeWithTag("RegionSelectorDropdown")
            .performClick()

        composeTestRule.onNodeWithTag("ServerUrlEntry")
            .performClick()
            .performTextInput("test.com")

        composeTestRule.onNodeWithTag("SaveButton")
            .performClick()

        // Wait for save to complete
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("EmailEntry").fetchSemanticsNodes().isNotEmpty()
        }

        // 2. Login with test credentials
        composeTestRule.onNodeWithTag("EmailEntry")
            .performClick()
            .performTextInput("test@bitwarden.com")

        composeTestRule.onNodeWithTag("MasterPasswordEntry")
            .performClick()
            .performTextInput("password123")

        composeTestRule.onNodeWithTag("LoginButton")
            .performClick()

        // Wait for login to complete and verify we're logged in
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("SettingsButton").fetchSemanticsNodes().isNotEmpty()
        }

        // 3. Go to settings and lock vault
        composeTestRule.onNodeWithTag("SettingsButton")
            .performClick()

        composeTestRule.onNodeWithTag("LockNowButton")
            .performClick()

        // Wait for vault to lock
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("MasterPasswordEntry").fetchSemanticsNodes().isNotEmpty()
        }

        // 4. Unlock vault
        composeTestRule.onNodeWithTag("MasterPasswordEntry")
            .performClick()
            .performTextInput("password123")

        composeTestRule.onNodeWithTag("UnlockButton")
            .performClick()

        // 5. Verify vault is unlocked
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("SettingsButton").fetchSemanticsNodes().isNotEmpty()
        }

        // Additional verification
        composeTestRule.onNodeWithTag("SettingsButton")
            .assertIsDisplayed()
    }
}
