package e2e.tests

import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import e2e.pageObjects.login.MainPage
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RealDeviceE2ETests : BaseE2ETest() {

    @Test
    fun testVaultLockUnlockFlow() {
        MainPage(composeTestRule)
            .startLogin()
            .turnOnScreenRecording()
            .openEnvironmentSettings()
            .setupEnvironment(testData.baseUrl)
            .performLogin(testData.email, testData.password)
            .navigateToSettingsPage()
            .navigateToAccountSecurity()
            .lockVault()
            .performUnlockVault(testData.password)
            .assertVaultIsUnlocked()
    }
}
