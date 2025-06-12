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
internal interface DigitalAssetLinkApi {

    /**
     * Checks if the given [relations] are declared in the digital asset link file for the given
     * [sourceWebSite] for the given [targetPackageName] with a [targetCertificateFingerprint].
     *
     * @param sourceWebSite The host of the source digital asset links file.
     * @param targetPackageName The package name of the target application.
     * @param targetCertificateFingerprint The certificate fingerprint of the target application.
     */
    @GET("v1/assetlinks:check")
    suspend fun checkDigitalAssetLinksRelations(
        @Query("source.web.site")
        sourceWebSite: String,
        @Query("target.androidApp.packageName")
        targetPackageName: String,
        @Query("target.androidApp.certificate.sha256Fingerprint")
        targetCertificateFingerprint: String,
        @Query("relation")
        relations: List<String>,
    ): NetworkResult<DigitalAssetLinkCheckResponseJson>
}
