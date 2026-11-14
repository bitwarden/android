package com.bitwarden.core.data.manager.biometrics

import android.content.Context
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.core.data.manager.biometrics.model.BiometricSupportStatus

/**
 * Manages the biometric capability checks that do not require an activity.
 */
interface BiometricsSupportManager {
    /**
     * Returns `true` if the device supports performing user verification with biometrics or device
     * credentials, `false` otherwise.
     */
    val isUserVerificationSupported: Boolean

    /**
     * Returns the status of biometric support available on the device.
     */
    val biometricSupportStatus: BiometricSupportStatus

    /**
     * Returns the authenticators that are allowed when prompting for biometrics.
     *
     * @param isDeviceCredentialAllowed Whether device credentials (PIN, pattern, or password) may
     * be used in place of biometrics.
     */
    fun getBiometricsAuthenticators(isDeviceCredentialAllowed: Boolean): Int

    /**
     * Returns `true` if the device supports the authenticators used when prompting for biometrics,
     * `false` otherwise.
     *
     * @param isDeviceCredentialAllowed Whether device credentials (PIN, pattern, or password) may
     * be used in place of biometrics.
     */
    fun isBiometricsSupported(isDeviceCredentialAllowed: Boolean): Boolean

    @Suppress("UndocumentedPublicClass")
    @OmitFromCoverage
    companion object {
        /**
         * Creates a new [BiometricsSupportManager] instance.
         */
        fun create(
            context: Context,
        ): BiometricsSupportManager = BiometricsSupportManagerImpl(
            context = context,
        )
    }
}
