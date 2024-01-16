package com.x8bit.bitwarden.ui.platform.base

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import org.junit.Rule

/**
 * A base class that can be used for performing Compose-layer testing using Robolectric, Compose
 * Testing, and JUnit 4.
 */
abstract class BaseComposeTest : BaseRobolectricTest() {
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Helper for testing a basic Composable function that only requires a Composable environment
     * with the [BitwardenTheme].
     */
    protected fun runTestWithTheme(
        theme: AppTheme,
        test: @Composable () -> Unit,
    ) {
        composeTestRule.setContent {
            BitwardenTheme(
                theme = theme,
            ) {
                test()
            }
        }
    }
}
