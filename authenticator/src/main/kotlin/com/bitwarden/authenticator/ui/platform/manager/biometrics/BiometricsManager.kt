package com.bitwarden.authenticator.ui.platform.manager.biometrics

import androidx.compose.runtime.Immutable
import javax.crypto.Cipher

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
        onSuccess: (Cipher) -> Unit,
        onCancel: () -> Unit,
        onLockOut: () -> Unit,
        onError: () -> Unit,
        cipher: Cipher,
    )
}
