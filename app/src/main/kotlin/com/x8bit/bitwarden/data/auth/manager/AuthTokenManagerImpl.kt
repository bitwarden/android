package com.x8bit.bitwarden.data.auth.manager

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource

/**
 * Default implementation of [AuthTokenManager].
 */
class AuthTokenManagerImpl(
    private val authDiskSource: AuthDiskSource,
) : AuthTokenManager {

    override fun getActiveAccessTokenOrNull(): String? = authDiskSource
        .userState
        ?.activeUserId
        ?.let { authDiskSource.getAccountTokens(it) }
        ?.accessToken
}
