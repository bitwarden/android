package com.bitwarden.authenticator.data.authenticator.datasource.sdk

import com.bitwarden.core.DateTime
import com.bitwarden.vault.TotpResponse

/**
 * Source of authenticator information from the Bitwarden SDK.
 */
interface AuthenticatorSdkSource {

    /**
     * Generate a verification code and the period using the totp code.
     */
    suspend fun generateTotp(
        totp: String,
        time: DateTime,
    ): Result<TotpResponse>

    /**
     * Generate a random key for seeding biometrics.
     */
    suspend fun generateBiometricsKey(): Result<String>
}
