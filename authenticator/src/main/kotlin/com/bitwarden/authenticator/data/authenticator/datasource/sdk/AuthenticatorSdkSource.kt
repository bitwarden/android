package com.bitwarden.authenticator.data.authenticator.datasource.sdk

import com.bitwarden.vault.TotpResponse
import java.time.Instant

/**
 * Source of authenticator information from the Bitwarden SDK.
 */
interface AuthenticatorSdkSource {

    /**
     * Generate a verification code and the period using the totp code.
     */
    suspend fun generateTotp(
        totp: String,
        time: Instant,
    ): Result<TotpResponse>

    /**
     * Generate a random key for seeding biometrics.
     */
    suspend fun generateBiometricsKey(): Result<String>

    /**
     * Scores the given [password], returning a strength level in the range `[0, 4]`.
     */
    suspend fun passwordStrength(password: String): Result<UByte>
}
