package com.bitwarden.authenticator.data.platform.manager

import app.cash.turbine.test
import com.bitwarden.authenticator.data.platform.manager.model.FlagKey
import com.bitwarden.authenticator.data.platform.repository.DebugMenuRepository
import com.bitwarden.authenticator.data.platform.repository.util.bufferedMutableSharedFlow
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DebugMenuFeatureFlagManagerTest {

    private val mockFeatureFlagManager = mockk<FeatureFlagManager>(relaxed = true) {
        every { getFeatureFlag<Boolean>(any()) } returns true
    }

    private val mutableOverridesUpdateFlow = bufferedMutableSharedFlow<Unit>()
    private val mockDebugMenuRepository = mockk<DebugMenuRepository>(relaxed = true) {
        every { updateFeatureFlag(any(), any()) } just runs
        every { featureFlagOverridesUpdatedFlow } returns mutableOverridesUpdateFlow
    }

    private val debugMenuFeatureFlagManager = DebugMenuFeatureFlagManagerImpl(
        defaultFeatureFlagManager = mockFeatureFlagManager,
        debugMenuRepository = mockDebugMenuRepository,
    )

    @Test
    fun `If value exists in repository return that value for requested FlagKey`() {
        val flagKey = FlagKey.DummyBoolean
        val expectedValue = true
        every { mockDebugMenuRepository.getFeatureFlag(flagKey) } returns expectedValue

        assertTrue(debugMenuFeatureFlagManager.getFeatureFlag(flagKey))

        verify(exactly = 0) { mockFeatureFlagManager.getFeatureFlag(flagKey) }
    }

    @Test
    fun `If value does not exist in repository return that value from the default manager`() {
        val flagKey = FlagKey.DummyBoolean

        every { mockDebugMenuRepository.getFeatureFlag(flagKey) } returns null

        assertTrue(debugMenuFeatureFlagManager.getFeatureFlag(flagKey))

        verify(exactly = 1) { mockFeatureFlagManager.getFeatureFlag(flagKey) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `get feature flag with force refresh will call the default manager to use as the fallback value`() =
        runTest {
            val flagKey = FlagKey.DummyBoolean
            val expectedValue = true

            coEvery {
                mockFeatureFlagManager.getFeatureFlag(key = flagKey, forceRefresh = true)
            } returns expectedValue
            every { mockDebugMenuRepository.getFeatureFlag(flagKey) } returns null

            assertTrue(
                debugMenuFeatureFlagManager.getFeatureFlag(
                    key = flagKey,
                    forceRefresh = true,
                ),
            )

            coVerify(exactly = 1) {
                mockFeatureFlagManager.getFeatureFlag(
                    key = flagKey,
                    forceRefresh = true,
                )
            }
        }

    @Test
    fun `when repository update flow emits, the feature flag flow will refresh to the value`() =
        runTest {
            val flagKey = FlagKey.DummyBoolean
            every { mockDebugMenuRepository.getFeatureFlag(flagKey) } returns true

            debugMenuFeatureFlagManager
                .getFeatureFlagFlow(flagKey)
                .test {
                    mutableOverridesUpdateFlow.emit(Unit)
                    assertEquals(true, awaitItem())
                    cancel()
                }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `when repository update flow emits the flow will refresh to the value from default manager if repo returns null`() =
        runTest {
            val flagKey = FlagKey.DummyBoolean
            every { mockDebugMenuRepository.getFeatureFlag(flagKey) } returns null

            debugMenuFeatureFlagManager
                .getFeatureFlagFlow(flagKey)
                .test {
                    mutableOverridesUpdateFlow.emit(Unit)
                    assertEquals(true, awaitItem())
                    cancel()
                }
            verify(exactly = 1) { mockFeatureFlagManager.getFeatureFlag(flagKey) }
        }
}
