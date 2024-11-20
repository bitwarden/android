package com.x8bit.bitwarden.data.auth.datasource.network.api

import com.x8bit.bitwarden.data.auth.datasource.network.model.KeyConnectorKeyRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.PasswordHintRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.ResendEmailRequestJson
import com.x8bit.bitwarden.data.platform.datasource.network.model.NetworkResult
import com.x8bit.bitwarden.data.platform.datasource.network.util.HEADER_KEY_AUTHORIZATION
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Defines raw calls under the /accounts API.
 */
interface UnauthenticatedAccountsApi {
    @POST("/accounts/password-hint")
    suspend fun passwordHintRequest(
        @Body body: PasswordHintRequestJson,
    ): NetworkResult<Unit>

    @POST("/two-factor/send-email-login")
    suspend fun resendVerificationCodeEmail(
        @Body body: ResendEmailRequestJson,
    ): NetworkResult<Unit>

    @POST("/accounts/set-key-connector-key")
    suspend fun setKeyConnectorKey(
        @Body body: KeyConnectorKeyRequestJson,
        @Header(HEADER_KEY_AUTHORIZATION) bearerToken: String,
    ): NetworkResult<Unit>
}
