package com.bitwarden.authenticator.data.platform.datasource.disk.model

import com.bitwarden.authenticator.data.platform.datasource.network.model.ConfigResponseJson
import kotlinx.serialization.SerialName
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
    @SerialName("lastSync")
    val lastSync: Long,

    @SerialName("serverData")
    val serverData: ConfigResponseJson,
)
