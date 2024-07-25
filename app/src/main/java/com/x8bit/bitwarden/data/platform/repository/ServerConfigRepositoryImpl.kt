package com.x8bit.bitwarden.data.platform.repository

import com.x8bit.bitwarden.data.platform.datasource.disk.ConfigDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.model.ServerConfig
import com.x8bit.bitwarden.data.platform.datasource.network.service.ConfigService
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.Instant

class ServerConfigRepositoryImpl(
    private val configDiskSource: ConfigDiskSource,
    private val configService: ConfigService,
    dispatcherManager: DispatcherManager,
) : ServerConfigRepository {

    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)
    override suspend fun getServerConfig(forceRefresh: Boolean): ServerConfig? {
        val localConfig = configDiskSource.serverConfig
        val needsRefresh = localConfig == null || localConfig.let {
            Instant.ofEpochMilli(it.lastSync).isAfter(
                Instant.now().plusSeconds(MINIMUM_CONFIG_SYNC_INTERVAL)
            )
        }

        if (needsRefresh || forceRefresh) {
            configService.getConfig().fold(
                onSuccess = { configResponse ->
                    val serverConfig = ServerConfig(
                        lastSync = Instant.now().toEpochMilli(),
                        serverData = configResponse
                    )
                    configDiskSource.serverConfig = serverConfig
                    return serverConfig
                },
                onFailure = {
                    throw it
                }
            )
        }

        // If we are unable to retrieve a configuration from the server,
        // fall back to the local configuration.
        return localConfig
    }

    override val serverConfigStateFlow: StateFlow<ServerConfig?>
        get() = configDiskSource.serverConfigFlow
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = configDiskSource.serverConfig,
            )

    companion object {
        private const val MINIMUM_CONFIG_SYNC_INTERVAL: Long = 60 * 60
    }
}