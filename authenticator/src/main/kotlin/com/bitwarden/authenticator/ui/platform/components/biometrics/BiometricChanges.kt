package com.bitwarden.authenticator.ui.platform.components.biometrics

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import com.bitwarden.authenticator.ui.platform.manager.biometrics.BiometricsManager
import com.bitwarden.ui.platform.base.util.LifecycleEventEffect

/**
 * Tracks changes in biometric support.
 */
@Composable
fun BiometricChanges(
    biometricsManager: BiometricsManager,
    onBiometricSupportChange: (isSupported: Boolean) -> Unit,
) {
    LifecycleEventEffect { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                onBiometricSupportChange(biometricsManager.isBiometricsSupported)
            }

            else -> Unit
        }
    }
}
