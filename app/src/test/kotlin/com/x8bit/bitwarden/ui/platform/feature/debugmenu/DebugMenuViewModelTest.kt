package com.x8bit.bitwarden.ui.platform.feature.debugmenu

import app.cash.turbine.test
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.LogsManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.repository.DebugMenuRepository
import com.x8bit.bitwarden.data.util.assertCoroutineThrows
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DebugMenuViewModelTest : BaseViewModelTest() {

    private val mockFeatureFlagManager = mockk<FeatureFlagManager>(relaxed = true) {
        every { getFeatureFlagFlow<Boolean>(any()) } returns flowOf(true)
    }

    private val mockAuthRepository = mockk<AuthRepository>(relaxed = true) {
        every { hasPendingAccountAddition = true } just runs
    }

    private val mockDebugMenuRepository = mockk<DebugMenuRepository>(relaxed = true) {
        coEvery { resetFeatureFlagOverrides() } just runs
        every { updateFeatureFlag<Boolean>(any(), any()) } just runs
        every { resetOnboardingStatusForCurrentUser() } just runs
        every {
            modifyStateToShowOnboardingCarousel(userStateUpdateTrigger = any())
        } answers {
            // invokes the passed in lambda, allowing verification in tests.
            firstArg<() -> Unit>().invoke()
        }
    }

    private val logsManager = mockk<LogsManager> {
        every { trackNonFatalException(throwable = any()) } just runs
    }

    @Test
    fun `initial state should be correct`() {
        val viewModel = createViewModel()
        assertEquals(viewModel.stateFlow.value, DEFAULT_STATE)
    }

    @Test
    fun `handleUpdateFeatureFlag should update the feature flag`() {
        val viewModel = createViewModel()
        assertEquals(viewModel.stateFlow.value, DEFAULT_STATE)
        viewModel.trySendAction(
            DebugMenuAction.Internal.UpdateFeatureFlagMap(UPDATED_MAP_VALUE),
        )
        assertEquals(viewModel.stateFlow.value, DebugMenuState(UPDATED_MAP_VALUE))
    }

    @Test
    fun `handleResetFeatureFlagValues should reset the feature flag values`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(DebugMenuAction.ResetFeatureFlagValues)
        coVerify(exactly = 1) { mockDebugMenuRepository.resetFeatureFlagOverrides() }
    }

    @Test
    fun `handleNavigateBack should send NavigateBack event`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(DebugMenuAction.NavigateBack)
        viewModel.eventFlow.test {
            assertEquals(DebugMenuEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `GenerateCrashClick should throw an IllegalStateException`() {
        val viewModel = createViewModel()
        assertCoroutineThrows<IllegalStateException> {
            viewModel.trySendAction(DebugMenuAction.GenerateCrashClick)
        }
    }

    @Test
    fun `GenerateErrorReportClick should log an IllegalStateException`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(DebugMenuAction.GenerateErrorReportClick)
        verify(exactly = 1) {
            logsManager.trackNonFatalException(throwable = any())
        }
    }

    @Test
    fun `handleUpdateFeatureFlag should update the feature flag via the repository`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(
            DebugMenuAction.UpdateFeatureFlag(FlagKey.EmailVerification, false),
        )
        verify(exactly = 1) {
            mockDebugMenuRepository.updateFeatureFlag(FlagKey.EmailVerification, false)
        }
    }

    @Test
    fun `handleResetOnboardingStatus should reset the onboarding status`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(DebugMenuAction.RestartOnboarding)
        verify(exactly = 1) { mockDebugMenuRepository.resetOnboardingStatusForCurrentUser() }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `handleResetOnboardingCarousel should reset the onboarding carousel and update user state pending account action`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(DebugMenuAction.RestartOnboardingCarousel)
        verify(exactly = 1) {
            mockDebugMenuRepository.modifyStateToShowOnboardingCarousel(any())
            mockAuthRepository.hasPendingAccountAddition = true
        }
    }

    @Test
    fun `handleResetCoachMarkTourStatuses should call repository to reset values`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(DebugMenuAction.ResetCoachMarkTourStatuses)
        verify(exactly = 1) {
            mockDebugMenuRepository.resetCoachMarkTourStatuses()
        }
    }

    private fun createViewModel(): DebugMenuViewModel = DebugMenuViewModel(
        featureFlagManager = mockFeatureFlagManager,
        debugMenuRepository = mockDebugMenuRepository,
        authRepository = mockAuthRepository,
        logsManager = logsManager,
    )
}

private val DEFAULT_MAP_VALUE: ImmutableMap<FlagKey<Any>, Any> = persistentMapOf(
    FlagKey.EmailVerification to true,
    FlagKey.ImportLoginsFlow to true,
    FlagKey.CredentialExchangeProtocolImport to true,
    FlagKey.CredentialExchangeProtocolExport to true,
    FlagKey.MutualTls to true,
    FlagKey.SingleTapPasskeyCreation to true,
    FlagKey.SingleTapPasskeyAuthentication to true,
    FlagKey.AnonAddySelfHostAlias to true,
    FlagKey.SimpleLoginSelfHostAlias to true,
    FlagKey.ChromeAutofill to true,
    FlagKey.MobileErrorReporting to true,
    FlagKey.RestrictCipherItemDeletion to true,
    FlagKey.UserManagedPrivilegedApps to true,
    FlagKey.RemoveCardPolicy to true,
)

private val UPDATED_MAP_VALUE: ImmutableMap<FlagKey<Any>, Any> = persistentMapOf(
    FlagKey.EmailVerification to false,
    FlagKey.ImportLoginsFlow to false,
    FlagKey.CredentialExchangeProtocolImport to false,
    FlagKey.CredentialExchangeProtocolExport to false,
    FlagKey.MutualTls to false,
    FlagKey.SingleTapPasskeyCreation to false,
    FlagKey.SingleTapPasskeyAuthentication to false,
    FlagKey.AnonAddySelfHostAlias to false,
    FlagKey.SimpleLoginSelfHostAlias to false,
    FlagKey.ChromeAutofill to false,
    FlagKey.MobileErrorReporting to false,
    FlagKey.RestrictCipherItemDeletion to false,
    FlagKey.UserManagedPrivilegedApps to false,
    FlagKey.RemoveCardPolicy to false,
)

private val DEFAULT_STATE = DebugMenuState(
    featureFlags = DEFAULT_MAP_VALUE,
)
