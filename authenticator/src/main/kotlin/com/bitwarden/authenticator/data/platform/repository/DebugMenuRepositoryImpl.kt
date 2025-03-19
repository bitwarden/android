package com.bitwarden.authenticator.data.platform.repository

import com.bitwarden.authenticator.BuildConfig
import com.bitwarden.authenticator.data.platform.datasource.disk.FeatureFlagOverrideDiskSource
import com.bitwarden.authenticator.data.platform.manager.getFlagValueOrDefault
import com.bitwarden.authenticator.data.platform.manager.model.FlagKey
import com.bitwarden.authenticator.data.platform.repository.util.bufferedMutableSharedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onSubscription

/**
 * Default implementation of the [DebugMenuRepository]
 */
class DebugMenuRepositoryImpl(
    private val featureFlagOverrideDiskSource: FeatureFlagOverrideDiskSource,
    private val serverConfigRepository: ServerConfigRepository,
) : DebugMenuRepository {

    private val mutableOverridesUpdatedFlow = bufferedMutableSharedFlow<Unit>(replay = 1)
    override val featureFlagOverridesUpdatedFlow: Flow<Unit> = mutableOverridesUpdatedFlow
        .onSubscription { emit(Unit) }

    override val isDebugMenuEnabled: Boolean
        get() = BuildConfig.HAS_DEBUG_MENU

    override fun <T : Any> updateFeatureFlag(key: FlagKey<T>, value: T) {
        featureFlagOverrideDiskSource.saveFeatureFlag(key = key, value = value)
        mutableOverridesUpdatedFlow.tryEmit(Unit)
    }

    override fun <T : Any> getFeatureFlag(key: FlagKey<T>): T? =
        featureFlagOverrideDiskSource.getFeatureFlag(
            key = key,
        )

    override fun resetFeatureFlagOverrides() {
        val currentServerConfig = serverConfigRepository.serverConfigStateFlow.value
        FlagKey.activeFlags.forEach { flagKey ->
            updateFeatureFlag(
                flagKey,
                currentServerConfig.getFlagValueOrDefault(flagKey),
            )
        }
    }
}
