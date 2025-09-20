package com.bitwarden.network.service

import com.bitwarden.network.api.AuthenticatedAccountsApi
import com.bitwarden.network.api.AuthenticatedKeyConnectorApi
import com.bitwarden.network.api.UnauthenticatedAccountsApi
import com.bitwarden.network.api.UnauthenticatedKeyConnectorApi
import com.bitwarden.network.model.CreateAccountKeysRequest
import com.bitwarden.network.model.DeleteAccountRequestJson
import com.bitwarden.network.model.DeleteAccountResponseJson
import com.bitwarden.network.model.KeyConnectorKeyRequestJson
import com.bitwarden.network.model.KeyConnectorMasterKeyRequestJson
import com.bitwarden.network.model.KeyConnectorMasterKeyResponseJson
import com.bitwarden.network.model.PasswordHintRequestJson
import com.bitwarden.network.model.PasswordHintResponseJson
import com.bitwarden.network.model.ResendEmailRequestJson
import com.bitwarden.network.model.ResendNewDeviceOtpRequestJson
import com.bitwarden.network.model.ResetPasswordRequestJson
import com.bitwarden.network.model.SetPasswordRequestJson
import com.bitwarden.network.model.UpdateKdfJsonRequest
import com.bitwarden.network.model.VerificationCodeResponseJson
import com.bitwarden.network.model.VerificationOtpResponseJson
import com.bitwarden.network.model.VerifyOtpRequestJson
import com.bitwarden.network.model.toBitwardenError
import com.bitwarden.network.util.HEADER_VALUE_BEARER_PREFIX
import com.bitwarden.network.util.NetworkErrorCode
import com.bitwarden.network.util.parseErrorBodyOrNull
import com.bitwarden.network.util.toResult
import kotlinx.serialization.json.Json

/**
 * The default implementation of the [AccountsService].
 */
@Suppress("TooManyFunctions")
internal class AccountsServiceImpl(
    private val unauthenticatedAccountsApi: UnauthenticatedAccountsApi,
    private val authenticatedAccountsApi: AuthenticatedAccountsApi,
    private val unauthenticatedKeyConnectorApi: UnauthenticatedKeyConnectorApi,
    private val authenticatedKeyConnectorApi: AuthenticatedKeyConnectorApi,
    private val json: Json,
) : AccountsService {

    /**
     * Converts the currently active account to a key-connector account.
     */
    override suspend fun convertToKeyConnector(): Result<Unit> =
        authenticatedAccountsApi
            .convertToKeyConnector()
            .toResult()

    override suspend fun createAccountKeys(
        publicKey: String,
        encryptedPrivateKey: String,
    ): Result<Unit> =
        authenticatedAccountsApi
            .createAccountKeys(
                body = CreateAccountKeysRequest(
                    publicKey = publicKey,
                    encryptedPrivateKey = encryptedPrivateKey,
                ),
            )
            .toResult()

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
            .toResult()
            .map { DeleteAccountResponseJson.Success }
            .recoverCatching { throwable ->
                throwable
                    .toBitwardenError()
                    .parseErrorBodyOrNull<DeleteAccountResponseJson.Invalid>(
                        code = NetworkErrorCode.BAD_REQUEST,
                        json = json,
                    )
                    ?: throw throwable
            }

    override suspend fun requestOneTimePasscode(): Result<Unit> =
        authenticatedAccountsApi
            .requestOtp()
            .toResult()

    override suspend fun verifyOneTimePasscode(passcode: String): Result<Unit> =
        authenticatedAccountsApi
            .verifyOtp(
                VerifyOtpRequestJson(
                    oneTimePasscode = passcode,
                ),
            )
            .toResult()

    override suspend fun requestPasswordHint(
        email: String,
    ): Result<PasswordHintResponseJson> =
        unauthenticatedAccountsApi
            .passwordHintRequest(PasswordHintRequestJson(email))
            .toResult()
            .map { PasswordHintResponseJson.Success }
            .recoverCatching { throwable ->
                throwable
                    .toBitwardenError()
                    .parseErrorBodyOrNull<PasswordHintResponseJson.Error>(
                        code = NetworkErrorCode.TOO_MANY_REQUESTS,
                        json = json,
                    )
                    ?: throw throwable
            }

    override suspend fun resendVerificationCodeEmail(
        body: ResendEmailRequestJson,
    ): Result<VerificationCodeResponseJson> =
        unauthenticatedAccountsApi
            .resendVerificationCodeEmail(body = body)
            .toResult()
            .map { VerificationCodeResponseJson.Success }
            .recoverCatching { throwable ->
                throwable
                    .toBitwardenError()
                    .parseErrorBodyOrNull<VerificationCodeResponseJson.Invalid>(
                        code = NetworkErrorCode.BAD_REQUEST,
                        json = json,
                    )
                    ?: throw throwable
            }

    override suspend fun resendNewDeviceOtp(
        body: ResendNewDeviceOtpRequestJson,
    ): Result<VerificationOtpResponseJson> =
        unauthenticatedAccountsApi
            .resendNewDeviceOtp(body = body)
            .toResult()
            .map { VerificationOtpResponseJson.Success }
            .recoverCatching { throwable ->
                throwable
                    .toBitwardenError()
                    .parseErrorBodyOrNull<VerificationOtpResponseJson.Invalid>(
                        code = NetworkErrorCode.BAD_REQUEST,
                        json = json,
                    )
                    ?: throw throwable
            }

    override suspend fun resetPassword(body: ResetPasswordRequestJson): Result<Unit> =
        if (body.currentPasswordHash == null) {
            authenticatedAccountsApi
                .resetTempPassword(body = body)
                .toResult()
        } else {
            authenticatedAccountsApi
                .resetPassword(body = body)
                .toResult()
        }

    override suspend fun setKeyConnectorKey(
        accessToken: String,
        body: KeyConnectorKeyRequestJson,
    ): Result<Unit> =
        unauthenticatedAccountsApi
            .setKeyConnectorKey(
                body = body,
                bearerToken = "$HEADER_VALUE_BEARER_PREFIX$accessToken",
            )
            .toResult()

    override suspend fun setPassword(
        body: SetPasswordRequestJson,
    ): Result<Unit> = authenticatedAccountsApi
        .setPassword(body)
        .toResult()

    override suspend fun getMasterKeyFromKeyConnector(
        url: String,
        accessToken: String,
    ): Result<KeyConnectorMasterKeyResponseJson> =
        unauthenticatedKeyConnectorApi
            .getMasterKeyFromKeyConnector(
                url = "$url/user-keys",
                bearerToken = "$HEADER_VALUE_BEARER_PREFIX$accessToken",
            )
            .toResult()

    override suspend fun storeMasterKeyToKeyConnector(
        url: String,
        masterKey: String,
    ): Result<Unit> =
        authenticatedKeyConnectorApi
            .storeMasterKeyToKeyConnector(
                url = "$url/user-keys",
                body = KeyConnectorMasterKeyRequestJson(masterKey = masterKey),
            )
            .toResult()

    override suspend fun storeMasterKeyToKeyConnector(
        url: String,
        accessToken: String,
        masterKey: String,
    ): Result<Unit> =
        unauthenticatedKeyConnectorApi
            .storeMasterKeyToKeyConnector(
                url = "$url/user-keys",
                bearerToken = "$HEADER_VALUE_BEARER_PREFIX$accessToken",
                body = KeyConnectorMasterKeyRequestJson(masterKey = masterKey),
            )
            .toResult()

    override suspend fun updateKdf(body: UpdateKdfJsonRequest): Result<Unit> =
        authenticatedAccountsApi
            .updateKdf(body)
            .toResult()
}
