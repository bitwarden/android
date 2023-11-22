package com.x8bit.bitwarden.ui.platform.base

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

/**
 * A base class that can be used for performing Compose-layer testing using Robolectric, Compose
 * Testing, and JUnit 4.
 */
@Config(
    application = HiltTestApplication::class,
    sdk = [Config.NEWEST_SDK],
)
@RunWith(RobolectricTestRunner::class)
abstract class BaseComposeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    init {
        ShadowLog.stream = System.out
    }

    /**
     * Helper for testing a basic Composable function that only requires a Composable environment
     * with the [BitwardenTheme].
     */
    protected fun runTestWithTheme(
        isDarkTheme: Boolean,
        test: @Composable () -> Unit,
    ) {
        composeTestRule.setContent {
            BitwardenTheme(
                darkTheme = isDarkTheme,
            ) {
                test()
            }
        }
    }
}
