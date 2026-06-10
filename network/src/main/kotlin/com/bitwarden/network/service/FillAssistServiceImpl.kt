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

    override suspend fun getManifest(): Result<FillAssistManifestJson> =
        api.getManifest().toResult()

    override suspend fun getForms(filename: String): Result<FillAssistFormsJson> =
        api.getForms(filename = filename).toResult()
}
