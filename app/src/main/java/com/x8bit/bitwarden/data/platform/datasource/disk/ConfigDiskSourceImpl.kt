package com.x8bit.bitwarden.data.platform.datasource.disk

import android.content.SharedPreferences
import com.x8bit.bitwarden.data.platform.datasource.network.model.ConfigResponseJson
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

    override var serverConfig: ConfigResponseJson?
        get() = getString(key = SERVER_CONFIGURATIONS)?.let { json.decodeFromStringOrNull(it) }
        set(value) {
            putString(
                key = SERVER_CONFIGURATIONS,
                value = value?.let { json.encodeToString(value) },
            )
            mutableServerConfigFlow.tryEmit(value)
        }

    override val serverConfigFlow: Flow<ConfigResponseJson?>
        get() = mutableServerConfigFlow.onSubscription { emit(serverConfig) }

    private val mutableServerConfigFlow = bufferedMutableSharedFlow<ConfigResponseJson?>(replay = 1)
}
