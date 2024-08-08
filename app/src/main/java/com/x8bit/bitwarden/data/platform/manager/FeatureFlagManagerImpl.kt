package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.platform.datasource.disk.model.ServerConfig
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.repository.ServerConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val CIPHER_KEY_ENCRYPTION_KEY = "enableCipherKeyEncryption"

/**
 * Primary implementation of [FeatureFlagManager].
 */
class FeatureFlagManagerImpl(
    private val serverConfigRepository: ServerConfigRepository,
) : FeatureFlagManager {

    override val sdkFeatureFlags: Map<String, Boolean>
        get() = mapOf(CIPHER_KEY_ENCRYPTION_KEY to true)

    override fun <T : Any> getFeatureFlagFlow(key: FlagKey<T>): Flow<T> =
        serverConfigRepository
            .serverConfigStateFlow
            .map { serverConfig ->
                serverConfig.getFlagValueOrDefault(key = key)
            }

    override suspend fun <T : Any> getFeatureFlag(
        key: FlagKey<T>,
        forceRefresh: Boolean,
    ): T =
        serverConfigRepository
            .getServerConfig(forceRefresh = forceRefresh)
            .getFlagValueOrDefault(key = key)

    override fun <T : Any> getFeatureFlag(key: FlagKey<T>): T =
        serverConfigRepository
            .serverConfigStateFlow
            .value
            .getFlagValueOrDefault(key = key)
}

private fun <T : Any> ServerConfig?.getFlagValueOrDefault(key: FlagKey<T>): T {
    val defaultValue = key.defaultValue
    return this?.serverData
        ?.featureStates
        ?.get(key.keyName)
        ?.let {
            try {
                // Suppressed since we are checking the type before doing the cast
                @Suppress("UNCHECKED_CAST")
                when (defaultValue::class) {
                    Boolean::class -> it.content.toBoolean() as T
                    String::class -> it.content as T
                    Int::class -> it.content.toInt() as T
                    else -> defaultValue
                }
            } catch (ex: ClassCastException) {
                defaultValue
            } catch (ex: NumberFormatException) {
                defaultValue
            }
        }
        ?: defaultValue
}
