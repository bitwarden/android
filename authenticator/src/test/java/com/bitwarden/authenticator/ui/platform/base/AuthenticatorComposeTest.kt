package com.bitwarden.authenticator.ui.platform.base

import androidx.compose.runtime.Composable
import com.bitwarden.authenticator.ui.platform.composition.LocalManagerProvider
import com.bitwarden.authenticator.ui.platform.theme.AuthenticatorTheme
import com.bitwarden.ui.platform.base.BaseComposeTest
import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme

/**
 * A base class that can be used for performing Compose-layer testing using Robolectric, Compose
 * Testing, and JUnit 4.
 */
abstract class AuthenticatorComposeTest : BaseComposeTest() {

    /**
     * Helper for testing a basic Composable function that only requires a Composable environment
     * with the [AuthenticatorTheme].
     */
    protected fun setContent(
        theme: AppTheme = AppTheme.DEFAULT,
        test: @Composable () -> Unit,
    ) {
        setTestContent {
            AuthenticatorTheme(theme = theme) {
                LocalManagerProvider { test() }
            }
        }
    }
}
