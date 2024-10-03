package com.x8bit.bitwarden.data.platform.repository

import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.platform.datasource.disk.FeatureFlagOverrideDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.model.ServerConfig
import com.x8bit.bitwarden.data.platform.datasource.network.model.ConfigResponseJson
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DebugMenuRepositoryTest {
    private val mockFeatureFlagOverrideDiskSource = mockk<FeatureFlagOverrideDiskSource> {
        every { getFeatureFlag(FlagKey.DummyBoolean) } returns true
        every { getFeatureFlag(FlagKey.DummyString) } returns TEST_STRING_VALUE
        every { getFeatureFlag(FlagKey.DummyInt()) } returns TEST_INT_VALUE
        every { saveFeatureFlag(any(), any()) } just runs
    }
    private val mutableServerConfigStateFlow = MutableStateFlow<ServerConfig?>(null)
    private val mockServerConfigRepository = mockk<ServerConfigRepository> {
        every { serverConfigStateFlow } returns mutableServerConfigStateFlow
    }

    private val mockAuthDiskSource = mockk<AuthDiskSource>(relaxed = true) {
        every { storeOnboardingStatus(any(), any()) } just runs
    }

    private val mockSettingsDiskSource = mockk<SettingsDiskSource>(relaxed = true) {
        every { hasUserLoggedInOrCreatedAccount } returns true
        every { hasUserLoggedInOrCreatedAccount = any() } just runs
    }

    private val debugMenuRepository = DebugMenuRepositoryImpl(
        featureFlagOverrideDiskSource = mockFeatureFlagOverrideDiskSource,
        serverConfigRepository = mockServerConfigRepository,
        settingsDiskSource = mockSettingsDiskSource,
        authDiskSource = mockAuthDiskSource,
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
    fun `resetFeatureFlagOverrides should reset flags to default values if they don't exist in server config`() =
        runTest {
            debugMenuRepository.resetFeatureFlagOverrides()
            verify(exactly = 1) {
                mockFeatureFlagOverrideDiskSource.saveFeatureFlag(
                    FlagKey.EmailVerification,
                    FlagKey.EmailVerification.defaultValue,
                )
                mockFeatureFlagOverrideDiskSource.saveFeatureFlag(
                    FlagKey.OnboardingCarousel,
                    FlagKey.OnboardingCarousel.defaultValue,
                )
                mockFeatureFlagOverrideDiskSource.saveFeatureFlag(
                    FlagKey.OnboardingFlow,
                    FlagKey.OnboardingFlow.defaultValue,
                )
            }
            debugMenuRepository.featureFlagOverridesUpdatedFlow.test {
                awaitItem() // initial value on subscription
                awaitItem()
                expectNoEvents()
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `resetFeatureFlagOverrides should save all feature flags to values from the server config if remote configured is on`() =
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
            mutableServerConfigStateFlow.value = mockServerConfig

            debugMenuRepository.resetFeatureFlagOverrides()

            assertTrue(FlagKey.EmailVerification.isRemotelyConfigured)
            assertFalse(FlagKey.OnboardingCarousel.isRemotelyConfigured)
            verify(exactly = 1) {
                mockFeatureFlagOverrideDiskSource.saveFeatureFlag(FlagKey.EmailVerification, true)
                mockFeatureFlagOverrideDiskSource.saveFeatureFlag(
                    FlagKey.OnboardingCarousel,
                    false,
                )
                mockFeatureFlagOverrideDiskSource.saveFeatureFlag(
                    FlagKey.OnboardingFlow,
                    false,
                )
            }

            debugMenuRepository.featureFlagOverridesUpdatedFlow.test {
                awaitItem() // initial value on subscription
                awaitItem()
                cancel()
            }
            unmockkStatic(FlagKey.OnboardingFlow::class)
        }

    @Test
    fun `resetOnboardingStatusForCurrentUser should set the onboarding status to NOT_STARTED`() {
        val userId = "testUserId"
        val mockUserStateJson = mockk<UserStateJson>(relaxed = true) {
            every { activeUserId } returns userId
        }
        every { mockUserStateJson.activeUserId } returns userId
        every { mockAuthDiskSource.userState } returns mockUserStateJson
        debugMenuRepository.resetOnboardingStatusForCurrentUser()
        verify {
            mockAuthDiskSource.storeOnboardingStatus(
                userId = userId,
                onboardingStatus = OnboardingStatus.NOT_STARTED,
            )
        }
    }

    @Test
    fun `resetOnboardingStatusForCurrentUser should do nothing if no active user`() {
        every { mockAuthDiskSource.userState } returns null
        debugMenuRepository.resetOnboardingStatusForCurrentUser()
        verify(exactly = 0) {
            mockAuthDiskSource.storeOnboardingStatus(
                userId = any(),
                onboardingStatus = OnboardingStatus.NOT_STARTED,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `modifyStateToShowOnboardingCarousel should set hasUserLoggedInOrCreatedAccount to false and trigger user state update`() {
        var lambdaHasBeenCalled = false
        val triggerUserStateUpdate = {
            lambdaHasBeenCalled = true
        }
        debugMenuRepository.modifyStateToShowOnboardingCarousel(triggerUserStateUpdate)
        verify {
            mockSettingsDiskSource.hasUserLoggedInOrCreatedAccount = false
        }
        assertTrue(lambdaHasBeenCalled)
    }
}

private const val TEST_STRING_VALUE = "test"
private const val TEST_INT_VALUE = 100
