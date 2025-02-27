package com.bitwarden.authenticator.data.platform.repository

import com.bitwarden.authenticator.data.platform.datasource.disk.model.ServerConfig
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
