package com.x8bit.bitwarden.data.auth.datasource.network.api

import com.x8bit.bitwarden.data.auth.datasource.network.model.PasswordHintRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.ResendEmailRequestJson
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Defines raw calls under the /accounts API.
 */
interface AccountsApi {
    @POST("/accounts/password-hint")
    suspend fun passwordHintRequest(
        @Body body: PasswordHintRequestJson,
    ): Result<Unit>

    @POST("/two-factor/send-email-login")
    suspend fun resendVerificationCodeEmail(
        @Body body: ResendEmailRequestJson,
    ): Result<Unit>
}
