package com.bitwarden.network.service

import com.bitwarden.network.api.DigitalAssetLinkApi
import com.bitwarden.network.model.DigitalAssetLinkResponseJson
import com.bitwarden.network.util.toResult

/**
 * Primary implementation of [DigitalAssetLinkService].
 */
class DigitalAssetLinkServiceImpl(
    private val digitalAssetLinkApi: DigitalAssetLinkApi,
) : DigitalAssetLinkService {

    override suspend fun getDigitalAssetLinkForRp(
        scheme: String,
        relyingParty: String,
    ): Result<List<DigitalAssetLinkResponseJson>> =
        digitalAssetLinkApi
            .getDigitalAssetLinks(
                url = "$scheme$relyingParty/.well-known/assetlinks.json",
            )
            .toResult()
}
