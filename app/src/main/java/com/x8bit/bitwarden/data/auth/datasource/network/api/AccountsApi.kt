package com.x8bit.bitwarden.data.auth.datasource.network.api

import com.x8bit.bitwarden.data.auth.datasource.network.model.PreLoginRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.PreLoginResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RegisterRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RegisterResponseJson
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Defines raw calls under the /accounts API.
 */
interface AccountsApi {

    @POST("/accounts/prelogin")
    suspend fun preLogin(@Body body: PreLoginRequestJson): Result<PreLoginResponseJson>

    @POST("/accounts/register")
    suspend fun register(@Body body: RegisterRequestJson): Result<RegisterResponseJson.Success>
}
