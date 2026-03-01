package com.x8bit.bitwarden.data.platform.manager.sdk.repository

import com.bitwarden.core.ClientManagedTokens
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource

/**
 * A user-scoped implementation of a Bitwarden SDK [ClientManagedTokens].
 *
 * Note: This intentionally provides the raw stored token without proactive expiration checks
 * or refresh logic. The SDK handles automatic token refresh internally.
 */
class SdkTokenRepository(
    private val userId: String?,
    private val authDiskSource: AuthDiskSource,
) : ClientManagedTokens {
    override suspend fun getAccessToken(): String? =
        userId?.let {
            authDiskSource.getAccountTokens(userId = it)?.accessToken
        }
}
