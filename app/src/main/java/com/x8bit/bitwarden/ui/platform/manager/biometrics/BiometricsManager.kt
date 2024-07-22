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
     * Returns `true` if the device supports performing user verification with biometrics or device
     * credentials, `false` otherwise.
     */
    val isUserVerificationSupported: Boolean

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

    /**
     * Display a prompt for performing user verification with biometrics or device credentials.
     *
     * @param onSuccess Indicates the user was successfully verified.
     * @param onCancel Indicates the user cancelled verification.
     * @param onLockOut Indicates there were too many failed verification attempts and must wait
     * some time before attempting verification again.
     * @param onError Indicates the user was not verified due to an unexpected error.
     * @param onNotSupported Indicates the users device is not capable of performing user
     * verification.
     */
    fun promptUserVerification(
        onSuccess: () -> Unit,
        onCancel: () -> Unit,
        onLockOut: () -> Unit,
        onError: () -> Unit,
        onNotSupported: () -> Unit,
    )
}
