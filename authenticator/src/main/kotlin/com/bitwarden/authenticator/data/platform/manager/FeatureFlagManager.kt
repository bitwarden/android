package com.bitwarden.authenticator.data.platform.manager

import com.bitwarden.authenticator.data.platform.manager.model.FlagKey
import kotlinx.coroutines.flow.Flow

/**
 * Manages the available feature flags for the Bitwarden application.
 */
interface FeatureFlagManager {

    /**
     * Returns a flow emitting the value of flag [key] which is of generic type [T].
     * If the value of the flag cannot be retrieved, the default value of [key] will be returned
     */
    fun <T : Any> getFeatureFlagFlow(key: FlagKey<T>): Flow<T>

    /**
     * Get value for feature flag with [key] and returns it as generic type [T].
     * If no value is found the given [key] its default value will be returned.
     * Cached flags can be invalidated with [forceRefresh]
     */
    suspend fun <T : Any> getFeatureFlag(
        key: FlagKey<T>,
        forceRefresh: Boolean,
    ): T

    /**
     * Gets the value for feature flag with [key] and returns it as generic type [T].
     * If no value is found the given [key] its [FlagKey.defaultValue] will be returned.
     */
    fun <T : Any> getFeatureFlag(
        key: FlagKey<T>,
    ): T
}
