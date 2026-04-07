package com.bitwarden.data.repository

import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.data.datasource.disk.ConfigDiskSource
import com.bitwarden.data.datasource.disk.model.ServerConfig
import com.bitwarden.network.service.ConfigService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.Instant

/**
 * Primary implementation of [ServerConfigRepositoryImpl].
 */
internal class ServerConfigRepositoryImpl(
    private val configDiskSource: ConfigDiskSource,
    private val configService: ConfigService,
    private val clock: Clock,
    dispatcherManager: DispatcherManager,
) : ServerConfigRepository {

    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    override val serverConfigStateFlow: StateFlow<ServerConfig?>
        get() = configDiskSource
            .serverConfigFlow
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = configDiskSource.serverConfig,
            )

    override suspend fun getServerConfig(forceRefresh: Boolean): ServerConfig? {
        val localConfig = configDiskSource.serverConfig
        val needsRefresh = localConfig == null ||
            clock.instant().isAfter(
                Instant
                    .ofEpochMilli(localConfig.lastSync)
                    .plusSeconds(MINIMUM_CONFIG_SYNC_INTERVAL_SEC),
            )

        if (needsRefresh || forceRefresh) {
            configService
                .getConfig()
                .onSuccess { configResponse ->
                    val serverConfig = ServerConfig(
                        lastSync = clock.instant().toEpochMilli(),
                        serverData = configResponse,
                    )
                    configDiskSource.serverConfig = serverConfig
                    return serverConfig
                }
        }

        // If we are unable to retrieve a configuration from the server,
        // fall back to the local configuration.
        return localConfig
    }

    private companion object {
        private const val MINIMUM_CONFIG_SYNC_INTERVAL_SEC: Long = 60 * 60
    }
}
