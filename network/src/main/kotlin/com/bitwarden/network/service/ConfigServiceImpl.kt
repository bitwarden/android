package com.bitwarden.network.service

import com.bitwarden.network.api.ConfigApi
import com.bitwarden.network.model.ConfigResponseJson
import com.bitwarden.network.util.toResult

/**
 * Default implementation of [ConfigService] for querying app configurations.
 */
// TODO [PM-19846] Make internal when dependents are migrated.
internal class ConfigServiceImpl(private val configApi: ConfigApi) : ConfigService {
    override suspend fun getConfig(): Result<ConfigResponseJson> = configApi.getConfig().toResult()
}
