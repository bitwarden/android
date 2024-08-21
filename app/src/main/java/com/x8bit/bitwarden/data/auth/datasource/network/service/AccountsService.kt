package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.model.DeleteAccountResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KeyConnectorKeyRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KeyConnectorMasterKeyResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.PasswordHintResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.ResendEmailRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.ResetPasswordRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.SetPasswordRequestJson

/**
 * Provides an API for querying accounts endpoints.
 */
@Suppress("TooManyFunctions")
interface AccountsService {

    /**
     * Converts the currently active account to a key-connector account.
     */
    suspend fun convertToKeyConnector(): Result<Unit>

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
     * Set the key connector key.
     *
     * This API requires the [accessToken] to be passed in manually because it occurs during the
     * login process.
     */
    suspend fun setKeyConnectorKey(
        accessToken: String,
        body: KeyConnectorKeyRequestJson,
    ): Result<Unit>

    /**
     * Set the password.
     */
    suspend fun setPassword(body: SetPasswordRequestJson): Result<Unit>

    /**
     * Retrieves the master key from the key connector.
     *
     * This API requires the [accessToken] to be passed in manually because it occurs during the
     * login process.
     */
    suspend fun getMasterKeyFromKeyConnector(
        url: String,
        accessToken: String,
    ): Result<KeyConnectorMasterKeyResponseJson>

    /**
     * Stores the master key to the key connector.
     */
    suspend fun storeMasterKeyToKeyConnector(
        url: String,
        masterKey: String,
    ): Result<Unit>

    /**
     * Stores the master key to the key connector.
     *
     * This API requires the [accessToken] to be passed in manually because it occurs during the
     * login process.
     */
    suspend fun storeMasterKeyToKeyConnector(
        url: String,
        accessToken: String,
        masterKey: String,
    ): Result<Unit>
}
