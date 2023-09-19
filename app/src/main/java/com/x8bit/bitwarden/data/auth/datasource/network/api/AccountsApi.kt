package com.x8bit.bitwarden.data.auth.datasource.network.api

import com.x8bit.bitwarden.data.auth.datasource.network.model.PreLoginRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.PreLoginResponseJson
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Defines calls under the /accounts API.
 */
interface AccountsApi {

    /**
     * Make pre login request to get KDF params.
     */
    @POST("/accounts/prelogin")
    suspend fun preLogin(@Body body: PreLoginRequestJson): Result<PreLoginResponseJson>
}
