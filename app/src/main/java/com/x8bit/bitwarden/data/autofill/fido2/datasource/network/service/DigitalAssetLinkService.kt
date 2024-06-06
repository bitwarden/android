package com.x8bit.bitwarden.data.autofill.fido2.datasource.network.service

import com.x8bit.bitwarden.data.autofill.fido2.datasource.network.model.DigitalAssetLinkResponseJson

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
