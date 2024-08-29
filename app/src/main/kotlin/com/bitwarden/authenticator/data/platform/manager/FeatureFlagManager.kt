package com.bitwarden.authenticator.data.platform.manager

import com.bitwarden.authenticator.data.platform.manager.model.FeatureFlag
import kotlinx.coroutines.flow.Flow

/**
 * Manages the available feature flags for the Bitwarden application.
 */
interface FeatureFlagManager {

    /**
     * Returns a flow emitting the value of flag [key] which is of generic type [T].
     * If the value of the flag cannot be retrieved, the default value of [key] will be returned
     */
    fun <T : Any> getFeatureFlagFlow(key: FeatureFlag<T>): Flow<T>

    /**
     * Gets the value for feature flag with [key] and returns it as generic type [T].
     * If no value is found the given [key] its [FeatureFlag.defaultValue] will be returned.
     */
    fun <T : Any> getFeatureFlag(
        key: FeatureFlag<T>,
    ): T
}
