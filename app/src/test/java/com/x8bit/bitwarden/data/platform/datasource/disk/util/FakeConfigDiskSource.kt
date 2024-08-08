package com.x8bit.bitwarden.data.platform.datasource.disk.util

import com.x8bit.bitwarden.data.platform.datasource.disk.ConfigDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.model.ServerConfig
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onSubscription

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
