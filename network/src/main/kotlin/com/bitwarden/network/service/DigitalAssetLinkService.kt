package com.bitwarden.network.service

import com.bitwarden.network.model.DigitalAssetLinkCheckResponseJson

/**
 * Provides an API for querying digital asset links.
 */
interface DigitalAssetLinkService {
    /**
     * Checks if the given [relations] are declared in the digital asset link file for the given
     * [sourceWebSite] for the given [targetPackageName] with a [targetCertificateFingerprint].
     *
     * @param sourceWebSite The host of the source digital asset links file.
     * @param targetPackageName The package name of the target application.
     * @param targetCertificateFingerprint The certificate fingerprint of the target application.
     */
    suspend fun checkDigitalAssetLinksRelations(
        sourceWebSite: String,
        targetPackageName: String,
        targetCertificateFingerprint: String,
        relations: List<String>,
    ): Result<DigitalAssetLinkCheckResponseJson>
}
