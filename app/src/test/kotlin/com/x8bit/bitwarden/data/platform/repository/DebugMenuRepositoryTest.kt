package com.x8bit.bitwarden.data.platform.repository

import app.cash.turbine.test
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.data.datasource.disk.model.ServerConfig
import com.bitwarden.data.repository.ServerConfigRepository
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.platform.datasource.disk.FeatureFlagOverrideDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DebugMenuRepositoryTest {
    private val mockFeatureFlagOverrideDiskSource = mockk<FeatureFlagOverrideDiskSource> {
        every { getFeatureFlag(FlagKey.DummyBoolean) } returns true
        every { getFeatureFlag(FlagKey.DummyString) } returns TEST_STRING_VALUE
        every { getFeatureFlag(FlagKey.DummyInt) } returns TEST_INT_VALUE
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
        assertEquals(TEST_STRING_VALUE, debugMenuRepository.getFeatureFlag(FlagKey.DummyString))
    }

    @Test
    fun `getFeatureFlag should return the feature flag int value from disk`() {
        assertEquals(TEST_INT_VALUE, debugMenuRepository.getFeatureFlag(FlagKey.DummyInt))
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
                    FlagKey.CredentialExchangeProtocolImport,
                    FlagKey.CredentialExchangeProtocolImport.defaultValue,
                )
            }
            debugMenuRepository.featureFlagOverridesUpdatedFlow.test {
                awaitItem() // initial value on subscription
                awaitItem()
                expectNoEvents()
            }
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

    @Test
    fun `resetCoachMarkTourStatuses calls settings disk source setting values back to null`() {
        every {
            mockSettingsDiskSource.storeShouldShowGeneratorCoachMark(shouldShow = any())
        } just runs
        every {
            mockSettingsDiskSource.storeShouldShowAddLoginCoachMark(shouldShow = any())
        } just runs

        debugMenuRepository.resetCoachMarkTourStatuses()

        verify(exactly = 1) {
            mockSettingsDiskSource.storeShouldShowGeneratorCoachMark(shouldShow = null)
            mockSettingsDiskSource.storeShouldShowAddLoginCoachMark(shouldShow = null)
        }
    }
}

private const val TEST_STRING_VALUE = "test"
private const val TEST_INT_VALUE = 100
