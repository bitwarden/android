package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.AccountsApi
import com.x8bit.bitwarden.data.auth.datasource.network.api.AuthenticatedAccountsApi
import com.x8bit.bitwarden.data.auth.datasource.network.model.CreateAccountKeysRequest
import com.x8bit.bitwarden.data.auth.datasource.network.model.DeleteAccountRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.DeleteAccountResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.PasswordHintRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.PasswordHintResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.ResendEmailRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.ResetPasswordRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.SetPasswordRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.VerifyOtpRequestJson
import com.x8bit.bitwarden.data.platform.datasource.network.model.toBitwardenError
import com.x8bit.bitwarden.data.platform.datasource.network.util.parseErrorBodyOrNull
import kotlinx.serialization.json.Json

class AccountsServiceImpl(
    private val accountsApi: AccountsApi,
    private val authenticatedAccountsApi: AuthenticatedAccountsApi,
    private val json: Json,
) : AccountsService {

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
        accountsApi
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
        accountsApi.resendVerificationCodeEmail(body = body)

    override suspend fun resetPassword(body: ResetPasswordRequestJson): Result<Unit> {
        return if (body.currentPasswordHash == null) {
            authenticatedAccountsApi.resetTempPassword(body = body)
        } else {
            authenticatedAccountsApi.resetPassword(body = body)
        }
    }

    override suspend fun setPassword(
        body: SetPasswordRequestJson,
    ): Result<Unit> = authenticatedAccountsApi.setPassword(body)
}
