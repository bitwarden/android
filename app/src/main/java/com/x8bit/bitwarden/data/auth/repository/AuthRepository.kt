package com.x8bit.bitwarden.data.auth.repository

import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthState
import com.x8bit.bitwarden.data.auth.datasource.network.model.LoginResult
import kotlinx.coroutines.flow.StateFlow

/**
 * Provides an API for observing an modifying authentication state.
 */
interface AuthRepository {
    /**
     * Models the current auth state.
     */
    val authStateFlow: StateFlow<AuthState>

    /**
     * Attempt to login with the given email and password. Updated access token will be reflected
     * in [authStateFlow].
     */
    suspend fun login(
        email: String,
        password: String,
    ): LoginResult
}
