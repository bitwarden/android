package com.bitwarden.network.api

import com.bitwarden.network.model.DigitalAssetLinkResponseJson
import com.bitwarden.network.model.NetworkResult
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * Defines calls to an RP digital asset link file.
 */
interface DigitalAssetLinkApi {

    /**
     * Attempts to download the asset links file from the RP.
     */
    @GET
    suspend fun getDigitalAssetLinks(
        @Url url: String,
    ): NetworkResult<List<DigitalAssetLinkResponseJson>>
}
