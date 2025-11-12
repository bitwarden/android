package com.bitwarden.authenticator.ui.platform.components.biometrics

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import com.bitwarden.authenticator.ui.platform.manager.biometrics.BiometricsManager
import com.bitwarden.ui.platform.base.util.LifecycleEventEffect

/**
 * Tracks changes in biometric support and notifies when the app resumes.
 *
 * This composable monitors lifecycle events and checks biometric support status
 * whenever the app returns to the foreground (ON_RESUME), useful for detecting
 * when biometric settings change while the app is backgrounded.
 *
 * @param biometricsManager Manager to check current biometric support status.
 * @param onBiometricSupportChange Callback invoked with the current biometric
 * support status when the app resumes.
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
