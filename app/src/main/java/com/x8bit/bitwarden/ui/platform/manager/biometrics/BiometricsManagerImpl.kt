package com.x8bit.bitwarden.ui.platform.manager.biometrics

import android.app.Activity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage

/**
 * Default implementation of the [BiometricsManager] to manage biometrics within the app.
 */
@OmitFromCoverage
class BiometricsManagerImpl(
    private val activity: Activity,
) : BiometricsManager {
    private val biometricManager: BiometricManager = BiometricManager.from(activity)

    override val isBiometricsSupported: Boolean
        get() = when (biometricManager.canAuthenticate(Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN,
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED,
            -> false

            else -> false
        }
}
