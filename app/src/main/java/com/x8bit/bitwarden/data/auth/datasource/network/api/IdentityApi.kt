package com.x8bit.bitwarden.data.auth.datasource.network.api

import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.PrevalidateSsoResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RefreshTokenResponseJson
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Defines raw calls under the /identity API.
 */
interface IdentityApi {

    @POST("/connect/token")
    @Suppress("LongParameterList")
    @FormUrlEncoded
    suspend fun getToken(
        @Field(value = "scope", encoded = true) scope: String,
        @Field(value = "client_id") clientId: String,
        @Field(value = "username") email: String,
        @Header(value = "Auth-Email") authEmail: String,
        @Field(value = "password") passwordHash: String?,
        @Field(value = "deviceIdentifier") deviceIdentifier: String,
        @Field(value = "deviceName") deviceName: String,
        @Field(value = "deviceType") deviceType: String,
        @Field(value = "grant_type") grantType: String,
        @Field(value = "captchaResponse") captchaResponse: String?,
        @Field(value = "code") ssoCode: String?,
        @Field(value = "code_verifier") ssoCodeVerifier: String?,
        @Field(value = "redirect_uri") ssoRedirectUri: String?,
        @Field(value = "twoFactorToken") twoFactorCode: String?,
        @Field(value = "twoFactorProvider") twoFactorMethod: String?,
        @Field(value = "twoFactorRemember") twoFactorRemember: String?,
        @Field(value = "authRequest") authRequestId: String?,
    ): Result<GetTokenResponseJson.Success>

    @GET("/sso/prevalidate")
    suspend fun prevalidateSso(
        @Query("domainHint") organizationIdentifier: String,
    ): Result<PrevalidateSsoResponseJson>

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
    ): Call<RefreshTokenResponseJson>
}
