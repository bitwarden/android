package com.bitwarden.authenticator.data.platform.datasource.disk

import com.bitwarden.authenticator.data.platform.datasource.disk.model.FeatureFlagsConfiguration
import kotlinx.coroutines.flow.Flow

/**
 * Primary access point for feature flag configuration.
 */
interface FeatureFlagDiskSource {

    /**
     * The currently persisted [FeatureFlagsConfiguration].
     */
    var featureFlagsConfiguration: FeatureFlagsConfiguration?

    /**
     * Emits updates to track [FeatureFlagsConfiguration]. This will replay the last known value.
     */
    val featureFlagsConfigurationFlow: Flow<FeatureFlagsConfiguration?>
}
