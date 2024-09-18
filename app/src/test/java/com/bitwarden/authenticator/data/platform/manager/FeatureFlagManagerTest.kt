package com.bitwarden.authenticator.data.platform.manager

import app.cash.turbine.test
import com.bitwarden.authenticator.data.platform.datasource.disk.model.FeatureFlagsConfiguration
import com.bitwarden.authenticator.data.platform.manager.model.LocalFeatureFlag
import com.bitwarden.authenticator.data.platform.repository.util.FakeFeatureFlagRepository
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue

class FeatureFlagManagerTest {
    private val fakeFeatureFlagRepository = FakeFeatureFlagRepository()

    private val featureFlagManager = FeatureFlagManagerImpl(
        featureFlagRepository = fakeFeatureFlagRepository,
    )

    @Test
    fun `FeatureFlagRepository flow with value should trigger new flags`() = runTest {
        fakeFeatureFlagRepository.featureFlagsConfiguration = null
        assertNull(
            fakeFeatureFlagRepository.featureFlagsConfiguration,
        )

        fakeFeatureFlagRepository.featureFlagsConfiguration = FEATURE_FLAGS_CONFIG

        featureFlagManager
            .getFeatureFlagFlow(LocalFeatureFlag.BitwardenAuthenticationEnabled)
            .test {
                assertNotNull(awaitItem())
            }
    }

    @Test
    fun `getFeatureFlag should return value if exists`() = runTest {
        fakeFeatureFlagRepository.featureFlagsConfiguration = FEATURE_FLAGS_CONFIG

        val flagValue = featureFlagManager.getFeatureFlag(
            key = LocalFeatureFlag.BitwardenAuthenticationEnabled,
        )

        assertTrue(flagValue)
    }

    @Test
    fun `getFeatureFlag should return default value if flag doesn't exist`() {
        fakeFeatureFlagRepository.featureFlagsConfiguration = FEATURE_FLAGS_CONFIG.copy(
            featureFlags = emptyMap(),
        )

        val flagValue = featureFlagManager.getFeatureFlag(
            LocalFeatureFlag.BitwardenAuthenticationEnabled,
        )

        assertFalse(flagValue)
    }

    @Test
    fun `PasswordManagerSync should default to false`() {
        assertFalse(LocalFeatureFlag.PasswordManagerSync.defaultValue)
    }
}

private val FEATURE_FLAGS_CONFIG =
    FeatureFlagsConfiguration(
        mapOf(
            LocalFeatureFlag.BitwardenAuthenticationEnabled.name to
                JsonPrimitive(true),
        ),
    )
