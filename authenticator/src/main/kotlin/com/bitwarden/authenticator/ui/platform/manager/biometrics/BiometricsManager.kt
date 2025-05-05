package com.bitwarden.authenticator.ui.platform.manager.biometrics

import androidx.compose.runtime.Immutable

/**
 * Interface to manage biometrics within the app.
 */
@Immutable
interface BiometricsManager {
    /**
     * Returns `true` if the device supports string biometric authentication, `false` otherwise.
     */
    val isBiometricsSupported: Boolean

    /**
     * Display a prompt for biometrics.
     */
    fun promptBiometrics(
        onSuccess: () -> Unit,
        onCancel: () -> Unit,
        onLockOut: () -> Unit,
        onError: () -> Unit,
    )
}
