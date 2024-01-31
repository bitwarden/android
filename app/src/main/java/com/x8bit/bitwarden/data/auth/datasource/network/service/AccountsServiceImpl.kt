package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.AccountsApi
import com.x8bit.bitwarden.data.auth.datasource.network.api.AuthenticatedAccountsApi
import com.x8bit.bitwarden.data.auth.datasource.network.model.DeleteAccountRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.PasswordHintRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.PasswordHintResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.PreLoginRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.PreLoginResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RegisterRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RegisterResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.ResendEmailRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.ResetPasswordRequestJson
import com.x8bit.bitwarden.data.platform.datasource.network.model.toBitwardenError
import com.x8bit.bitwarden.data.platform.datasource.network.util.parseErrorBodyOrNull
import kotlinx.serialization.json.Json

class AccountsServiceImpl constructor(
    private val accountsApi: AccountsApi,
    private val authenticatedAccountsApi: AuthenticatedAccountsApi,
    private val json: Json,
) : AccountsService {

    override suspend fun deleteAccount(masterPasswordHash: String): Result<Unit> =
        authenticatedAccountsApi.deleteAccount(DeleteAccountRequestJson(masterPasswordHash))

    override suspend fun preLogin(email: String): Result<PreLoginResponseJson> =
        accountsApi.preLogin(PreLoginRequestJson(email = email))

    @Suppress("MagicNumber")
    override suspend fun register(body: RegisterRequestJson): Result<RegisterResponseJson> =
        accountsApi
            .register(body)
            .recoverCatching { throwable ->
                val bitwardenError = throwable.toBitwardenError()
                bitwardenError.parseErrorBodyOrNull<RegisterResponseJson.CaptchaRequired>(
                    code = 400,
                    json = json,
                ) ?: bitwardenError.parseErrorBodyOrNull<RegisterResponseJson.Invalid>(
                    codes = listOf(400, 429),
                    json = json,
                ) ?: bitwardenError.parseErrorBodyOrNull<RegisterResponseJson.Error>(
                    code = 429,
                    json = json,
                ) ?: throw throwable
            }

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
}
