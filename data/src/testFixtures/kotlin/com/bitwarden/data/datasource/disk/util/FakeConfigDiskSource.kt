package com.bitwarden.data.datasource.disk.util

import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.data.datasource.disk.ConfigDiskSource
import com.bitwarden.data.datasource.disk.model.ServerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onSubscription

/**
 * A faked [ConfigDiskSource] that holds data in memory.
 */
class FakeConfigDiskSource : ConfigDiskSource {
    private var serverConfigValue: ServerConfig? = null

    override var serverConfig: ServerConfig?
        get() = serverConfigValue
        set(value) {
            serverConfigValue = value
            mutableServerConfigFlow.tryEmit(value)
        }

    override val serverConfigFlow: Flow<ServerConfig?>
        get() = mutableServerConfigFlow
            .onSubscription { emit(serverConfig) }

    private val mutableServerConfigFlow =
        bufferedMutableSharedFlow<ServerConfig?>(replay = 1)
}
