package com.bitwarden.network.service

import com.bitwarden.network.model.DigitalAssetLinkCheckResponseJson

/**
 * Provides an API for querying digital asset links.
 */
interface DigitalAssetLinkService {
    /**
     * Checks if the given [packageName] with a given [certificateFingerprint] has the given
     * [relation].
     */
    suspend fun checkDigitalAssetLinksRelations(
        packageName: String,
        certificateFingerprint: String,
        relation: String,
    ): Result<DigitalAssetLinkCheckResponseJson>
}
