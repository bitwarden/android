package com.x8bit.bitwarden.data.platform.repository

import com.x8bit.bitwarden.data.platform.datasource.disk.ConfigDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.model.ServerConfig
import com.x8bit.bitwarden.data.platform.datasource.network.service.ConfigService
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.Instant

/**
 * Primary implementation of [ServerConfigRepositoryImpl].
 */
class ServerConfigRepositoryImpl(
    private val configDiskSource: ConfigDiskSource,
    private val configService: ConfigService,
    private val clock: Clock = Clock.systemDefaultZone(),
    environmentRepository: EnvironmentRepository,
    dispatcherManager: DispatcherManager,
) : ServerConfigRepository {

    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    init {
        environmentRepository
            .environmentStateFlow
            .onEach {
                getServerConfig(true)
            }
            .launchIn(unconfinedScope)
    }

    override suspend fun getServerConfig(forceRefresh: Boolean): ServerConfig? {
        val localConfig = configDiskSource.serverConfig
        val needsRefresh = localConfig == null || localConfig.let {
            Instant.ofEpochMilli(it.lastSync).isAfter(
                clock.instant().plusSeconds(MINIMUM_CONFIG_SYNC_INTERVAL),
            )
        }

        if (needsRefresh || forceRefresh) {
            configService.getConfig().onSuccess { configResponse ->
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
