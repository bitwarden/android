package com.x8bit.bitwarden.data.auth.repository

import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthState
import com.x8bit.bitwarden.data.auth.datasource.network.model.LoginResult
import com.x8bit.bitwarden.data.auth.datasource.network.util.CaptchaCallbackTokenResult
import kotlinx.coroutines.flow.Flow
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
     * Flow of the current [CaptchaCallbackTokenResult]. Subscribers should listen to the flow
     * in order to receive updates whenever [setCaptchaCallbackTokenResult] is called.
     */
    val captchaTokenResultFlow: Flow<CaptchaCallbackTokenResult>

    /**
     * The currently persisted saved email address (or `null` if not set).
     */
    var rememberedEmailAddress: String?

    /**
     * Attempt to login with the given email and password. Updated access token will be reflected
     * in [authStateFlow].
     */
    suspend fun login(
        email: String,
        password: String,
        captchaToken: String?,
    ): LoginResult

    /**
     * Set the value of [captchaTokenResultFlow].
     */
    fun setCaptchaCallbackTokenResult(tokenResult: CaptchaCallbackTokenResult)
}
