package com.x8bit.bitwarden.data.platform.datasource.disk.model

import com.x8bit.bitwarden.data.platform.datasource.network.model.ConfigResponseJson
import kotlinx.serialization.Serializable

/**
 * A higher-level wrapper around [ConfigResponseJson] that provides a timestamp
 * to check if a sync is necessary
 *
 * @property lastSync The [Long] of the last sync.
 * @property serverData The raw [ConfigResponseJson] that contains specific data of the
 * server configuration
 */
@Serializable
data class ServerConfig(
    val lastSync: Long,
    val serverData: ConfigResponseJson,
)
