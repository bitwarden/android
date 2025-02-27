package com.bitwarden.authenticator.data.platform.manager

import com.bitwarden.authenticator.data.platform.datasource.disk.model.ServerConfig
import com.bitwarden.authenticator.data.platform.manager.model.FlagKey
import com.bitwarden.authenticator.data.platform.repository.ServerConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Primary implementation of [FeatureFlagManager].
 */
class FeatureFlagManagerImpl(
    private val serverConfigRepository: ServerConfigRepository,
) : FeatureFlagManager {

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

/**
 * Extract the value of a [FlagKey] from the [ServerConfig]. If there is an issue with retrieving
 * or if the value is null, the default value will be returned.
 */
fun <T : Any> ServerConfig?.getFlagValueOrDefault(key: FlagKey<T>): T {
    val defaultValue = key.defaultValue
    if (!key.isRemotelyConfigured) return key.defaultValue
    return this
        ?.serverData
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
