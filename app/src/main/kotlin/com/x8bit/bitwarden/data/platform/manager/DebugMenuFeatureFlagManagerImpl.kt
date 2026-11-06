package com.x8bit.bitwarden.data.platform.manager

import com.bitwarden.core.data.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.repository.DebugMenuRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The [FeatureFlagManager] implementation for the debug menu. This manager uses the
 * values returned from the [debugMenuRepository] if they are available. otherwise it will use
 * the default [FeatureFlagManager]. [FlagKey.Vfo1Foundation] defaults to `true` when no override
 * is stored.
 */
class DebugMenuFeatureFlagManagerImpl(
    private val defaultFeatureFlagManager: FeatureFlagManager,
    private val debugMenuRepository: DebugMenuRepository,
) : FeatureFlagManager by defaultFeatureFlagManager {

    override fun <T : Any> getFeatureFlagFlow(key: FlagKey<T>): Flow<T> {
        return debugMenuRepository.featureFlagOverridesUpdatedFlow.map { _ ->
            getOverrideOrDebugDefault(key)
                ?: defaultFeatureFlagManager.getFeatureFlag(key = key)
        }
    }

    override suspend fun <T : Any> getFeatureFlag(key: FlagKey<T>, forceRefresh: Boolean): T {
        return getOverrideOrDebugDefault(key)
            ?: defaultFeatureFlagManager.getFeatureFlag(key = key, forceRefresh = forceRefresh)
    }

    override fun <T : Any> getFeatureFlag(key: FlagKey<T>): T {
        return getOverrideOrDebugDefault(key)
            ?: defaultFeatureFlagManager.getFeatureFlag(key = key)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> getOverrideOrDebugDefault(key: FlagKey<T>): T? =
        debugMenuRepository.getFeatureFlag(key)
            ?: (true as T).takeIf { key == FlagKey.Vfo1Foundation }
}
