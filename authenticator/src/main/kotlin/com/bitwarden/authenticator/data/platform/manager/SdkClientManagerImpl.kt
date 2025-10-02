package com.bitwarden.authenticator.data.platform.manager

import com.bitwarden.core.ClientManagedTokens
import com.bitwarden.sdk.Client

/**
 * Primary implementation of [SdkClientManager].
 */
class SdkClientManagerImpl(
    private val clientProvider: suspend () -> Client = {
        Client(
            tokenProvider = Token(),
            settings = null,
        )
    },
) : SdkClientManager {
    private var client: Client? = null

    override suspend fun getOrCreateClient(): Client = client ?: clientProvider.invoke()

    override fun destroyClient() {
        client = null
    }

    /**
     * The token provider to pass to the SDK.
     */
    private class Token : ClientManagedTokens {
        override suspend fun getAccessToken(): String? {
            return null
        }
    }
}
