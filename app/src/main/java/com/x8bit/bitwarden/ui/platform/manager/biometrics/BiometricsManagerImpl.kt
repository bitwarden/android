package com.x8bit.bitwarden.ui.platform.manager.biometrics

import android.app.Activity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import javax.crypto.Cipher

/**
 * Default implementation of the [BiometricsManager] to manage biometrics within the app.
 */
@OmitFromCoverage
class BiometricsManagerImpl(
    private val activity: Activity,
) : BiometricsManager {
    private val biometricManager: BiometricManager = BiometricManager.from(activity)

    private val fragmentActivity: FragmentActivity get() = activity as FragmentActivity

    override val isBiometricsSupported: Boolean
        get() = biometricSupportStatus == BiometricSupportStatus.CLASS_3_SUPPORTED

    override val isUserVerificationSupported: Boolean
        get() = canAuthenticate(
            authenticators = Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL,
        )

    override val biometricSupportStatus: BiometricSupportStatus
        get() = when {
            canAuthenticate(Authenticators.BIOMETRIC_STRONG) -> {
                BiometricSupportStatus.CLASS_3_SUPPORTED
            }

            canAuthenticate(Authenticators.BIOMETRIC_WEAK) -> {
                BiometricSupportStatus.CLASS_2_SUPPORTED
            }

            else -> BiometricSupportStatus.NOT_SUPPORTED
        }

    override fun promptBiometrics(
        onSuccess: (cipher: Cipher?) -> Unit,
        onCancel: () -> Unit,
        onLockOut: () -> Unit,
        onError: () -> Unit,
        cipher: Cipher,
    ) {
        configureAndDisplayPrompt(
            onSuccess = onSuccess,
            onCancel = onCancel,
            onLockOut = onLockOut,
            onError = onError,
            cipher = cipher,
        )
    }

    override fun promptUserVerification(
        onSuccess: () -> Unit,
        onCancel: () -> Unit,
        onLockOut: () -> Unit,
        onError: () -> Unit,
        onNotSupported: () -> Unit,
    ) {
        if (isUserVerificationSupported.not()) {
            onNotSupported()
        } else {
            configureAndDisplayPrompt(
                onSuccess = { onSuccess() },
                onCancel = onCancel,
                onLockOut = onLockOut,
                onError = onError,
                cipher = null,
            )
        }
    }

    private fun canAuthenticate(authenticators: Int): Boolean =
        when (biometricManager.canAuthenticate(authenticators)) {
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

    private fun configureAndDisplayPrompt(
        onSuccess: (Cipher?) -> Unit,
        onCancel: () -> Unit,
        onLockOut: () -> Unit,
        onError: () -> Unit,
        cipher: Cipher?,
    ) {
        val biometricPrompt = BiometricPrompt(
            fragmentActivity,
            ContextCompat.getMainExecutor(fragmentActivity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult,
                ) = onSuccess(result.cryptoObject?.cipher)

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

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.bitwarden))
            .setAllowedAuthenticators(Authenticators.BIOMETRIC_STRONG)

        cipher
            ?.let {
                promptInfoBuilder
                    .setDescription(activity.getString(R.string.biometrics_direction))
                    .setAllowedAuthenticators(Authenticators.BIOMETRIC_STRONG)
                    .setNegativeButtonText(activity.getString(R.string.cancel))
                    .setConfirmationRequired(false)
                biometricPrompt.authenticate(
                    promptInfoBuilder.build(),
                    BiometricPrompt.CryptoObject(it),
                )
            }
            ?: run {
                promptInfoBuilder
                    .setDescription(activity.getString(R.string.user_verification_direction))
                    .setAllowedAuthenticators(
                        Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL,
                    )
                    .setConfirmationRequired(false)
                biometricPrompt.authenticate(promptInfoBuilder.build())
            }
    }
}
