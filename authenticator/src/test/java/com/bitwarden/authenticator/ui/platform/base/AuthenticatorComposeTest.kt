package com.bitwarden.authenticator.ui.platform.base

import androidx.compose.runtime.Composable
import com.bitwarden.authenticator.ui.platform.composition.LocalManagerProvider
import com.bitwarden.authenticator.ui.platform.manager.biometrics.BiometricsManager
import com.bitwarden.authenticator.ui.platform.manager.exit.ExitManager
import com.bitwarden.authenticator.ui.platform.manager.intent.IntentManager
import com.bitwarden.authenticator.ui.platform.manager.permissions.PermissionsManager
import com.bitwarden.authenticator.ui.platform.theme.AuthenticatorTheme
import com.bitwarden.ui.platform.base.BaseComposeTest
import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import io.mockk.mockk

/**
 * A base class that can be used for performing Compose-layer testing using Robolectric, Compose
 * Testing, and JUnit 4.
 */
abstract class AuthenticatorComposeTest : BaseComposeTest() {

    /**
     * Helper for testing a basic Composable function that only requires a Composable environment
     * with the [AuthenticatorTheme].
     */
    @Suppress("LongParameterList")
    protected fun setContent(
        theme: AppTheme = AppTheme.DEFAULT,
        permissionsManager: PermissionsManager = mockk(),
        intentManager: IntentManager = mockk(),
        exitManager: ExitManager = mockk(),
        biometricsManager: BiometricsManager = mockk(),
        test: @Composable () -> Unit,
    ) {
        setTestContent {
            AuthenticatorTheme(theme = theme) {
                LocalManagerProvider(
                    permissionsManager = permissionsManager,
                    intentManager = intentManager,
                    exitManager = exitManager,
                    biometricsManager = biometricsManager,
                    content = test,
                )
            }
        }
    }
}
