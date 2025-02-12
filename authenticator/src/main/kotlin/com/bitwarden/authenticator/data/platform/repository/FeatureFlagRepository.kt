package com.bitwarden.authenticator.data.platform.repository

import com.bitwarden.authenticator.data.platform.datasource.disk.model.FeatureFlagsConfiguration
import kotlinx.coroutines.flow.StateFlow

/**
 * Provides an API for observing the server config state.
 */
interface FeatureFlagRepository {

    /**
     * Emits updates that track [FeatureFlagsConfiguration].
     */
    val featureFlagConfigStateFlow: StateFlow<FeatureFlagsConfiguration?>

    /**
     * Gets the state [FeatureFlagsConfiguration].
     */
    suspend fun getFeatureFlagsConfiguration(): FeatureFlagsConfiguration
}
