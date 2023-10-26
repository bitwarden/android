package com.x8bit.bitwarden.data.auth.repository

import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength
import com.x8bit.bitwarden.data.auth.repository.model.AuthState
import com.x8bit.bitwarden.data.auth.repository.model.LoginResult
import com.x8bit.bitwarden.data.auth.repository.model.RegisterResult
import com.x8bit.bitwarden.data.auth.repository.util.CaptchaCallbackTokenResult
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
     * Log out the current user.
     */
    fun logout()

    /**
     * Attempt to register a new account with the given parameters.
     */
    suspend fun register(
        email: String,
        masterPassword: String,
        masterPasswordHint: String?,
        captchaToken: String?,
        shouldCheckDataBreaches: Boolean,
    ): RegisterResult

    /**
     * Set the value of [captchaTokenResultFlow].
     */
    fun setCaptchaCallbackTokenResult(tokenResult: CaptchaCallbackTokenResult)

    /**
     * Get the password strength for the given [email] and [password] combo.
     */
    suspend fun getPasswordStrength(email: String, password: String): Result<PasswordStrength>
}
