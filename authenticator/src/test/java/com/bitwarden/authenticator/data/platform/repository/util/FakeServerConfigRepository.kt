package com.bitwarden.authenticator.data.platform.repository.util

import com.bitwarden.authenticator.data.platform.datasource.disk.model.ServerConfig
import com.bitwarden.authenticator.data.platform.datasource.network.model.ConfigResponseJson
import com.bitwarden.authenticator.data.platform.datasource.network.model.ConfigResponseJson.EnvironmentJson
import com.bitwarden.authenticator.data.platform.datasource.network.model.ConfigResponseJson.ServerJson
import com.bitwarden.authenticator.data.platform.repository.ServerConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant

class FakeServerConfigRepository : ServerConfigRepository {
    var serverConfigValue: ServerConfig?
        get() = mutableServerConfigFlow.value
        set(value) {
            mutableServerConfigFlow.value = value
        }

    private val mutableServerConfigFlow = MutableStateFlow<ServerConfig?>(SERVER_CONFIG)

    override suspend fun getServerConfig(forceRefresh: Boolean): ServerConfig? {
        if (forceRefresh) {
            return SERVER_CONFIG
        }

        return serverConfigValue
    }

    override val serverConfigStateFlow: StateFlow<ServerConfig?>
        get() = mutableServerConfigFlow
}

private val SERVER_CONFIG = ServerConfig(
    lastSync = Instant.parse("2023-10-27T12:00:00Z").toEpochMilli(),
    serverData = ConfigResponseJson(
        type = null,
        version = "2024.7.0",
        gitHash = "25cf6119-dirty",
        server = ServerJson(
            name = "example",
            url = "https://localhost:8080",
        ),
        environment = EnvironmentJson(
            cloudRegion = null,
            vaultUrl = "https://localhost:8080",
            apiUrl = "http://localhost:4000",
            identityUrl = "http://localhost:33656",
            notificationsUrl = "http://localhost:61840",
            ssoUrl = "http://localhost:51822",
        ),
        featureStates = mapOf(
            "duo-redirect" to JsonPrimitive(true),
            "flexible-collections-v-1" to JsonPrimitive(false),
            "dummy-boolean" to JsonPrimitive(true),
        ),
    ),
)
