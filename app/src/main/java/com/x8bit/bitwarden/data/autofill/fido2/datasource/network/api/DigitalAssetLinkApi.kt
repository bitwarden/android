package com.x8bit.bitwarden.data.autofill.fido2.datasource.network.api

import com.x8bit.bitwarden.data.autofill.fido2.datasource.network.model.DigitalAssetLinkResponseJson
import com.x8bit.bitwarden.data.platform.datasource.network.model.NetworkResult
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
