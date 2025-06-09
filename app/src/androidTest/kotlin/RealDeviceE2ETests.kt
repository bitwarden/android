import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.x8bit.bitwarden.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RealDeviceE2ETests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testVaultLockUnlockFlow() {
        // 1. Update environment URL to test.com
        composeTestRule.onNodeWithTag("ChooseLoginButton")
            .performClick()

        composeTestRule.onNodeWithTag("RegionSelectorDropdown")
            .performClick()

        var performClick = composeTestRule.onAllNodes(hasTestTag("AlertRadioButtonOption"))
            .get(2)
            .performClick()

        composeTestRule.onNodeWithTag("ServerUrlEntry")
            .performClick()
            .performTextInput("- - - - - -- ")

        composeTestRule.onNodeWithTag("SaveButton")
            .performClick()

        // Wait for save to complete
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("ServerUrlEntry").fetchSemanticsNodes().isEmpty()
        }

        // 2. Login with test credentials
        composeTestRule.onNodeWithTag("EmailAddressEntry")
            .performClick()
            .performTextInput("- - - - - - -")

        composeTestRule.onNodeWithTag("ContinueButton")
            .performClick()

        composeTestRule.onNodeWithTag("MasterPasswordEntry")
            .performClick()
            .performTextInput("- - - - - - -")

        composeTestRule.onNodeWithTag("LogInWithMasterPasswordButton")
            .performClick()

        // Wait for login to complete and verify we're logged in
        composeTestRule.waitUntil(timeoutMillis = 20000) {
            composeTestRule.onAllNodesWithTag("SettingsTab").fetchSemanticsNodes().isNotEmpty()
        }

        // 3. Go to settings and lock vault
        composeTestRule.onNodeWithTag("SettingsTab")
            .performClick()

        composeTestRule.onNodeWithTag("AccountSecuritySettingsButton")
            .performClick()

        composeTestRule.onNodeWithTag("LockNowLabel").performScrollTo()
            .performClick()

        // Wait for vault to lock
        composeTestRule.waitUntil(timeoutMillis = 20000) {
            composeTestRule.onAllNodesWithTag("MasterPasswordEntry").fetchSemanticsNodes().isNotEmpty()
        }

        // 4. Unlock vault
        composeTestRule.onNodeWithTag("MasterPasswordEntry")
            .performClick()
            .performTextInput("- - - - - - -")

        composeTestRule.onNodeWithTag("UnlockVaultButton")
            .performClick()

        // 5. Verify vault is unlocked
        composeTestRule.waitUntil(timeoutMillis = 20000) {
            composeTestRule.onAllNodesWithTag("SettingsTab").fetchSemanticsNodes().isNotEmpty()
        }

        // Additional verification
        composeTestRule.onNodeWithTag("SettingsTab")
            .assertIsDisplayed()
    }
}
