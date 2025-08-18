package com.bitwarden.network.api

import com.bitwarden.network.model.KeyConnectorKeyRequestJson
import com.bitwarden.network.model.NetworkResult
import com.bitwarden.network.model.PasswordHintRequestJson
import com.bitwarden.network.model.ResendEmailRequestJson
import com.bitwarden.network.model.ResendNewDeviceOtpRequestJson
import com.bitwarden.network.util.HEADER_KEY_AUTHORIZATION
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Defines raw calls under the /accounts API.
 */
internal interface UnauthenticatedAccountsApi {
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

    @POST("/accounts/resend-new-device-otp")
    suspend fun resendNewDeviceOtp(
        @Body body: ResendNewDeviceOtpRequestJson,
    ): NetworkResult<Unit>
}
