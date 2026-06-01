package com.bitwarden.data.repository

import com.bitwarden.data.datasource.disk.model.ServerConfig
import kotlinx.coroutines.flow.StateFlow

/**
 * Provides an API for observing the server config state.
 */
interface ServerConfigRepository {

    /**
     * Emits updates that track [ServerConfig].
     */
    val serverConfigStateFlow: StateFlow<ServerConfig?>

    /**
     * Gets the state [ServerConfig]. If needed or forced by [forceRefresh],
     * updates the values using server side data.
     */
    suspend fun getServerConfig(forceRefresh: Boolean): ServerConfig?
}
