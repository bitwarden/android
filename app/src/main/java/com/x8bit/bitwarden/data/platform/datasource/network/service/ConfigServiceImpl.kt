package com.x8bit.bitwarden.data.platform.datasource.network.service

import com.bitwarden.network.api.ConfigApi
import com.bitwarden.network.model.ConfigResponseJson
import com.bitwarden.network.util.toResult

class ConfigServiceImpl(private val configApi: ConfigApi) : ConfigService {
    override suspend fun getConfig(): Result<ConfigResponseJson> = configApi.getConfig().toResult()
}
