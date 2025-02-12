package com.bitwarden.authenticator.ui.platform.base

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.authenticator.ui.platform.theme.AuthenticatorTheme
import org.junit.Rule

/**
 * A base class that can be used for performing Compose-layer testing using Robolectric, Compose
 * Testing, and JUnit 4.
 */
abstract class BaseComposeTest : BaseRobolectricTest() {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * instance of [OnBackPressedDispatcher] made available if testing using
     *
     * [setContentWithBackDispatcher] or [runTestWithTheme]
     */
    var backDispatcher: OnBackPressedDispatcher? = null
        private set

    /**
     * Helper for testing a basic Composable function that only requires a Composable environment
     * with the [BitwardenTheme].
     */
    protected fun runTestWithTheme(
        theme: AppTheme,
        test: @Composable () -> Unit,
    ) {
        composeTestRule.setContent {
            AuthenticatorTheme(
                theme = theme,
            ) {
                backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                test()
            }
        }
    }

    /**
     * Helper for testing a basic Composable function that provides access to a
     * [OnBackPressedDispatcher].
     *
     * Use if the [Composable] function being tested uses a [BackHandler]
     */
    protected fun setContentWithBackDispatcher(test: @Composable () -> Unit) {
        composeTestRule.setContent {
            backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
            test()
        }
    }
}
