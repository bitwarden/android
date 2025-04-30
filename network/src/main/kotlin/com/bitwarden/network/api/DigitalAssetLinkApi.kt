package com.bitwarden.network.api

import androidx.annotation.Keep
import com.bitwarden.network.model.DigitalAssetLinkCheckResponseJson
import com.bitwarden.network.model.NetworkResult
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Defines calls to a digital asset link file.
 */
@Keep
interface DigitalAssetLinkApi {

    /**
     * Checks if the given [relation] exists in a digital asset link file.
     */
    @GET("v1/assetlinks:check")
    suspend fun checkDigitalAssetLinksRelations(
        @Query("source.androidApp.packageName")
        sourcePackageName: String,
        @Query("source.androidApp.certificate.sha256Fingerprint")
        sourceCertificateFingerprint: String,
        @Query("target.androidApp.packageName")
        targetPackageName: String,
        @Query("target.androidApp.certificate.sha256Fingerprint")
        targetCertificateFingerprint: String,
        @Query("relation")
        relation: String,
    ): NetworkResult<DigitalAssetLinkCheckResponseJson>
}
