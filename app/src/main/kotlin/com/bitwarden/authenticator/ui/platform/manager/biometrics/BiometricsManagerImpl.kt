package com.bitwarden.authenticator.ui.platform.manager.biometrics

import android.app.Activity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.bitwarden.authenticator.R

/**
 * Default implementation of the [BiometricsManager] to manage biometrics within the app.
 */
class BiometricsManagerImpl(
    private val activity: Activity,
) : BiometricsManager {
    private val biometricManager: BiometricManager = BiometricManager.from(activity)

    private val fragmentActivity: FragmentActivity get() = activity as FragmentActivity

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

    override fun promptBiometrics(
        onSuccess: () -> Unit,
        onCancel: () -> Unit,
        onLockOut: () -> Unit,
        onError: () -> Unit,
    ) {
        val biometricPrompt = BiometricPrompt(
            fragmentActivity,
            ContextCompat.getMainExecutor(fragmentActivity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult,
                ) = onSuccess()

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    when (errorCode) {
                        BiometricPrompt.ERROR_HW_UNAVAILABLE,
                        BiometricPrompt.ERROR_UNABLE_TO_PROCESS,
                        BiometricPrompt.ERROR_TIMEOUT,
                        BiometricPrompt.ERROR_NO_SPACE,
                        BiometricPrompt.ERROR_CANCELED,
                        BiometricPrompt.ERROR_VENDOR,
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NO_BIOMETRICS,
                        BiometricPrompt.ERROR_HW_NOT_PRESENT,
                        BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
                        -> onError()

                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> onCancel()

                        BiometricPrompt.ERROR_LOCKOUT,
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT,
                        -> onLockOut()
                    }
                }

                override fun onAuthenticationFailed() {
                    // Just keep on keepin' on, if there is a real issue it
                    // will come from the onAuthenticationError callback.
                }
            },
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.bitwarden_authenticator))
            .setDescription(activity.getString(R.string.biometrics_direction))
            .setNegativeButtonText(activity.getString(R.string.cancel))
            .setAllowedAuthenticators(Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
