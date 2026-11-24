package com.x8bit.bitwarden.e2e

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.x8bit.bitwarden.MainActivity
import com.x8bit.bitwarden.e2e.config.E2eConfig
import com.x8bit.bitwarden.e2e.seeder.SeederClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Rule

/**
 * Base class for end-to-end tests against a real server: launches [MainActivity], provides a
 * [SeederClient], and deletes seeded data after each test. Every test expects a cleared app and
 * starts from the accessibility disclosure: the orchestrator clears app data in Gradle runs, and
 * `scripts/run-e2e-tests.sh` runs `pm clear` before each test.
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
}
