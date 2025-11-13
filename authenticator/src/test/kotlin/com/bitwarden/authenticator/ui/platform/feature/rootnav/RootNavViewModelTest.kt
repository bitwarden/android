package com.bitwarden.authenticator.ui.platform.feature.rootnav

import app.cash.turbine.test
import com.bitwarden.authenticator.data.auth.repository.AuthRepository
import com.bitwarden.authenticator.data.platform.manager.BiometricsEncryptionManager
import com.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RootNavViewModelTest : BaseViewModelTest() {

    private val mutableHasSeenWelcomeTutorialFlow = MutableStateFlow(false)
    private val authRepository: AuthRepository = mockk {
        every { updateLastActiveTime() } just runs
    }
    private val settingsRepository: SettingsRepository = mockk {
        every { hasSeenWelcomeTutorial } returns false
        every { hasSeenWelcomeTutorial = any() } just runs
        every { hasSeenWelcomeTutorialFlow } returns mutableHasSeenWelcomeTutorialFlow
        every { isUnlockWithBiometricsEnabled } returns false
        every { clearBiometricsKey() } just runs
    }
    private val biometricsEncryptionManager: BiometricsEncryptionManager = mockk()

    @Test
    fun `initialState should be correct when hasSeenWelcomeTutorial is false`() = runTest {
        every { settingsRepository.hasSeenWelcomeTutorial } returns false
        mutableHasSeenWelcomeTutorialFlow.value = false
        val viewModel = createViewModel()
        // When hasSeenWelcomeTutorial is false, the flow emits and triggers navigation to Tutorial
        assertEquals(
            RootNavState(
                hasSeenWelcomeGuide = false,
                navState = RootNavState.NavState.Tutorial,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `initialState should be correct when hasSeenWelcomeTutorial is true`() = runTest {
        every { settingsRepository.hasSeenWelcomeTutorial } returns true
        mutableHasSeenWelcomeTutorialFlow.value = true
        val viewModel = createViewModel()
        // When hasSeenWelcomeTutorial is true and biometrics is not enabled, navigates to Unlocked
        assertEquals(
            RootNavState(
                hasSeenWelcomeGuide = true,
                navState = RootNavState.NavState.Unlocked,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on BackStackUpdate should call updateLastActiveTime`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(RootNavAction.BackStackUpdate)
        verify(exactly = 1) { authRepository.updateLastActiveTime() }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `on HasSeenWelcomeTutorialChange with true and biometrics enabled and valid should navigate to Locked`() {
        every { settingsRepository.isUnlockWithBiometricsEnabled } returns true
        every { biometricsEncryptionManager.isBiometricIntegrityValid() } returns true
        val viewModel = createViewModel()

        viewModel.trySendAction(
            RootNavAction.Internal.HasSeenWelcomeTutorialChange(true),
        )

        assertEquals(
            RootNavState(
                hasSeenWelcomeGuide = false,
                navState = RootNavState.NavState.Locked,
            ),
            viewModel.stateFlow.value,
        )
        verify(exactly = 1) { settingsRepository.hasSeenWelcomeTutorial = true }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `on HasSeenWelcomeTutorialChange with true and biometrics enabled but invalid should navigate to Unlocked`() {
        every { settingsRepository.isUnlockWithBiometricsEnabled } returns true
        every { biometricsEncryptionManager.isBiometricIntegrityValid() } returns false
        val viewModel = createViewModel()

        viewModel.trySendAction(
            RootNavAction.Internal.HasSeenWelcomeTutorialChange(true),
        )

        assertEquals(
            RootNavState(
                hasSeenWelcomeGuide = false,
                navState = RootNavState.NavState.Unlocked,
            ),
            viewModel.stateFlow.value,
        )
        verify(exactly = 1) { settingsRepository.hasSeenWelcomeTutorial = true }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `on HasSeenWelcomeTutorialChange with true and biometrics disabled should navigate to Unlocked`() {
        every { settingsRepository.isUnlockWithBiometricsEnabled } returns false
        val viewModel = createViewModel()

        viewModel.trySendAction(
            RootNavAction.Internal.HasSeenWelcomeTutorialChange(true),
        )

        assertEquals(
            RootNavState(
                hasSeenWelcomeGuide = false,
                navState = RootNavState.NavState.Unlocked,
            ),
            viewModel.stateFlow.value,
        )
        verify(exactly = 1) { settingsRepository.hasSeenWelcomeTutorial = true }
    }

    @Test
    fun `on HasSeenWelcomeTutorialChange with false should navigate to Tutorial`() {
        val viewModel = createViewModel()

        viewModel.trySendAction(
            RootNavAction.Internal.HasSeenWelcomeTutorialChange(false),
        )

        assertEquals(
            RootNavState(
                hasSeenWelcomeGuide = false,
                navState = RootNavState.NavState.Tutorial,
            ),
            viewModel.stateFlow.value,
        )
        // Called twice: once during init when flow emits, once from the action
        verify(exactly = 2) { settingsRepository.hasSeenWelcomeTutorial = false }
    }

    @Test
    fun `on TutorialFinished should update settingsRepository and navigate to Unlocked`() {
        val viewModel = createViewModel()

        viewModel.trySendAction(RootNavAction.Internal.TutorialFinished)

        assertEquals(
            RootNavState(
                hasSeenWelcomeGuide = false,
                navState = RootNavState.NavState.Unlocked,
            ),
            viewModel.stateFlow.value,
        )
        verify(exactly = 1) { settingsRepository.hasSeenWelcomeTutorial = true }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `on SplashScreenDismissed when hasSeenWelcomeTutorial is true and currently Splash should navigate to Unlocked`() =
        runTest {
            // Set hasSeenWelcomeTutorial to false initially to stay on Splash
            every { settingsRepository.hasSeenWelcomeTutorial } returns false
            mutableHasSeenWelcomeTutorialFlow.value = false
            val viewModel = createViewModel()

            viewModel.stateFlow.test {
                // Initial state - Tutorial from init flow
                assertEquals(
                    RootNavState(
                        hasSeenWelcomeGuide = false,
                        navState = RootNavState.NavState.Tutorial,
                    ),
                    awaitItem(),
                )

                // Now change the repository value and trigger SplashScreenDismissed
                every { settingsRepository.hasSeenWelcomeTutorial } returns true

                viewModel.trySendAction(RootNavAction.Internal.SplashScreenDismissed)

                // Should navigate to Unlocked based on new repository value
                assertEquals(
                    RootNavState(
                        hasSeenWelcomeGuide = false,
                        navState = RootNavState.NavState.Unlocked,
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `on SplashScreenDismissed when hasSeenWelcomeTutorial is false and currently in different state should navigate to Tutorial`() =
        runTest {
            // Start with hasSeenWelcomeTutorial = true to go to Unlocked
            every { settingsRepository.hasSeenWelcomeTutorial } returns true
            mutableHasSeenWelcomeTutorialFlow.value = true
            val viewModel = createViewModel()

            viewModel.stateFlow.test {
                // Initial state - Unlocked from init flow
                assertEquals(
                    RootNavState(
                        hasSeenWelcomeGuide = true,
                        navState = RootNavState.NavState.Unlocked,
                    ),
                    awaitItem(),
                )

                // Change the repository value and trigger SplashScreenDismissed
                every { settingsRepository.hasSeenWelcomeTutorial } returns false

                viewModel.trySendAction(RootNavAction.Internal.SplashScreenDismissed)

                // Should navigate to Tutorial based on new repository value
                assertEquals(
                    RootNavState(
                        hasSeenWelcomeGuide = true,
                        navState = RootNavState.NavState.Tutorial,
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `on AppUnlocked should navigate to Unlocked`() {
        val viewModel = createViewModel()

        viewModel.trySendAction(RootNavAction.Internal.AppUnlocked)

        assertEquals(
            RootNavState(
                hasSeenWelcomeGuide = false,
                navState = RootNavState.NavState.Unlocked,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on BiometricSupportChanged with false should clear biometrics key`() {
        val viewModel = createViewModel()

        viewModel.trySendAction(RootNavAction.BiometricSupportChanged(false))

        verify(exactly = 1) { settingsRepository.clearBiometricsKey() }
    }

    @Test
    fun `on BiometricSupportChanged with true should not clear biometrics key`() {
        val viewModel = createViewModel()

        viewModel.trySendAction(RootNavAction.BiometricSupportChanged(true))

        verify(exactly = 0) { settingsRepository.clearBiometricsKey() }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `on BiometricSupportChanged with false when Locked should navigate to Unlocked`() = runTest {
        every { settingsRepository.hasSeenWelcomeTutorial } returns true
        every { settingsRepository.isUnlockWithBiometricsEnabled } returns true
        every { biometricsEncryptionManager.isBiometricIntegrityValid() } returns true
        mutableHasSeenWelcomeTutorialFlow.value = true
        val viewModel = createViewModel()

        // Verify initial state is Locked
        assertEquals(
            RootNavState(
                hasSeenWelcomeGuide = true,
                navState = RootNavState.NavState.Locked,
            ),
            viewModel.stateFlow.value,
        )

        // Send BiometricSupportChanged with false
        viewModel.trySendAction(RootNavAction.BiometricSupportChanged(false))

        // Should navigate to Unlocked and clear biometric key
        assertEquals(
            RootNavState(
                hasSeenWelcomeGuide = true,
                navState = RootNavState.NavState.Unlocked,
            ),
            viewModel.stateFlow.value,
        )
        verify(exactly = 1) { settingsRepository.clearBiometricsKey() }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `on BiometricSupportChanged with false when not Locked should not change navigation state`() =
        runTest {
            every { settingsRepository.hasSeenWelcomeTutorial } returns true
            mutableHasSeenWelcomeTutorialFlow.value = true
            val viewModel = createViewModel()

            // Verify initial state is Unlocked (biometrics not enabled)
            assertEquals(
                RootNavState(
                    hasSeenWelcomeGuide = true,
                    navState = RootNavState.NavState.Unlocked,
                ),
                viewModel.stateFlow.value,
            )

            // Send BiometricSupportChanged with false
            viewModel.trySendAction(RootNavAction.BiometricSupportChanged(false))

            // Should remain Unlocked and clear biometric key
            assertEquals(
                RootNavState(
                    hasSeenWelcomeGuide = true,
                    navState = RootNavState.NavState.Unlocked,
                ),
                viewModel.stateFlow.value,
            )
            verify(exactly = 1) { settingsRepository.clearBiometricsKey() }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `hasSeenWelcomeTutorialFlow updates should trigger HasSeenWelcomeTutorialChange action`() =
        runTest {
            every { settingsRepository.isUnlockWithBiometricsEnabled } returns false
            val viewModel = createViewModel()

            viewModel.stateFlow.test {
                // Initial emission after flow subscription - navigates to Tutorial since hasSeenWelcomeTutorial is false
                assertEquals(
                    RootNavState(
                        hasSeenWelcomeGuide = false,
                        navState = RootNavState.NavState.Tutorial,
                    ),
                    awaitItem(),
                )

                // Update the flow value to true
                mutableHasSeenWelcomeTutorialFlow.value = true

                // Should navigate to Unlocked since biometrics is not enabled
                assertEquals(
                    RootNavState(
                        hasSeenWelcomeGuide = false,
                        navState = RootNavState.NavState.Unlocked,
                    ),
                    awaitItem(),
                )
            }
        }

    private fun createViewModel() = RootNavViewModel(
        authRepository = authRepository,
        settingsRepository = settingsRepository,
        biometricsEncryptionManager = biometricsEncryptionManager,
    )
}
