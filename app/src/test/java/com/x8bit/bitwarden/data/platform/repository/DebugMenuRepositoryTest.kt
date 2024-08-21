package com.x8bit.bitwarden.data.platform.repository

import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.datasource.disk.FeatureFlagOverrideDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.model.ServerConfig
import com.x8bit.bitwarden.data.platform.datasource.network.model.ConfigResponseJson
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DebugMenuRepositoryTest {
    private val mockFeatureFlagOverrideDiskSource = mockk<FeatureFlagOverrideDiskSource>() {
        every { getFeatureFlag(FlagKey.DummyBoolean) } returns true
        every { getFeatureFlag(FlagKey.DummyString) } returns TEST_STRING_VALUE
        every { getFeatureFlag(FlagKey.DummyInt()) } returns TEST_INT_VALUE
        every { saveFeatureFlag(any(), any()) } just runs
    }
    private val mockServerConfigRepository = mockk<ServerConfigRepository>() {
        coEvery { getServerConfig(any()) } returns null
    }

    private val debugMenuRepository = DebugMenuRepositoryImpl(
        featureFlagOverrideDiskSource = mockFeatureFlagOverrideDiskSource,
        serverConfigRepository = mockServerConfigRepository,
    )

    @Test
    fun `updateFeatureFlag should save the feature flag to disk`() {
        debugMenuRepository.updateFeatureFlag(FlagKey.DummyBoolean, true)
        verify(exactly = 1) {
            mockFeatureFlagOverrideDiskSource.saveFeatureFlag(
                FlagKey.DummyBoolean,
                true,
            )
        }
    }

    @Test
    fun `updateFeatureFlag should cause the feature flag overrides updated flow to emit`() =
        runTest {
            debugMenuRepository.updateFeatureFlag(FlagKey.DummyBoolean, true)
            debugMenuRepository.featureFlagOverridesUpdatedFlow.test {
                awaitItem() // initial value on subscription
                awaitItem()
                cancel()
            }
        }

    @Test
    fun `getFeatureFlag should return the feature flag boolean value from disk`() {
        assertTrue(debugMenuRepository.getFeatureFlag(FlagKey.DummyBoolean)!!)
    }

    @Test
    fun `getFeatureFlag should return the feature flag string value from disk`() {
        assertEquals(TEST_STRING_VALUE, debugMenuRepository.getFeatureFlag(FlagKey.DummyString)!!)
    }

    @Test
    fun `getFeatureFlag should return the feature flag int value from disk`() {
        assertEquals(TEST_INT_VALUE, debugMenuRepository.getFeatureFlag(FlagKey.DummyInt())!!)
    }

    @Test
    fun `getFeatureFlag should return null if the feature flag does not exist in disk`() {
        every { mockFeatureFlagOverrideDiskSource.getFeatureFlag<Boolean>(any()) } returns null
        assertNull(debugMenuRepository.getFeatureFlag(FlagKey.DummyBoolean))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `resetFeatureFlagOverrides should not update flags or emit if there server config is null`() =
        runTest {
            debugMenuRepository.resetFeatureFlagOverrides()
            verify(exactly = 0) {
                mockFeatureFlagOverrideDiskSource.saveFeatureFlag(any(), any())
            }
            debugMenuRepository.featureFlagOverridesUpdatedFlow.test {
                awaitItem() // initial value on subscription
                expectNoEvents()
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `resetFeatureFlagOverrides should save all feature flags to values from the server config`() =
        runTest {
            val mockServerData = mockk<ConfigResponseJson>(relaxed = true) {
                every { featureStates } returns mapOf(
                    FlagKey.EmailVerification.keyName to JsonPrimitive(true),
                    FlagKey.OnboardingCarousel.keyName to JsonPrimitive(false),
                    FlagKey.OnboardingFlow.keyName to JsonPrimitive(true),
                )
            }
            val mockServerConfig = mockk<ServerConfig>(relaxed = true) {
                every { serverData } returns mockServerData
            }
            coEvery { mockServerConfigRepository.getServerConfig(any()) } returns mockServerConfig

            debugMenuRepository.resetFeatureFlagOverrides()
            verify(exactly = 1) {
                mockFeatureFlagOverrideDiskSource.saveFeatureFlag(FlagKey.EmailVerification, true)
                mockFeatureFlagOverrideDiskSource.saveFeatureFlag(
                    FlagKey.OnboardingCarousel,
                    false,
                )
                mockFeatureFlagOverrideDiskSource.saveFeatureFlag(
                    FlagKey.OnboardingFlow,
                    true,
                )
            }

            debugMenuRepository.featureFlagOverridesUpdatedFlow.test {
                awaitItem() // initial value on subscription
                awaitItem()
                cancel()
            }
        }
}

private const val TEST_STRING_VALUE = "test"
private const val TEST_INT_VALUE = 100
