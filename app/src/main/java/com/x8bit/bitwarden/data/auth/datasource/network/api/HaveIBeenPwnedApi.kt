package com.x8bit.bitwarden.data.auth.datasource.network.api

import com.x8bit.bitwarden.data.platform.datasource.network.model.NetworkResult
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Defines endpoints for the "have I been pwned" API. For docs see
 * https://haveibeenpwned.com/API/v2.
 */
interface HaveIBeenPwnedApi {

    @GET("/range/{hashPrefix}")
    suspend fun fetchBreachedPasswords(
        @Path("hashPrefix")
        hashPrefix: String,
    ): NetworkResult<ResponseBody>
}
