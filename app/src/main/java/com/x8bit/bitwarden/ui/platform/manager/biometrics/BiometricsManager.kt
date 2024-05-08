package com.x8bit.bitwarden.ui.platform.manager.biometrics

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
     * Display a prompt for setting up or verifying biometrics.
     */
    fun promptBiometrics(
        onSuccess: (cipher: Cipher?) -> Unit,
        onCancel: () -> Unit,
        onLockOut: () -> Unit,
        onError: () -> Unit,
        cipher: Cipher,
    )
}
