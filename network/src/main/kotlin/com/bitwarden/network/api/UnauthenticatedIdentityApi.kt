package com.bitwarden.network.api

import com.bitwarden.network.model.GetTokenResponseJson
import com.bitwarden.network.model.NetworkResult
import com.bitwarden.network.model.PreLoginRequestJson
import com.bitwarden.network.model.PreLoginResponseJson
import com.bitwarden.network.model.PrevalidateSsoResponseJson
import com.bitwarden.network.model.RefreshTokenResponseJson
import com.bitwarden.network.model.RegisterFinishRequestJson
import com.bitwarden.network.model.RegisterRequestJson
import com.bitwarden.network.model.RegisterResponseJson
import com.bitwarden.network.model.SendVerificationEmailRequestJson
import com.bitwarden.network.model.VerifyEmailTokenRequestJson
import kotlinx.serialization.json.JsonPrimitive
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Defines raw calls under the /identity API.
 */
internal interface UnauthenticatedIdentityApi {

    @POST("/connect/token")
    @Suppress("LongParameterList")
    @FormUrlEncoded
    suspend fun getToken(
        @Field(value = "scope", encoded = true) scope: String,
        @Field(value = "client_id") clientId: String,
        @Field(value = "username") email: String,
        @Field(value = "password") passwordHash: String?,
        @Field(value = "deviceIdentifier") deviceIdentifier: String,
        @Field(value = "deviceName") deviceName: String,
        @Field(value = "deviceType") deviceType: String,
        @Field(value = "grant_type") grantType: String,
        @Field(value = "code") ssoCode: String?,
        @Field(value = "code_verifier") ssoCodeVerifier: String?,
        @Field(value = "redirect_uri") ssoRedirectUri: String?,
        @Field(value = "twoFactorToken") twoFactorCode: String?,
        @Field(value = "twoFactorProvider") twoFactorMethod: String?,
        @Field(value = "twoFactorRemember") twoFactorRemember: String?,
        @Field(value = "authRequest") authRequestId: String?,
        @Field(value = "newDeviceOtp") newDeviceOtp: String?,
    ): NetworkResult<GetTokenResponseJson.Success>

    @GET("/sso/prevalidate")
    suspend fun prevalidateSso(
        @Query("domainHint") organizationIdentifier: String,
    ): NetworkResult<PrevalidateSsoResponseJson.Success>

    /**
     * This call needs to be synchronous so we need it to return a [Call] directly. The identity
     * service will wrap it up for us.
     */
    @POST("/connect/token")
    @FormUrlEncoded
    fun refreshTokenCall(
        @Field(value = "client_id") clientId: String,
        @Field(value = "refresh_token") refreshToken: String,
        @Field(value = "grant_type") grantType: String,
    ): Call<RefreshTokenResponseJson.Success>

    @POST("/accounts/prelogin")
    suspend fun preLogin(@Body body: PreLoginRequestJson): NetworkResult<PreLoginResponseJson>

    @POST("/accounts/register")
    suspend fun register(
        @Body body: RegisterRequestJson,
    ): NetworkResult<RegisterResponseJson.Success>

    @POST("/accounts/register/finish")
    suspend fun registerFinish(
        @Body body: RegisterFinishRequestJson,
    ): NetworkResult<RegisterResponseJson.Success>

    @POST("/accounts/register/send-verification-email")
    suspend fun sendVerificationEmail(
        @Body body: SendVerificationEmailRequestJson,
    ): NetworkResult<JsonPrimitive?>

    @POST("/accounts/register/verification-email-clicked")
    suspend fun verifyEmailToken(
        @Body body: VerifyEmailTokenRequestJson,
    ): NetworkResult<Unit>
}
