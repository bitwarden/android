package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.platform.datasource.disk.model.ServerConfig
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.repository.ServerConfigRepository
import com.x8bit.bitwarden.data.platform.util.isServerVersionAtLeast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val CIPHER_KEY_ENCRYPTION_KEY = "enableCipherKeyEncryption"
private const val CIPHER_KEY_ENC_MIN_SERVER_VERSION = "2024.2.0"

/**
 * Primary implementation of [FeatureFlagManager].
 */
class FeatureFlagManagerImpl(
    private val serverConfigRepository: ServerConfigRepository,
) : FeatureFlagManager {

    override val sdkFeatureFlags: Map<String, Boolean>
        get() = mapOf(
            CIPHER_KEY_ENCRYPTION_KEY to
                getCipherKeyEncryptionFlagState(),
        )

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

    /**
     * Get the computed value of the cipher key encryption flag based on server version and
     * remote flag.
     */
    private fun getCipherKeyEncryptionFlagState() =
        isServerVersionAtLeast(
            serverConfigRepository.serverConfigStateFlow.value,
            CIPHER_KEY_ENC_MIN_SERVER_VERSION,
        ) && getFeatureFlag(FlagKey.CipherKeyEncryption)
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
