package com.bitwarden.network.service

import com.bitwarden.network.model.DigitalAssetLinkResponseJson

/**
 * Provides an API for querying digital asset links.
 */
interface DigitalAssetLinkService {

    /**
     * Attempt to retrieve the asset links file from the provided [relyingParty].
     */
    suspend fun getDigitalAssetLinkForRp(
        scheme: String = "https://",
        relyingParty: String,
    ): Result<List<DigitalAssetLinkResponseJson>>
}
