package com.x8bit.bitwarden.ui.platform.manager.biometrics

/**
 * Interface to manage biometrics within the app.
 */
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
