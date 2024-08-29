package com.bitwarden.authenticator.data.platform.manager

import com.bitwarden.authenticator.data.platform.datasource.disk.model.FeatureFlagsConfiguration
import com.bitwarden.authenticator.data.platform.manager.model.FeatureFlag
import com.bitwarden.authenticator.data.platform.repository.FeatureFlagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Primary implementation of [FeatureFlagManager].
 */
class FeatureFlagManagerImpl(
    private val featureFlagRepository: FeatureFlagRepository,
) : FeatureFlagManager {

    override fun <T : Any> getFeatureFlagFlow(key: FeatureFlag<T>): Flow<T> =
        featureFlagRepository
            .featureFlagConfigStateFlow
            .map { serverConfig ->
                serverConfig.getFlagValueOrDefault(key = key)
            }

    override fun <T : Any> getFeatureFlag(key: FeatureFlag<T>): T =
        featureFlagRepository
            .featureFlagConfigStateFlow
            .value
            .getFlagValueOrDefault(key = key)
}

private fun <T : Any> FeatureFlagsConfiguration?.getFlagValueOrDefault(key: FeatureFlag<T>): T {
    val defaultValue = key.defaultValue
    return this
        ?.featureFlags
        ?.get(key.name)
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
