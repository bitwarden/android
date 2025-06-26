package e2e.tests

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.x8bit.bitwarden.MainActivity
import e2e.pageObjects.login.MainPage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RealDeviceE2ETests {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    // Workaround to find Compose UI elements on Espresso tests
    @get:Rule
    val composeTestRule: ComposeTestRule = createEmptyComposeRule()

    @Test
    fun testVaultLockUnlockFlow() {
        MainPage(composeTestRule)
            .startLogin()
            .openEnvironmentSettings()
            .setupEnvironment("-")
            .performLogin("-", "-")
            .navigateToSettingsPage()
            .navigateToAccountSecurity()
            .lockVault()
            .performUnlockVault("-")
            .assertVaultIsUnlocked()
    }
}
