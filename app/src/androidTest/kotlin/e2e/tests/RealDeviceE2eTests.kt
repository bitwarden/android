package e2e.tests

import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import e2e.pageObjects.login.MainPage
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RealDeviceE2eTests : BaseE2eTest() {

    @Test
    fun testVaultLockUnlockFlow() {
        var vault = MainPage(composeTestRule)
            .startLogin()
            .turnOnScreenRecording()
            .openEnvironmentSettings()
            .setupEnvironment(testData.baseUrl)
            .performLogin(testData.email, testData.password)
        vault.assertVaultIsUnlocked()
        vault.navigateToSettingsPage()
            .navigateToAccountSecurity()
            .lockVault()
            .performUnlockVault(testData.password)
            .assertVaultIsUnlocked()
    }
}
