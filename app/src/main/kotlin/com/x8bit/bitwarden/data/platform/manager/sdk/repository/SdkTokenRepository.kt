package com.x8bit.bitwarden.data.platform.manager.sdk.repository

import com.bitwarden.core.ClientManagedTokens
import com.bitwarden.network.provider.TokenProvider

/**
 * A user-scoped implementation of a Bitwarden SDK [ClientManagedTokens].
 */
class SdkTokenRepository(
    private val userId: String?,
    private val tokenProvider: TokenProvider,
) : ClientManagedTokens {
    override suspend fun getAccessToken(): String? =
        userId?.let { tokenProvider.getAccessToken(userId = it) }
}
