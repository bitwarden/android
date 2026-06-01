package com.bitwarden.authenticator.ui.platform.base

import androidx.compose.runtime.Composable
import com.bitwarden.authenticator.ui.platform.composition.LocalManagerProvider
import com.bitwarden.authenticator.ui.platform.manager.biometrics.BiometricsManager
import com.bitwarden.authenticator.ui.platform.manager.permissions.PermissionsManager
import com.bitwarden.ui.platform.base.BaseComposeTest
import com.bitwarden.ui.platform.feature.qrcodescan.util.QrCodeAnalyzer
import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.manager.exit.ExitManager
import com.bitwarden.ui.platform.theme.BitwardenTheme
import io.mockk.mockk

/**
 * A base class that can be used for performing Compose-layer testing using Robolectric, Compose
 * Testing, and JUnit 4.
 */
abstract class AuthenticatorComposeTest : BaseComposeTest() {

    /**
     * Helper for testing a basic Composable function that only requires a Composable environment
     * with the [BitwardenTheme].
     */
    @Suppress("LongParameterList")
    protected fun setContent(
        theme: AppTheme = AppTheme.DEFAULT,
        permissionsManager: PermissionsManager = mockk(),
        intentManager: IntentManager = mockk(),
        exitManager: ExitManager = mockk(),
        biometricsManager: BiometricsManager = mockk(),
        qrCodeAnalyzer: QrCodeAnalyzer = mockk(),
        test: @Composable () -> Unit,
    ) {
        setTestContent {
            BitwardenTheme(theme = theme) {
                LocalManagerProvider(
                    permissionsManager = permissionsManager,
                    intentManager = intentManager,
                    exitManager = exitManager,
                    biometricsManager = biometricsManager,
                    qrCodeAnalyzer = qrCodeAnalyzer,
                    content = test,
                )
            }
        }
    }
}
