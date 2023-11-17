package com.x8bit.bitwarden.data.auth.datasource.network.api

import com.x8bit.bitwarden.data.auth.datasource.network.model.DeleteAccountRequestJson
import retrofit2.http.Body
import retrofit2.http.HTTP

/**
 * Defines raw calls under the /accounts API with authentication applied.
 */
interface AuthenticatedAccountsApi {

    /**
     * Deletes the current account.
     */
    @HTTP(method = "DELETE", path = "/accounts", hasBody = true)
    suspend fun deleteAccount(@Body body: DeleteAccountRequestJson): Result<Unit>
}
