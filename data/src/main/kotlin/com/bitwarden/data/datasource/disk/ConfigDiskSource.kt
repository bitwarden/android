package com.bitwarden.data.datasource.disk

import com.bitwarden.data.datasource.disk.model.ServerConfig
import kotlinx.coroutines.flow.Flow

/**
 * Primary access point for server configuration-related disk information.
 */
interface ConfigDiskSource {

    /**
     * The currently persisted [ServerConfig] (or `null` if not set).
     */
    var serverConfig: ServerConfig?

    /**
     * Emits updates that track [ServerConfig]. This will replay the last known value,
     * if any.
     */
    val serverConfigFlow: Flow<ServerConfig?>
}
