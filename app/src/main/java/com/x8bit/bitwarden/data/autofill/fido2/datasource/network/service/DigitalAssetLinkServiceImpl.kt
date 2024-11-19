package com.x8bit.bitwarden.data.autofill.fido2.datasource.network.service

import com.x8bit.bitwarden.data.autofill.fido2.datasource.network.api.DigitalAssetLinkApi
import com.x8bit.bitwarden.data.autofill.fido2.datasource.network.model.DigitalAssetLinkResponseJson
import com.x8bit.bitwarden.data.platform.datasource.network.util.toResult

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
