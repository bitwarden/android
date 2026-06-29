package com.bitwarden.network.api

import com.bitwarden.network.model.CreateAccountKeysRequest
import com.bitwarden.network.model.CreateAccountKeysResponseJson
import com.bitwarden.network.model.DeleteAccountRequestJson
import com.bitwarden.network.model.NetworkResult
import com.bitwarden.network.model.ResetPasswordRequestJson
import com.bitwarden.network.model.SetPasswordRequestJson
import com.bitwarden.network.model.UpdateKdfJsonRequest
import com.bitwarden.network.model.VerifyOtpRequestJson
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT

/**
 * Defines raw calls under the /accounts API with authentication applied.
 */
internal interface AuthenticatedAccountsApi {

    /**
     * Converts the currently active account to a key-connector account.
     */
    @POST("/accounts/convert-to-key-connector")
    suspend fun convertToKeyConnector(): NetworkResult<Unit>

    /**
     * Creates the keys for the current account.
     */
    @POST("/accounts/keys")
    suspend fun createAccountKeys(
        @Body body: CreateAccountKeysRequest,
    ): NetworkResult<CreateAccountKeysResponseJson>

    /**
     * Deletes the current account.
     */
    @HTTP(method = "DELETE", path = "/accounts", hasBody = true)
    suspend fun deleteAccount(@Body body: DeleteAccountRequestJson): NetworkResult<Unit>

    @POST("/accounts/request-otp")
    suspend fun requestOtp(): NetworkResult<Unit>

    /**
     * Update the KDF settings for the current account.
     */
    @POST("/accounts/kdf")
    suspend fun updateKdf(@Body body: UpdateKdfJsonRequest): NetworkResult<Unit>

    @POST("/accounts/verify-otp")
    suspend fun verifyOtp(
        @Body body: VerifyOtpRequestJson,
    ): NetworkResult<Unit>

    /**
     * Resets the temporary password.
     */
    @PUT("/accounts/update-temp-password")
    suspend fun resetTempPassword(@Body body: ResetPasswordRequestJson): NetworkResult<Unit>

    /**
     * Resets the password.
     */
    @POST("/accounts/password")
    suspend fun resetPassword(@Body body: ResetPasswordRequestJson): NetworkResult<Unit>

    /**
     * Sets the password.
     */
    @POST("/accounts/set-password")
    suspend fun setPassword(@Body body: SetPasswordRequestJson): NetworkResult<Unit>
}
