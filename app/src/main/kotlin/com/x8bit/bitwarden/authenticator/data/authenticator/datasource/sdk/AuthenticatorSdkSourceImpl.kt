package com.x8bit.bitwarden.authenticator.data.authenticator.datasource.sdk

import com.bitwarden.core.DateTime
import com.bitwarden.core.TotpResponse
import com.bitwarden.sdk.Client
import com.x8bit.bitwarden.authenticator.data.platform.manager.SdkClientManager
import javax.inject.Inject

class AuthenticatorSdkSourceImpl @Inject constructor(
    private val sdkClientManager: SdkClientManager,
) : AuthenticatorSdkSource {

    override suspend fun generateTotp(
        totp: String,
        time: DateTime,
    ): Result<TotpResponse> = runCatching {
        getClient()
            .vault()
            .generateTotp(
                key = totp,
                time = time,
            )
    }

    private suspend fun getClient(): Client = sdkClientManager.getOrCreateClient()

}
