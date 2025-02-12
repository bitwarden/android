package com.bitwarden.authenticator.data.platform.manager

import com.bitwarden.authenticator.data.platform.manager.model.FlagKey
import com.bitwarden.authenticator.data.platform.repository.DebugMenuRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The [FeatureFlagManager] implementation for the debug menu. This manager uses the
 * values returned from the [debugMenuRepository] if they are available. otherwise it will use
 * the default [FeatureFlagManager].
 */
class DebugMenuFeatureFlagManagerImpl(
    private val defaultFeatureFlagManager: FeatureFlagManager,
    private val debugMenuRepository: DebugMenuRepository,
) : FeatureFlagManager by defaultFeatureFlagManager {

    override fun <T : Any> getFeatureFlagFlow(key: FlagKey<T>): Flow<T> {
        return debugMenuRepository.featureFlagOverridesUpdatedFlow.map { _ ->
            debugMenuRepository
                .getFeatureFlag(key)
                ?: defaultFeatureFlagManager.getFeatureFlag(key = key)
        }
    }

    override suspend fun <T : Any> getFeatureFlag(key: FlagKey<T>, forceRefresh: Boolean): T {
        return debugMenuRepository
            .getFeatureFlag(key)
            ?: defaultFeatureFlagManager.getFeatureFlag(key = key, forceRefresh = forceRefresh)
    }

    override fun <T : Any> getFeatureFlag(key: FlagKey<T>): T {
        return debugMenuRepository
            .getFeatureFlag(key)
            ?: defaultFeatureFlagManager.getFeatureFlag(key = key)
    }
}
