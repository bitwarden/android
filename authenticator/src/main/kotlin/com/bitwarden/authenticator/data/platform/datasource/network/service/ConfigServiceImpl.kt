package com.bitwarden.authenticator.data.platform.datasource.network.service

import com.bitwarden.authenticator.data.platform.datasource.network.api.ConfigApi
import com.bitwarden.authenticator.data.platform.datasource.network.model.ConfigResponseJson
import com.bitwarden.network.util.toResult

/**
 * Default implementation of [ConfigService] for querying for app configurations.
 */
class ConfigServiceImpl(private val configApi: ConfigApi) : ConfigService {
    override suspend fun getConfig(): Result<ConfigResponseJson> = configApi.getConfig().toResult()
}
