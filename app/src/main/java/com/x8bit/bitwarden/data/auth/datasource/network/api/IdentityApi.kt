package com.x8bit.bitwarden.data.auth.datasource.network.api

import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson
import com.x8bit.bitwarden.data.platform.datasource.network.util.base64UrlEncode
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url
import java.util.UUID

/**
 * Defines calls under the /identity API.
 */
interface IdentityApi {

    @POST
    @Suppress("LongParameterList")
    @FormUrlEncoded
    suspend fun getToken(
        // TODO: use correct base URL here BIT-328
        @Url url: String = "https://vault.bitwarden.com/identity/connect/token",
        @Field(value = "scope", encoded = true) scope: String = "api+offline_access",
        @Field(value = "client_id") clientId: String = "mobile",
        @Field(value = "username") email: String,
        @Header(value = "auth-email") authEmail: String = email.base64UrlEncode(),
        @Field(value = "password") passwordHash: String,
        // TODO: use correct device identifier here BIT-325
        @Field(value = "deviceIdentifier") deviceIdentifier: String = UUID.randomUUID().toString(),
        // TODO: use correct values for deviceName and deviceType BIT-326
        @Field(value = "deviceName") deviceName: String = "Pixel 6",
        @Field(value = "deviceType") deviceType: String = "1",
        @Field(value = "grant_type") grantType: String = "password",
    ): Result<GetTokenResponseJson>
}
