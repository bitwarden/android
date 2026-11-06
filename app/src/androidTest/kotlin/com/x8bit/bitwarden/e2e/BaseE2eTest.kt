package com.x8bit.bitwarden.e2e

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.x8bit.bitwarden.MainActivity
import com.x8bit.bitwarden.e2e.config.E2eConfig
import com.x8bit.bitwarden.e2e.screens.AccessibilityDisclosureScreen
import com.x8bit.bitwarden.e2e.screens.VaultScreen
import com.x8bit.bitwarden.e2e.seeder.SeededAccount
import com.x8bit.bitwarden.e2e.seeder.SeederClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Rule

/**
 * Base class for end-to-end tests against a real server: launches [MainActivity], provides a
 * [SeederClient], and deletes seeded data after each test. The test orchestrator clears app data
 * between tests, so every test starts from the accessibility disclosure.
 */
abstract class BaseE2eTest {
    /**
     * Launches the app and drives its Compose UI. Effects run eagerly: MainActivity only removes
     * its splash screen from a composition effect, which the v2 default (queued) dispatcher never
     * runs during launch.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>(
        effectContext = UnconfinedTestDispatcher(),
    )

    /**
     * Environment settings from the instrumentation arguments.
     */
    protected val config: E2eConfig = E2eConfig.fromInstrumentationArguments()

    /**
     * Seeds this test's data; everything it creates shares one play id.
     */
    protected val seeder: SeederClient = SeederClient(config)

    /**
     * Removes this test's seeded data.
     */
    @After
    fun deleteSeededData() {
        seeder.deleteSeededData()
    }

    /**
     * Points the app at the server under test and logs in as [account].
     */
    protected fun logIn(account: SeededAccount): VaultScreen =
        AccessibilityDisclosureScreen(composeRule)
            .acknowledge()
            .chooseLogIn()
            .selectSelfHosted()
            .setServerUrl(config.serverUrl)
            .continueWithEmail(account.email)
            .logIn(account.password)
}
