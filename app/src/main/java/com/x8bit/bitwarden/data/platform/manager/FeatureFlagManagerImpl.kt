package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.platform.datasource.disk.model.ServerConfig
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
}

private fun <T : Any> ServerConfig?.getFlagValueOrDefault(key: FlagKey<T>): T {
    val defaultValue = key.defaultValue
    return this?.serverData
        ?.featureStates
        ?.get(key.stringValue)
        ?.let {
            try {
                // Suppressed since we are checking the type before doing the cast
                @Suppress("UNCHECKED_CAST")
                when (defaultValue::class) {
                    Boolean::class -> it.content.toBoolean() as? T
                    String::class -> it.content as? T
                    Int::class -> it.content.toInt() as? T
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

/**
 * Class to hold feature flag keys.
 * @property [stringValue] corresponds to the string value of a give key
 * @property [defaultValue] corresponds to default value of the flag of type [T]
 */
sealed class FlagKey<out T : Any> {
    abstract val stringValue: String
    abstract val defaultValue: T

    /**
     * Data object holding the key for Email Verification feature
     */
    data object EmailVerification : FlagKey<Boolean>() {
        override val stringValue: String = "email-verification"
        override val defaultValue: Boolean = false
    }

    /**
     * Data object holding the key for an Int flag to be used in tests
     */
    data object DummyInt : FlagKey<Int>() {
        override val stringValue: String = "email-verification"
        override val defaultValue: Int = Int.MIN_VALUE
    }

    /**
     * Data object holding the key for an String flag to be used in tests
     */
    data object DummyString : FlagKey<String>() {
        override val stringValue: String = "email-verification"
        override val defaultValue: String = "defaultValue"
    }
}
