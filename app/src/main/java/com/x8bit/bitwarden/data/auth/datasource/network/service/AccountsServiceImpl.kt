package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.AccountsApi
import com.x8bit.bitwarden.data.auth.datasource.network.model.PreLoginRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.PreLoginResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RegisterRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RegisterResponseJson
import com.x8bit.bitwarden.data.platform.datasource.network.model.toBitwardenError
import com.x8bit.bitwarden.data.platform.datasource.network.util.parseErrorBodyOrNull
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection

class AccountsServiceImpl constructor(
    private val accountsApi: AccountsApi,
    private val json: Json,
) : AccountsService {

    override suspend fun preLogin(email: String): Result<PreLoginResponseJson> =
        accountsApi.preLogin(PreLoginRequestJson(email = email))

    // TODO add error parsing and pass along error message for validations BIT-763
    override suspend fun register(body: RegisterRequestJson): Result<RegisterResponseJson> =
        accountsApi
            .register(body)
            .recoverCatching { throwable ->
                throwable
                    .toBitwardenError()
                    .parseErrorBodyOrNull<RegisterResponseJson.CaptchaRequired>(
                        code = HttpURLConnection.HTTP_BAD_REQUEST,
                        json = json,
                    ) ?: throw throwable
            }
}
