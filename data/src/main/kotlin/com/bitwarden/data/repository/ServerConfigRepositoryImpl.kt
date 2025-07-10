package com.bitwarden.data.repository

import android.util.Log
import com.bitwarden.data.datasource.disk.ConfigDiskSource
import com.bitwarden.data.datasource.disk.model.ServerConfig
import com.bitwarden.data.manager.DispatcherManager
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
            Instant
                .ofEpochMilli(localConfig.lastSync)
                .isAfter(
                    clock.instant().plusSeconds(MINIMUM_CONFIG_SYNC_INTERVAL_SEC),
                )
        Log.d("ServerConfigRepository", "needsRefresh: $needsRefresh")
        Log.d("ServerConfigRepository", "lastSync: ${localConfig?.lastSync}")

        if (needsRefresh || forceRefresh) {
            configService
                .getConfig()
                .onSuccess { configResponse ->
                    val serverConfig = ServerConfig(
                        lastSync = clock.instant().toEpochMilli(),
                        serverData = configResponse,
                    )
                    configDiskSource.serverConfig = serverConfig
                    val featureStatesMap = serverConfig.serverData.featureStates

                    Log.d("ServerConfigRepository", "Will check for flags")
                    if (featureStatesMap != null) {
                        // Option 3: Specifically print the value for "enable-pm-bwa-sync"
                        val specificValue = featureStatesMap["enable-pm-bwa-sync"]
                        if (specificValue != null) {
                            Log.d(
                                "ServerConfigRepository",
                                "Feature 'enable-pm-bwa-sync': $specificValue",
                            )
                        } else {
                            Log.d(
                                "ServerConfigRepository",
                                "Feature 'enable-pm-bwa-sync' not found.",
                            )
                        }
                    }
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
