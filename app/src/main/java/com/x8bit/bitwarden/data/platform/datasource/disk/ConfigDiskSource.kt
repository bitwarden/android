package com.x8bit.bitwarden.data.platform.datasource.disk

import com.x8bit.bitwarden.data.platform.datasource.network.model.ConfigResponseJson
import kotlinx.coroutines.flow.Flow

/**
 * Primary access point for server configuration-related disk information.
 */
interface ConfigDiskSource {
    /**
     * The currently persisted [ConfigResponseJson] (or `null` if not set).
     */
    var serverConfig: ConfigResponseJson?

    /**
     * Emits updates that track [ConfigResponseJson]. This will replay the last known value,
     * if any.
     */
    val serverConfigDataFlow: Flow<ConfigResponseJson?>
}
