package com.bitwarden.network.api

import com.bitwarden.network.model.FillAssistFormsJson
import com.bitwarden.network.model.FillAssistManifestJson
import com.bitwarden.network.model.NetworkResult
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Defines endpoints for retrieving fill-assist targeting rules. The base URL is set dynamically
 * at runtime via [com.bitwarden.network.interceptor.BaseUrlInterceptors.fillAssistInterceptor].
 */
internal interface FillAssistApi {
    /**
     * Fetches the fill-assist manifest.
     */
    @GET("manifest.json")
    suspend fun getManifest(): NetworkResult<FillAssistManifestJson>

    /**
     * Fetches the forms rules file by [filename] (e.g. "forms.v1.json").
     */
    @GET("{filename}")
    suspend fun getForms(
        @Path("filename") filename: String,
    ): NetworkResult<FillAssistFormsJson>
}
