package com.bitwarden.authenticator.data.platform.datasource.disk.util

import com.bitwarden.authenticator.data.platform.datasource.disk.FeatureFlagDiskSource
import com.bitwarden.authenticator.data.platform.datasource.disk.model.FeatureFlagsConfiguration
import com.bitwarden.authenticator.data.platform.repository.util.bufferedMutableSharedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onSubscription

/**
 * A faked implementation of [FeatureFlagDiskSource] for testing.
 */
class FakeFeatureFlagDiskSource : FeatureFlagDiskSource {

    private var configuration: FeatureFlagsConfiguration? = null
    private val mutableConfigurationFlow =
        bufferedMutableSharedFlow<FeatureFlagsConfiguration?>(replay = 1)

    override var featureFlagsConfiguration: FeatureFlagsConfiguration?
        get() = configuration
        set(value) {
            configuration = value
            mutableConfigurationFlow.tryEmit(value)
        }
    override val featureFlagsConfigurationFlow: Flow<FeatureFlagsConfiguration?>
        get() = mutableConfigurationFlow
            .onSubscription { emit(configuration) }
}
