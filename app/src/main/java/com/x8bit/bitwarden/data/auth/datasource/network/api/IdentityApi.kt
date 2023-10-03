package com.x8bit.bitwarden.data.auth.datasource.network.api

import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Defines raw calls under the /identity API.
 */
interface IdentityApi {

    @POST
    @Suppress("LongParameterList")
    @FormUrlEncoded
    suspend fun getToken(
        @Url url: String,
        @Field(value = "scope", encoded = true) scope: String,
        @Field(value = "client_id") clientId: String,
        @Field(value = "username") email: String,
        @Header(value = "auth-email") authEmail: String,
        @Field(value = "password") passwordHash: String,
        @Field(value = "deviceIdentifier") deviceIdentifier: String,
        @Field(value = "deviceName") deviceName: String,
        @Field(value = "deviceType") deviceType: String,
        @Field(value = "grant_type") grantType: String,
        @Field(value = "captchaResponse") captchaResponse: String?,
    ): Result<GetTokenResponseJson.Success>
}
