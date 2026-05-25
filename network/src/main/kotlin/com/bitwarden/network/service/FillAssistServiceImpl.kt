package com.bitwarden.network.service

import com.bitwarden.network.api.FillAssistApi
import com.bitwarden.network.model.FillAssistFormsJson
import com.bitwarden.network.model.FillAssistManifestJson
import com.bitwarden.network.util.toResult

/**
 * Default implementation of [FillAssistService].
 */
internal class FillAssistServiceImpl(
    private val api: FillAssistApi,
) : FillAssistService {

    override suspend fun getManifest(url: String): Result<FillAssistManifestJson> =
        api.getManifest(url = url).toResult()

    override suspend fun getForms(formsUrl: String): Result<FillAssistFormsJson> =
        api.getForms(url = formsUrl).toResult()
}
