package com.bitwarden.authenticator.data.platform.repository.util

import com.bitwarden.authenticator.data.platform.datasource.disk.model.FeatureFlagsConfiguration
import com.bitwarden.authenticator.data.platform.manager.model.LocalFeatureFlag
import com.bitwarden.authenticator.data.platform.repository.FeatureFlagRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonPrimitive

/**
 * Faked implementation of [FeatureFlagRepository] for testing.
 */
class FakeFeatureFlagRepository : FeatureFlagRepository {
    var featureFlagsConfiguration: FeatureFlagsConfiguration?
        get() = mutableFeatureFlagsConfiguration.value
        set(value) {
            mutableFeatureFlagsConfiguration.value = value
        }

    private val mutableFeatureFlagsConfiguration =
        MutableStateFlow<FeatureFlagsConfiguration?>(FEATURE_FLAGS_CONFIG)

    override val featureFlagConfigStateFlow: StateFlow<FeatureFlagsConfiguration?> =
        mutableFeatureFlagsConfiguration

    override suspend fun getFeatureFlagsConfiguration(): FeatureFlagsConfiguration {
        return featureFlagsConfiguration
            ?: FEATURE_FLAGS_CONFIG
    }
}

private val FEATURE_FLAGS_CONFIG =
    FeatureFlagsConfiguration(
        featureFlags = mapOf(
            LocalFeatureFlag.BitwardenAuthenticationEnabled.name to
                JsonPrimitive(LocalFeatureFlag.BitwardenAuthenticationEnabled.defaultValue),
        ),
    )
