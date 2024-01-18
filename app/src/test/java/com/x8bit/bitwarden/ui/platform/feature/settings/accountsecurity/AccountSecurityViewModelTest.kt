package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AccountSecurityViewModelTest : BaseViewModelTest() {

    @Test
    fun `initial state should be correct when saved state is set`() {
        val viewModel = createViewModel(initialState = DEFAULT_STATE)
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should be correct when saved state is not set`() {
        val settingsRepository: SettingsRepository = mockk {
            every { isUnlockWithPinEnabled } returns true
            every { vaultTimeout } returns VaultTimeout.ThirtyMinutes
            every { vaultTimeoutAction } returns VaultTimeoutAction.LOCK
            every { isApprovePasswordlessLoginsEnabled } returns false
        }
        val viewModel = createViewModel(
            initialState = null,
            settingsRepository = settingsRepository,
        )
        assertEquals(
            DEFAULT_STATE.copy(isUnlockWithPinEnabled = true),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on AccountFingerprintPhraseClick should show the fingerprint phrase dialog`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(AccountSecurityAction.AccountFingerprintPhraseClick)
        assertEquals(
            DEFAULT_STATE.copy(dialog = AccountSecurityDialog.FingerprintPhrase),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on FingerPrintLearnMoreClick should emit NavigateToFingerprintPhrase`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AccountSecurityAction.FingerPrintLearnMoreClick)
            assertEquals(AccountSecurityEvent.NavigateToFingerprintPhrase, awaitItem())
        }
    }

    @Test
    fun `on BackClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AccountSecurityAction.BackClick)
            assertEquals(AccountSecurityEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `on ChangeMasterPasswordClick should emit ShowToast`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AccountSecurityAction.ChangeMasterPasswordClick)
            assertEquals(
                AccountSecurityEvent.ShowToast("Not yet implemented.".asText()),
                awaitItem(),
            )
        }
    }

    @Test
    fun `on DeleteAccountClick should emit NavigateToDeleteAccount`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AccountSecurityAction.DeleteAccountClick)
            assertEquals(AccountSecurityEvent.NavigateToDeleteAccount, awaitItem())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on DismissSessionTimeoutActionDialog should update shouldShowSessionTimeoutActionDialog`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.trySendAction(AccountSecurityAction.DismissDialog)
            assertEquals(DEFAULT_STATE.copy(dialog = null), viewModel.stateFlow.value)
        }

    @Test
    fun `on LockNowClick should call lockVaultForCurrentUser`() {
        val vaultRepository = mockk<VaultRepository>(relaxed = true) {
            every { lockVaultForCurrentUser() } just runs
        }
        val viewModel = createViewModel(vaultRepository = vaultRepository)
        viewModel.trySendAction(AccountSecurityAction.LockNowClick)
        verify { vaultRepository.lockVaultForCurrentUser() }
    }

    @Test
    fun `on PendingLoginRequestsClick should emit ShowToast`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AccountSecurityAction.PendingLoginRequestsClick)
            assertEquals(
                AccountSecurityEvent.ShowToast("Not yet implemented.".asText()),
                awaitItem(),
            )
        }
    }

    @Test
    fun `on VaultTimeoutTypeSelect should update the selection()`() = runTest {
        val settingsRepository = mockk<SettingsRepository>() {
            every { vaultTimeout = any() } just runs
        }
        val viewModel = createViewModel(settingsRepository = settingsRepository)
        viewModel.trySendAction(
            AccountSecurityAction.VaultTimeoutTypeSelect(VaultTimeout.Type.FOUR_HOURS),
        )
        assertEquals(
            DEFAULT_STATE.copy(
                vaultTimeout = VaultTimeout.FourHours,
            ),
            viewModel.stateFlow.value,
        )
        verify { settingsRepository.vaultTimeout = VaultTimeout.FourHours }
    }

    @Test
    fun `on CustomVaultTimeoutSelect should update the selection()`() = runTest {
        val settingsRepository = mockk<SettingsRepository>() {
            every { vaultTimeout = any() } just runs
        }
        val viewModel = createViewModel(settingsRepository = settingsRepository)
        viewModel.trySendAction(
            AccountSecurityAction.CustomVaultTimeoutSelect(
                customVaultTimeout = VaultTimeout.Custom(vaultTimeoutInMinutes = 360),
            ),
        )
        assertEquals(
            DEFAULT_STATE.copy(
                vaultTimeout = VaultTimeout.Custom(vaultTimeoutInMinutes = 360),
            ),
            viewModel.stateFlow.value,
        )
        verify {
            settingsRepository.vaultTimeout = VaultTimeout.Custom(vaultTimeoutInMinutes = 360)
        }
    }

    @Test
    fun `on VaultTimeoutActionSelect should update vault timeout action`() = runTest {
        val settingsRepository = mockk<SettingsRepository>() {
            every { vaultTimeoutAction = any() } just runs
        }
        val viewModel = createViewModel(settingsRepository = settingsRepository)
        viewModel.trySendAction(
            AccountSecurityAction.VaultTimeoutActionSelect(VaultTimeoutAction.LOGOUT),
        )
        assertEquals(
            DEFAULT_STATE.copy(
                vaultTimeoutAction = VaultTimeoutAction.LOGOUT,
            ),
            viewModel.stateFlow.value,
        )
        verify { settingsRepository.vaultTimeoutAction = VaultTimeoutAction.LOGOUT }
    }

    @Test
    fun `on TwoStepLoginClick should emit NavigateToTwoStepLogin`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AccountSecurityAction.TwoStepLoginClick)
            assertEquals(
                AccountSecurityEvent.ShowToast("Not yet implemented.".asText()),
                awaitItem(),
            )
        }
    }

    @Test
    fun `on UnlockWithBiometricToggle should emit ShowToast`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AccountSecurityAction.UnlockWithBiometricToggle(true))
            assertEquals(
                AccountSecurityEvent.ShowToast("Handle unlock with biometrics.".asText()),
                awaitItem(),
            )
        }
        assertEquals(
            DEFAULT_STATE.copy(isUnlockWithBiometricsEnabled = true),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on UnlockWithPinToggle Disabled should set pin unlock to false and clear the PIN in settings`() {
        val initialState = DEFAULT_STATE.copy(
            isUnlockWithPinEnabled = true,
        )
        val settingsRepository: SettingsRepository = mockk() {
            every { clearUnlockPin() } just runs
        }
        val viewModel = createViewModel(
            initialState = initialState,
            settingsRepository = settingsRepository,
        )
        viewModel.trySendAction(
            AccountSecurityAction.UnlockWithPinToggle.Disabled,
        )
        assertEquals(
            initialState.copy(isUnlockWithPinEnabled = false),
            viewModel.stateFlow.value,
        )
        verify { settingsRepository.clearUnlockPin() }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on UnlockWithPinToggle PendingEnabled should set pin unlock to true`() {
        val initialState = DEFAULT_STATE.copy(
            isUnlockWithPinEnabled = false,
        )
        val viewModel = createViewModel(initialState = initialState)
        viewModel.trySendAction(
            AccountSecurityAction.UnlockWithPinToggle.PendingEnabled,
        )
        assertEquals(
            initialState.copy(isUnlockWithPinEnabled = true),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on UnlockWithPinToggle Enabled should set pin unlock to true and set the PIN in settings`() {
        val initialState = DEFAULT_STATE.copy(
            isUnlockWithPinEnabled = false,
        )
        val settingsRepository: SettingsRepository = mockk() {
            every { storeUnlockPin(any(), any()) } just runs
        }
        val viewModel = createViewModel(
            initialState = initialState,
            settingsRepository = settingsRepository,
        )
        viewModel.trySendAction(
            AccountSecurityAction.UnlockWithPinToggle.Enabled(
                pin = "1234",
                shouldRequireMasterPasswordOnRestart = true,
            ),
        )
        assertEquals(
            initialState.copy(isUnlockWithPinEnabled = true),
            viewModel.stateFlow.value,
        )
        verify {
            settingsRepository.storeUnlockPin(
                pin = "1234",
                shouldRequireMasterPasswordOnRestart = true,
            )
        }
    }

    @Test
    fun `on LogoutClick should show confirm log out dialog`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(AccountSecurityAction.LogoutClick)
        assertEquals(
            DEFAULT_STATE.copy(dialog = AccountSecurityDialog.ConfirmLogout),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on ConfirmLogoutClick should call logout and hide confirm dialog`() = runTest {
        val authRepository: AuthRepository = mockk {
            every { logout() } returns Unit
        }
        val viewModel = createViewModel(authRepository = authRepository)
        viewModel.trySendAction(AccountSecurityAction.ConfirmLogoutClick)
        assertEquals(DEFAULT_STATE.copy(dialog = null), viewModel.stateFlow.value)
        verify { authRepository.logout() }
    }

    @Test
    fun `on DismissDialog should hide dialog`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(AccountSecurityAction.DismissDialog)
        assertEquals(DEFAULT_STATE.copy(dialog = null), viewModel.stateFlow.value)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ApprovePasswordlessLoginsToggle enabled should update settings, set isApprovePasswordlessLoginsEnabled to true, and display toast`() =
        runTest {
            val settingsRepository = mockk<SettingsRepository> {
                every { isApprovePasswordlessLoginsEnabled = true } just runs
            }
            val viewModel = createViewModel(
                settingsRepository = settingsRepository,
            )
            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    AccountSecurityAction.ApprovePasswordlessLoginsToggle.Enabled,
                )
                assertEquals(
                    AccountSecurityEvent.ShowToast("Handle Login requests on this device.".asText()),
                    awaitItem(),
                )
                verify(exactly = 1) { settingsRepository.isApprovePasswordlessLoginsEnabled = true }
            }
            assertTrue(viewModel.stateFlow.value.isApproveLoginRequestsEnabled)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `on ApprovePasswordlessLoginsToggle pending enabled should set isApprovePasswordlessLoginsEnabled to true and display toast`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    AccountSecurityAction.ApprovePasswordlessLoginsToggle.PendingEnabled,
                )
                assertEquals(
                    AccountSecurityEvent.ShowToast("Handle Login requests on this device.".asText()),
                    awaitItem(),
                )
            }
            assertTrue(viewModel.stateFlow.value.isApproveLoginRequestsEnabled)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `on ApprovePasswordlessLoginsToggle disabled should update settings, set isApprovePasswordlessLoginsEnabled to false, and display toast`() =
        runTest {
            val settingsRepository = mockk<SettingsRepository> {
                every { isApprovePasswordlessLoginsEnabled = false } just runs
            }
            val viewModel = createViewModel(
                settingsRepository = settingsRepository,
            )
            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    AccountSecurityAction.ApprovePasswordlessLoginsToggle.Disabled,
                )
                assertEquals(
                    AccountSecurityEvent.ShowToast("Handle Login requests on this device.".asText()),
                    awaitItem(),
                )
                verify(exactly = 1) {
                    settingsRepository.isApprovePasswordlessLoginsEnabled = false
                }
            }
            assertFalse(viewModel.stateFlow.value.isApproveLoginRequestsEnabled)
        }

    private fun createViewModel(
        initialState: AccountSecurityState? = DEFAULT_STATE,
        authRepository: AuthRepository = mockk(relaxed = true),
        vaultRepository: VaultRepository = mockk(relaxed = true),
        settingsRepository: SettingsRepository = mockk(relaxed = true),
        savedStateHandle: SavedStateHandle = SavedStateHandle().apply {
            set("state", initialState)
        },
    ): AccountSecurityViewModel = AccountSecurityViewModel(
        authRepository = authRepository,
        vaultRepository = vaultRepository,
        settingsRepository = settingsRepository,
        savedStateHandle = savedStateHandle,
    )

    companion object {
        private val DEFAULT_STATE = AccountSecurityState(
            dialog = null,
            fingerprintPhrase = "fingerprint-placeholder".asText(),
            isApproveLoginRequestsEnabled = false,
            isUnlockWithBiometricsEnabled = false,
            isUnlockWithPinEnabled = false,
            vaultTimeout = VaultTimeout.ThirtyMinutes,
            vaultTimeoutAction = VaultTimeoutAction.LOCK,
        )
    }
}
