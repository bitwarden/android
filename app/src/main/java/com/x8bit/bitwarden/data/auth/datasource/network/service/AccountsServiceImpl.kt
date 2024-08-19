package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.AuthenticatedAccountsApi
import com.x8bit.bitwarden.data.auth.datasource.network.api.AuthenticatedKeyConnectorApi
import com.x8bit.bitwarden.data.auth.datasource.network.api.UnauthenticatedAccountsApi
import com.x8bit.bitwarden.data.auth.datasource.network.model.CreateAccountKeysRequest
import com.x8bit.bitwarden.data.auth.datasource.network.model.DeleteAccountRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.DeleteAccountResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KeyConnectorKeyRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KeyConnectorMasterKeyRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KeyConnectorMasterKeyResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.PasswordHintRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.PasswordHintResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.ResendEmailRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.ResetPasswordRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.SetPasswordRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.VerifyOtpRequestJson
import com.x8bit.bitwarden.data.platform.datasource.network.model.toBitwardenError
import com.x8bit.bitwarden.data.platform.datasource.network.util.parseErrorBodyOrNull
import kotlinx.serialization.json.Json

/**
 * The default implementation of the [AccountsService].
 */
@Suppress("TooManyFunctions")
class AccountsServiceImpl(
    private val unauthenticatedAccountsApi: UnauthenticatedAccountsApi,
    private val authenticatedAccountsApi: AuthenticatedAccountsApi,
    private val authenticatedKeyConnectorApi: AuthenticatedKeyConnectorApi,
    private val json: Json,
) : AccountsService {

    /**
     * Converts the currently active account to a key-connector account.
     */
    override suspend fun convertToKeyConnector(): Result<Unit> =
        authenticatedAccountsApi.convertToKeyConnector()

    override suspend fun createAccountKeys(
        publicKey: String,
        encryptedPrivateKey: String,
    ): Result<Unit> =
        authenticatedAccountsApi.createAccountKeys(
            body = CreateAccountKeysRequest(
                publicKey = publicKey,
                encryptedPrivateKey = encryptedPrivateKey,
            ),
        )

    override suspend fun deleteAccount(
        masterPasswordHash: String?,
        oneTimePassword: String?,
    ): Result<DeleteAccountResponseJson> =
        authenticatedAccountsApi
            .deleteAccount(
                DeleteAccountRequestJson(
                    masterPasswordHash = masterPasswordHash,
                    oneTimePassword = oneTimePassword,
                ),
            )
            .map {
                DeleteAccountResponseJson.Success
            }
            .recoverCatching { throwable ->
                throwable
                    .toBitwardenError()
                    .parseErrorBodyOrNull<DeleteAccountResponseJson.Invalid>(
                        code = 400,
                        json = json,
                    )
                    ?: throw throwable
            }

    override suspend fun requestOneTimePasscode(): Result<Unit> =
        authenticatedAccountsApi.requestOtp()

    override suspend fun verifyOneTimePasscode(passcode: String): Result<Unit> =
        authenticatedAccountsApi.verifyOtp(
            VerifyOtpRequestJson(
                oneTimePasscode = passcode,
            ),
        )

    override suspend fun requestPasswordHint(
        email: String,
    ): Result<PasswordHintResponseJson> =
        unauthenticatedAccountsApi
            .passwordHintRequest(PasswordHintRequestJson(email))
            .map { PasswordHintResponseJson.Success }
            .recoverCatching { throwable ->
                throwable
                    .toBitwardenError()
                    .parseErrorBodyOrNull<PasswordHintResponseJson.Error>(
                        code = 429,
                        json = json,
                    )
                    ?: throw throwable
            }

    override suspend fun resendVerificationCodeEmail(body: ResendEmailRequestJson): Result<Unit> =
        unauthenticatedAccountsApi.resendVerificationCodeEmail(body = body)

    override suspend fun resetPassword(body: ResetPasswordRequestJson): Result<Unit> {
        return if (body.currentPasswordHash == null) {
            authenticatedAccountsApi.resetTempPassword(body = body)
        } else {
            authenticatedAccountsApi.resetPassword(body = body)
        }
    }

    override suspend fun setKeyConnectorKey(
        body: KeyConnectorKeyRequestJson,
    ): Result<Unit> = authenticatedAccountsApi.setKeyConnectorKey(body)

    override suspend fun setPassword(
        body: SetPasswordRequestJson,
    ): Result<Unit> = authenticatedAccountsApi.setPassword(body)

    override suspend fun getMasterKeyFromKeyConnector(
        url: String,
    ): Result<KeyConnectorMasterKeyResponseJson> =
        authenticatedKeyConnectorApi.getMasterKeyFromKeyConnector(url = "$url/user-keys")

    override suspend fun storeMasterKeyToKeyConnector(
        url: String,
        masterKey: String,
    ): Result<Unit> =
        authenticatedKeyConnectorApi.storeMasterKeyToKeyConnector(
            url = "$url/user-keys",
            body = KeyConnectorMasterKeyRequestJson(masterKey = masterKey),
        )
}
