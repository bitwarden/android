package com.bitwarden.network.service

import com.bitwarden.network.model.FillAssistFormsJson
import com.bitwarden.network.model.FillAssistManifestJson

/**
 * Provides access to the fill-assist targeting rules service.
 */
interface FillAssistService {
    /**
     * Fetches and parses the fill-assist manifest from [url].
     */
    suspend fun getManifest(url: String): Result<FillAssistManifestJson>

    /**
     * Downloads and parses the forms rules file from [formsUrl].
     *
     * Returns [Result.failure] if the network request fails or parsing fails.
     * Version-agnostic: any forms file URL can be passed regardless of schema version.
     */
    suspend fun getForms(formsUrl: String): Result<FillAssistFormsJson>
}
