package com.x8bit.bitwarden.authenticator.data.authenticator.datasource.sdk

import com.bitwarden.core.DateTime
import com.bitwarden.core.TotpResponse

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
}
