package com.bitwarden.network.service

import com.bitwarden.network.model.FillAssistFormsJson
import com.bitwarden.network.model.FillAssistManifestJson

/**
 * Provides access to the fill-assist targeting rules service.
 */
interface FillAssistService {
    /**
     * Fetches and parses the fill-assist manifest.
     */
    suspend fun getManifest(): Result<FillAssistManifestJson>

    /**
     * Downloads and parses the forms rules file identified by [filename] (e.g. "forms.v1.json").
     *
     * Returns [Result.failure] if the network request fails or parsing fails.
     */
    suspend fun getForms(filename: String): Result<FillAssistFormsJson>
}
