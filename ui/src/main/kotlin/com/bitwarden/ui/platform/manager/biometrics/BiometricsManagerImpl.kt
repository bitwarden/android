package com.bitwarden.ui.platform.manager.biometrics

import android.app.Activity
import androidx.annotation.StringRes
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.core.data.manager.biometrics.BiometricsSupportManager
import com.bitwarden.core.data.manager.biometrics.model.BiometricSupportStatus
import com.bitwarden.ui.platform.manager.biometrics.model.BiometricsConfig
import com.bitwarden.ui.platform.resource.BitwardenString
import javax.crypto.Cipher

/**
 * Default implementation of the [BiometricsManager] to manage biometrics within the app.
 *
 * @param activity The activity used to display the biometric prompts.
 * @param config The app-specific configuration for the prompts and supported authenticators.
 * @param biometricsSupportManager Handles the biometric capability checks.
 */
@OmitFromCoverage
internal class BiometricsManagerImpl(
    private val activity: Activity,
    private val config: BiometricsConfig,
    private val biometricsSupportManager: BiometricsSupportManager,
) : BiometricsManager {
    private val fragmentActivity: FragmentActivity get() = activity as FragmentActivity

    /**
     * The authenticators allowed when prompting for biometrics.
     */
    private val biometricsAuthenticators: Int
        get() = biometricsSupportManager.getBiometricsAuthenticators(
            isDeviceCredentialAllowed = config.isDeviceCredentialAllowedForBiometrics,
        )

    override val isBiometricsSupported: Boolean
        get() = biometricsSupportManager.isBiometricsSupported(
            isDeviceCredentialAllowed = config.isDeviceCredentialAllowedForBiometrics,
        )

    override val isUserVerificationSupported: Boolean
        get() = biometricsSupportManager.isUserVerificationSupported

    override val biometricSupportStatus: BiometricSupportStatus
        get() = biometricsSupportManager.biometricSupportStatus

    override fun promptBiometrics(
        onSuccess: (cipher: Cipher) -> Unit,
        onCancel: () -> Unit,
        onLockOut: () -> Unit,
        onError: () -> Unit,
        cipher: Cipher,
    ) {
        configureAndDisplayPrompt(
            onSuccess = { it?.let(block = onSuccess) ?: onError() },
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
                ): Unit = onSuccess(result.cryptoObject?.cipher)

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
            .setTitle(getString(resId = config.promptTitleRes))
            .setConfirmationRequired(false)

        cipher
            ?.let {
                promptInfoBuilder
                    .setDescription(getString(resId = config.biometricsPromptDescriptionRes))
                    .setAllowedAuthenticators(biometricsAuthenticators)
                    .apply {
                        if (!config.isDeviceCredentialAllowedForBiometrics) {
                            // Negative text is not allowed for device credential authentication.
                            setNegativeButtonText(getString(resId = BitwardenString.cancel))
                        }
                    }
                biometricPrompt.authenticate(
                    promptInfoBuilder.build(),
                    BiometricPrompt.CryptoObject(it),
                )
            }
            ?: run {
                promptInfoBuilder
                    .setDescription(getString(resId = BitwardenString.user_verification_direction))
                    .setAllowedAuthenticators(
                        Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL,
                    )
                biometricPrompt.authenticate(promptInfoBuilder.build())
            }
    }

    private fun getString(@StringRes resId: Int): String = activity.getString(resId)
}
