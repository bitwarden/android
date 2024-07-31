package com.x8bit.bitwarden.data.platform.manager

import kotlinx.coroutines.flow.StateFlow

/**
 * Manages the available feature flags for the Bitwarden application.
 */
interface FeatureFlagManager {
    /**
     * Returns a map of constant feature flags that are only used locally.
     */
    val featureFlagsLocal: Map<String, Boolean>

    /**
     * Emits updates that track server-related feature flags.
     */
    val featureFlagsServerStateFlow: StateFlow<Map<String, String>?>

    /**
     * Get value for feature flag with [FlagKey] and returns it as [Boolean].
     * If no value is found the the given key [defaultValue] will be returned.
     * Cached flags can be invalidated with [forceRefresh]
     */
    suspend fun getFeatureFlag(
        key: FlagKey,
        defaultValue: Boolean = false,
        forceRefresh: Boolean = false,
    ): Boolean

    /**
     * Get value for feature flag with [FlagKey] and returns it as [Int].
     * If no value is found the the given key [defaultValue] will be returned.
     * Cached flags can be invalidated with [forceRefresh]
     */
    suspend fun getFeatureFlag(
        key: FlagKey,
        defaultValue: Int = 0,
        forceRefresh: Boolean = false,
    ): Int

    /**
     * Get value for feature flag with [FlagKey] and returns it as [String].
     * If no value is found the the given key [defaultValue] will be returned.
     * Cached flags can be invalidated with [forceRefresh]
     */
    suspend fun getFeatureFlag(
        key: FlagKey,
        defaultValue: String? = null,
        forceRefresh: Boolean = false,
    ): String?
}
