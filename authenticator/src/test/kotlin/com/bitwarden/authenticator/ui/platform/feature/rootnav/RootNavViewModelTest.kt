package com.bitwarden.authenticator.ui.platform.feature.rootnav

import app.cash.turbine.test
import com.bitwarden.authenticator.data.auth.repository.AuthRepository
import com.bitwarden.authenticator.data.platform.manager.lock.model.AppLockState
import com.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RootNavViewModelTest : BaseViewModelTest() {

    private val mutableAppLockStateFlow = MutableStateFlow(AppLockState.LOCKED)
    private val mutableHasSeenWelcomeTutorialFlow = MutableStateFlow(false)
    private val authRepository: AuthRepository = mockk {
        every { appLockStateFlow } returns mutableAppLockStateFlow
        every { updateLastActiveTime() } just runs
        every { isUnlockWithBiometricsEnabled } returns false
        every { clearBiometrics() } just runs
    }
    private val settingsRepository: SettingsRepository = mockk {
        every { hasSeenWelcomeTutorial } answers { mutableHasSeenWelcomeTutorialFlow.value }
        every { hasSeenWelcomeTutorialFlow } returns mutableHasSeenWelcomeTutorialFlow
    }

    @Test
    fun `on BackStackUpdate should call updateLastActiveTime`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(RootNavAction.BackStackUpdate)
        verify(exactly = 1) { authRepository.updateLastActiveTime() }
    }

    @Test
    fun `on BiometricSupportChanged with false should clear biometrics key`() {
        val viewModel = createViewModel()

        viewModel.trySendAction(RootNavAction.BiometricSupportChanged(false))

        verify(exactly = 1) { authRepository.clearBiometrics() }
    }

    @Test
    fun `on BiometricSupportChanged with true should not clear biometrics key`() {
        val viewModel = createViewModel()

        viewModel.trySendAction(RootNavAction.BiometricSupportChanged(true))

        verify(exactly = 0) { authRepository.clearBiometrics() }
    }

    @Test
    fun `on BiometricSupportChanged with false when Locked should clear biometrics`() =
        runTest {
            val viewModel = createViewModel()

            // Send BiometricSupportChanged with false
            viewModel.trySendAction(RootNavAction.BiometricSupportChanged(false))

            verify(exactly = 1) { authRepository.clearBiometrics() }
        }

    @Test
    fun `updates from StateReceived should update the navState accordingly`() =
        runTest {
            mutableHasSeenWelcomeTutorialFlow.update { false }
            mutableAppLockStateFlow.update { AppLockState.LOCKED }
            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                assertEquals(
                    DEFAULT_STATE.copy(navState = RootNavState.NavState.Tutorial),
                    awaitItem(),
                )

                mutableHasSeenWelcomeTutorialFlow.update { true }
                assertEquals(
                    DEFAULT_STATE.copy(navState = RootNavState.NavState.Locked),
                    awaitItem(),
                )

                mutableAppLockStateFlow.update { AppLockState.UNLOCKED }
                assertEquals(
                    DEFAULT_STATE.copy(navState = RootNavState.NavState.Unlocked),
                    awaitItem(),
                )
            }
        }

    private fun createViewModel(): RootNavViewModel = RootNavViewModel(
        authRepository = authRepository,
        settingsRepository = settingsRepository,
    )
}

private val DEFAULT_STATE: RootNavState = RootNavState(
    navState = RootNavState.NavState.Splash,
)
