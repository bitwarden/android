package com.bitwarden.data.datasource.disk

import android.content.SharedPreferences
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.core.data.util.decodeFromStringOrNull
import com.bitwarden.data.datasource.disk.model.ServerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.serialization.json.Json

private const val SERVER_CONFIGURATIONS = "serverConfigurations"

/**
 * Primary implementation of [ConfigDiskSource].
 */
internal class ConfigDiskSourceImpl(
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
