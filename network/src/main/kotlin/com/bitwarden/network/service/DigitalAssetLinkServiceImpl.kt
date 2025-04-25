package com.bitwarden.network.service

import com.bitwarden.network.api.DigitalAssetLinkApi
import com.bitwarden.network.model.DigitalAssetLinkCheckResponseJson
import com.bitwarden.network.util.toResult

/**
 * Primary implementation of [DigitalAssetLinkService].
 */
class DigitalAssetLinkServiceImpl(
    private val digitalAssetLinkApi: DigitalAssetLinkApi,
) : DigitalAssetLinkService {

    override suspend fun checkDigitalAssetLinksRelations(
        packageName: String,
        certificateFingerprint: String,
        relation: String,
    ): Result<DigitalAssetLinkCheckResponseJson> = digitalAssetLinkApi
        .checkDigitalAssetLinksRelations(
            sourcePackageName = packageName,
            sourceCertificateFingerprint = certificateFingerprint,
            targetPackageName = packageName,
            targetCertificateFingerprint = certificateFingerprint,
            relation = relation,
        )
        .toResult()
}
