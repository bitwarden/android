package com.x8bit.bitwarden.data.platform.datasource.disk

import android.content.SharedPreferences
import com.x8bit.bitwarden.data.platform.datasource.disk.model.ServerConfig
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.platform.util.decodeFromStringOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val SERVER_CONFIGURATIONS = "serverConfigurations"

/**
 * Primary implementation of [ConfigDiskSource].
 */
class ConfigDiskSourceImpl(
    sharedPreferences: SharedPreferences,
    private val json: Json,
) : BaseDiskSource(sharedPreferences = sharedPreferences),
    ConfigDiskSource {

    override var serverConfig: ServerConfig?
        get() = getString(key = SERVER_CONFIGURATIONS)?.let { json.decodeFromStringOrNull(it) }
        set(value) {
            putString(
                key = SERVER_CONFIGURATIONS,
                value = value?.let { json.encodeToString(it) },
            )
            mutableServerConfigFlow.tryEmit(value)
        }

    override val serverConfigFlow: Flow<ServerConfig?>
        get() = mutableServerConfigFlow.onSubscription { emit(serverConfig) }

    private val mutableServerConfigFlow = bufferedMutableSharedFlow<ServerConfig?>(replay = 1)
}
