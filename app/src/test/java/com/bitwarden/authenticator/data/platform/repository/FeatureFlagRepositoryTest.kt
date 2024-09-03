package com.bitwarden.authenticator.data.platform.repository

import app.cash.turbine.test
import com.bitwarden.authenticator.data.platform.base.FakeDispatcherManager
import com.bitwarden.authenticator.data.platform.datasource.disk.model.FeatureFlagsConfiguration
import com.bitwarden.authenticator.data.platform.datasource.disk.util.FakeFeatureFlagDiskSource
import com.bitwarden.authenticator.data.platform.manager.model.LocalFeatureFlag
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull

class FeatureFlagRepositoryTest {

    private val fakeFeatureFlagDiskSource = FakeFeatureFlagDiskSource()
    private val featureFlagRepo = FeatureFlagRepositoryImpl(
        featureFlagDiskSource = fakeFeatureFlagDiskSource,
        dispatcherManager = FakeDispatcherManager(),
    )

    @Suppress("MaxLineLength")
    @Test
    fun `getFeatureFlagsConfiguration should init configuration with local flags when there is none in state`() =
        runTest {
            assertNull(fakeFeatureFlagDiskSource.featureFlagsConfiguration)

            featureFlagRepo.getFeatureFlagsConfiguration()

            assertEquals(
                FEATURE_FLAGS_CONFIG,
                fakeFeatureFlagDiskSource.featureFlagsConfiguration,
            )
        }

    @Test
    fun `featureFlagsConfigurationFlow should react to feature flag configuration changes`() =
        runTest {
            featureFlagRepo.getFeatureFlagsConfiguration()

            featureFlagRepo.featureFlagConfigStateFlow.test {
                assertEquals(fakeFeatureFlagDiskSource.featureFlagsConfiguration, awaitItem())
            }
        }
}

private val FEATURE_FLAGS_CONFIG =
    FeatureFlagsConfiguration(
        featureFlags = mapOf(
            LocalFeatureFlag.BitwardenAuthenticationEnabled.name to
                JsonPrimitive(LocalFeatureFlag.BitwardenAuthenticationEnabled.defaultValue),
        ),
    )
