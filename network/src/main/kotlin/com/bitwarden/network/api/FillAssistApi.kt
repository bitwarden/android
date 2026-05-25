package com.bitwarden.network.api

import com.bitwarden.network.model.FillAssistFormsJson
import com.bitwarden.network.model.FillAssistManifestJson
import com.bitwarden.network.model.NetworkResult
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * Defines endpoints for retrieving fill-assist targeting rules from the fill-assist service.
 * Uses [Url] to support the dynamic base URL provided by server config at runtime.
 */
internal interface FillAssistApi {
    /**
     * Fetches the fill-assist manifest from the given [url].
     */
    @GET
    suspend fun getManifest(
        @Url url: String,
    ): NetworkResult<FillAssistManifestJson>

    /**
     * Fetches and decodes the forms rules file from [url].
     */
    @GET
    suspend fun getForms(
        @Url url: String,
    ): NetworkResult<FillAssistFormsJson>
}
