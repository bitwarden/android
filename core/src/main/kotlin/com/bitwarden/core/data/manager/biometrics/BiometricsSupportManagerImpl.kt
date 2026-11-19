package com.bitwarden.core.data.manager.biometrics

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.core.data.manager.biometrics.model.BiometricSupportStatus
import com.bitwarden.core.util.isBuildVersionAtLeast

/**
 * Default implementation of the [BiometricsSupportManager].
 *
 * @param context The context used to query the device's biometric capabilities.
 */
@OmitFromCoverage
internal class BiometricsSupportManagerImpl(
    context: Context,
) : BiometricsSupportManager {
    private val biometricManager: BiometricManager = BiometricManager.from(context)

    override val isUserVerificationSupported: Boolean
        get() = canAuthenticate(
            authenticators = Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL,
        )

    override val biometricSupportStatus: BiometricSupportStatus
        get() = when {
            canAuthenticate(authenticators = Authenticators.BIOMETRIC_STRONG) -> {
                BiometricSupportStatus.CLASS_3_SUPPORTED
            }

            canAuthenticate(authenticators = Authenticators.BIOMETRIC_WEAK) -> {
                BiometricSupportStatus.CLASS_2_SUPPORTED
            }

            else -> BiometricSupportStatus.NOT_SUPPORTED
        }

    override fun getBiometricsAuthenticators(
        isDeviceCredentialAllowed: Boolean,
    ): Int = if (isDeviceCredentialAllowed) {
        // [Authenticators.DEVICE_CREDENTIAL or Authenticators.BIOMETRIC_STRONG] is not
        // a valid combination prior to SDK version 30 so we use
        // [Authenticators.DEVICE_CREDENTIAL or Authenticators.BIOMETRIC_WEAK] instead.
        if (isBuildVersionAtLeast(version = Build.VERSION_CODES.R)) {
            Authenticators.DEVICE_CREDENTIAL or Authenticators.BIOMETRIC_STRONG
        } else {
            Authenticators.DEVICE_CREDENTIAL or Authenticators.BIOMETRIC_WEAK
        }
    } else {
        Authenticators.BIOMETRIC_STRONG
    }

    override fun isBiometricsSupported(
        isDeviceCredentialAllowed: Boolean,
    ): Boolean = canAuthenticate(
        authenticators = getBiometricsAuthenticators(
            isDeviceCredentialAllowed = isDeviceCredentialAllowed,
        ),
    )

    private fun canAuthenticate(
        authenticators: Int,
    ): Boolean = when (biometricManager.canAuthenticate(authenticators)) {
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
