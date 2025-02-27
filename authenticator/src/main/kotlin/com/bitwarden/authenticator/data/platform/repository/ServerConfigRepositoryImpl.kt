package com.bitwarden.authenticator.data.platform.repository

import com.bitwarden.authenticator.data.platform.datasource.disk.ConfigDiskSource
import com.bitwarden.authenticator.data.platform.datasource.disk.model.ServerConfig
import com.bitwarden.authenticator.data.platform.datasource.network.service.ConfigService
import com.bitwarden.authenticator.data.platform.manager.DispatcherManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.Instant

/**
 * Primary implementation of [ServerConfigRepositoryImpl].
 */
class ServerConfigRepositoryImpl(
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
            Instant
                .ofEpochMilli(localConfig.lastSync)
                .isAfter(
                    clock.instant().plusSeconds(MINIMUM_CONFIG_SYNC_INTERVAL_SEC),
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
