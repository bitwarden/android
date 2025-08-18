package com.bitwarden.network.service

import com.bitwarden.network.api.DigitalAssetLinkApi
import com.bitwarden.network.model.DigitalAssetLinkCheckResponseJson
import com.bitwarden.network.util.toResult

/**
 * Primary implementation of [DigitalAssetLinkService].
 */
internal class DigitalAssetLinkServiceImpl(
    private val digitalAssetLinkApi: DigitalAssetLinkApi,
) : DigitalAssetLinkService {

    override suspend fun checkDigitalAssetLinksRelations(
        sourceWebSite: String,
        targetPackageName: String,
        targetCertificateFingerprint: String,
        relations: List<String>,
    ): Result<DigitalAssetLinkCheckResponseJson> = digitalAssetLinkApi
        .checkDigitalAssetLinksRelations(
            sourceWebSite = sourceWebSite,
            targetPackageName = targetPackageName,
            targetCertificateFingerprint = targetCertificateFingerprint,
            relations = relations,
        )
        .toResult()
}
