package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.model.DeleteAccountResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.PasswordHintResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.ResendEmailRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.ResetPasswordRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.SetPasswordRequestJson

/**
 * Provides an API for querying accounts endpoints.
 */
interface AccountsService {

    /**
     * Creates a new account's keys.
     */
    suspend fun createAccountKeys(publicKey: String, encryptedPrivateKey: String): Result<Unit>

    /**
     * Make delete account request.
     */
    suspend fun deleteAccount(
        masterPasswordHash: String?,
        oneTimePassword: String?,
    ): Result<DeleteAccountResponseJson>

    /**
     * Request a one-time passcode that is sent to the user's email.
     */
    suspend fun requestOneTimePasscode(): Result<Unit>

    /**
     * Verify that the provided [passcode] is correct.
     */
    suspend fun verifyOneTimePasscode(passcode: String): Result<Unit>

    /**
     * Request a password hint.
     */
    suspend fun requestPasswordHint(email: String): Result<PasswordHintResponseJson>

    /**
     * Resend the email with the two-factor verification code.
     */
    suspend fun resendVerificationCodeEmail(body: ResendEmailRequestJson): Result<Unit>

    /**
     * Reset the password.
     */
    suspend fun resetPassword(body: ResetPasswordRequestJson): Result<Unit>

    /**
     * Set the password.
     */
    suspend fun setPassword(body: SetPasswordRequestJson): Result<Unit>
}
