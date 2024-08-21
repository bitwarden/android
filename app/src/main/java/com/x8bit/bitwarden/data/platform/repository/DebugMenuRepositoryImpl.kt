package com.x8bit.bitwarden.data.platform.repository

import com.x8bit.bitwarden.BuildConfig
import com.x8bit.bitwarden.data.platform.datasource.disk.FeatureFlagOverrideDiskSource
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.serialization.json.JsonPrimitive

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

    override suspend fun resetFeatureFlagOverrides() {
        serverConfigRepository
            .getServerConfig(
                forceRefresh = true,
            )
            ?.serverData
            ?.featureStates
            ?.forEach { (key, value) ->
                FlagKey
                    .activeFlags
                    .find { it.keyName == key }
                    ?.let { flagKey ->
                        updateKeyFromNetwork(flagKey, value)
                    }
            }
    }

    private fun updateKeyFromNetwork(
        flagKey: FlagKey<*>,
        value: JsonPrimitive,
    ) {
        try {
            when (flagKey.defaultValue) {
                is Boolean -> updateFeatureFlag(flagKey, value.content.toBoolean())
                is String -> updateFeatureFlag(flagKey, value.content)
                is Int -> updateFeatureFlag(flagKey, value.content.toInt())
                else -> Unit
            }
        } catch (_: ClassCastException) {
            // No-op
        } catch (_: NumberFormatException) {
            // No-op
        }
    }
}
