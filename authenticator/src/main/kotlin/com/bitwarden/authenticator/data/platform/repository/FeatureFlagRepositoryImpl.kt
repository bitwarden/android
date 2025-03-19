package com.bitwarden.authenticator.data.platform.repository

import com.bitwarden.authenticator.data.platform.datasource.disk.FeatureFlagDiskSource
import com.bitwarden.authenticator.data.platform.datasource.disk.model.FeatureFlagsConfiguration
import com.bitwarden.authenticator.data.platform.manager.DispatcherManager
import com.bitwarden.authenticator.data.platform.manager.model.FlagKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.JsonPrimitive

/**
 * Primary implementation of [FeatureFlagRepositoryImpl].
 */
class FeatureFlagRepositoryImpl(
    private val featureFlagDiskSource: FeatureFlagDiskSource,
    dispatcherManager: DispatcherManager,
) : FeatureFlagRepository {

    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    override val featureFlagConfigStateFlow: StateFlow<FeatureFlagsConfiguration?>
        get() = featureFlagDiskSource
            .featureFlagsConfigurationFlow
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = featureFlagDiskSource.featureFlagsConfiguration,
            )

    override suspend fun getFeatureFlagsConfiguration() =
        featureFlagDiskSource.featureFlagsConfiguration
            ?: initLocalFeatureFlagsConfiguration()

    private fun initLocalFeatureFlagsConfiguration(): FeatureFlagsConfiguration {
        val config = FeatureFlagsConfiguration(
            mapOf(
                FlagKey.BitwardenAuthenticationEnabled.keyName to JsonPrimitive(
                    FlagKey.BitwardenAuthenticationEnabled.defaultValue,
                ),
            ),
        )
        featureFlagDiskSource.featureFlagsConfiguration = config
        return config
    }
}
