package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.platform.datasource.disk.ConfigDiskSource
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.ServerConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private const val CIPHER_KEY_ENCRYPTION_KEY = "enableCipherKeyEncryption"

/**
 * Primary implementation of [FeatureFlagManager].
 */
class FeatureFlagManagerImpl(
    private val serverConfigRepository: ServerConfigRepository,
    private val configDiskSource: ConfigDiskSource,
    dispatcherManager: DispatcherManager,
) : FeatureFlagManager {

    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    override val featureFlagsLocal: Map<String, Boolean>
        get() = mapOf(CIPHER_KEY_ENCRYPTION_KEY to true)

    override val featureFlagsServerStateFlow: StateFlow<Map<String, String>?>
        get() = configDiskSource
            .serverConfigFlow
            .map { serverConfig ->
                serverConfig?.serverData?.featureStates
            }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = configDiskSource.serverConfig?.serverData?.featureStates,
            )

    override suspend fun getFeatureFlag(
        key: FlagKey,
        defaultValue: Boolean,
        forceRefresh: Boolean,
    ): Boolean {
        return getFlagStringValueOrNull(key, forceRefresh)?.toBoolean() ?: defaultValue
    }

    override suspend fun getFeatureFlag(
        key: FlagKey,
        defaultValue: Int,
        forceRefresh: Boolean,
    ): Int {
        return getFlagStringValueOrNull(key, forceRefresh)?.toInt() ?: defaultValue
    }

    override suspend fun getFeatureFlag(
        key: FlagKey,
        defaultValue: String?,
        forceRefresh: Boolean,
    ): String? {
        return getFlagStringValueOrNull(key, forceRefresh) ?: defaultValue
    }

    private suspend fun getFlagStringValueOrNull(key: FlagKey, forceRefresh: Boolean): String? {
        val configuration = serverConfigRepository.getServerConfig(forceRefresh)
        return configuration?.serverData?.featureStates?.get(key.stringValue)
    }
}

/**
 * Enum to hold feature flag keys.
 * [stringValue] corresponds to the string value of a give key
 */
enum class FlagKey(val stringValue: String) {
    EmailVerification("email-verification"),
}
