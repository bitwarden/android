package com.x8bit.bitwarden.ui.platform.base

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule

/**
 * A base class that can be used for performing Compose-layer testing using Robolectric, Compose
 * Testing, and JUnit 4.
 */
abstract class BaseComposeTest : BaseRobolectricTest() {
    @OptIn(ExperimentalCoroutinesApi::class)
    protected val dispatcher = UnconfinedTestDispatcher()

    @OptIn(ExperimentalTestApi::class)
    @get:Rule
    val composeTestRule = createComposeRule(effectContext = dispatcher)

    /**
     * instance of [OnBackPressedDispatcher] made available if testing using [setContent].
     */
    var backDispatcher: OnBackPressedDispatcher? = null
        private set

    /**
     * Helper for testing a basic Composable function that only requires a [Composable]. The
     * [AppTheme] is overridable and the [backDispatcher] is configured automatically.
     */
    protected fun setContent(
        theme: AppTheme = AppTheme.DEFAULT,
        test: @Composable () -> Unit,
    ) {
        composeTestRule.setContent {
            backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
            BitwardenTheme(
                theme = theme,
                content = test,
            )
        }
    }
}
