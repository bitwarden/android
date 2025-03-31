package com.x8bit.bitwarden.data.platform.datasource.network.service

import com.bitwarden.network.util.toResult
import com.x8bit.bitwarden.data.platform.datasource.network.api.ConfigApi
import com.x8bit.bitwarden.data.platform.datasource.network.model.ConfigResponseJson

class ConfigServiceImpl(private val configApi: ConfigApi) : ConfigService {
    override suspend fun getConfig(): Result<ConfigResponseJson> = configApi.getConfig().toResult()
}
