package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.data.auth.repository.model.UserFingerprintResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.BiometricsKeyResult
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.vault.datasource.network.model.PolicyTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockPolicy
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AccountSecurityViewModelTest : BaseViewModelTest() {

    private val fakeEnvironmentRepository = FakeEnvironmentRepository()
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(DEFAULT_USER_STATE)
    private val authRepository: AuthRepository = mockk(relaxed = true) {
        every { userStateFlow } returns mutableUserStateFlow
    }
    private val vaultRepository: VaultRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk {
        every { isUnlockWithBiometricsEnabled } returns false
        every { isApprovePasswordlessLoginsEnabled } returns false
        every { isUnlockWithPinEnabled } returns false
        every { vaultTimeout } returns VaultTimeout.ThirtyMinutes
        every { vaultTimeoutAction } returns VaultTimeoutAction.LOCK
        coEvery { getUserFingerprint() } returns UserFingerprintResult.Success(FINGERPRINT)
    }
    private val mutableActivePolicyFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Policy>>()
    private val policyManager: PolicyManager = mockk {
        every {
            getActivePoliciesFlow(type = PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT)
        } returns mutableActivePolicyFlow
    }

    @Test
    fun `initial state should be correct when saved state is set`() {
        val viewModel = createViewModel(initialState = DEFAULT_STATE)
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
        coVerify { settingsRepository.getUserFingerprint() }
    }

    @Test
    fun `initial state should be correct when saved state is not set`() {
        every { settingsRepository.isUnlockWithPinEnabled } returns true
        val viewModel = createViewModel(initialState = null)
        assertEquals(
            DEFAULT_STATE.copy(isUnlockWithPinEnabled = true),
            viewModel.stateFlow.value,
        )
        coVerify { settingsRepository.getUserFingerprint() }
    }

    @Test
    fun `state updates when policies change`() = runTest {
        val viewModel = createViewModel()

        val policyInformation = PolicyInformation.VaultTimeout(
            minutes = 10,
            action = "lock",
        )
        mutableActivePolicyFlow.emit(
            listOf(
                createMockPolicy(
                    isEnabled = true,
                    type = PolicyTypeJson.MAXIMUM_VAULT_TIMEOUT,
                    data = Json.encodeToJsonElement(policyInformation).jsonObject,
                ),
            ),
        )

        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(
                    vaultTimeoutPolicyMinutes = 10,
                    vaultTimeoutPolicyAction = "lock",
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `on FingerprintResultReceive should update the fingerprint phrase`() = runTest {
        val fingerprint = "fingerprint"
        val viewModel = createViewModel(initialState = DEFAULT_STATE)
        // Set fingerprint phrase to value received
        viewModel.trySendAction(
            AccountSecurityAction.Internal.FingerprintResultReceive(
                UserFingerprintResult.Success(fingerprint),
            ),
        )
        assertEquals(
            DEFAULT_STATE.copy(fingerprintPhrase = fingerprint.asText()),
            viewModel.stateFlow.value,
        )
        // Clear fingerprint phrase
        viewModel.trySendAction(
            AccountSecurityAction.Internal.FingerprintResultReceive(
                UserFingerprintResult.Error,
            ),
        )
        assertEquals(
            DEFAULT_STATE.copy(fingerprintPhrase = "".asText()),
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

    @Suppress("MaxLineLength")
    @Test
    fun `on ChangeMasterPasswordClick should emit NavigateToChangeMasterPassword with correct URL based on US and EU environments`() =
        runTest {
            fakeEnvironmentRepository.environment = Environment.Us
            val viewModel = createViewModel()
            viewModel.eventFlow.test {

                viewModel.trySendAction(AccountSecurityAction.ChangeMasterPasswordClick)
                assertEquals(
                    AccountSecurityEvent.NavigateToChangeMasterPassword(
                        "https://vault.bitwarden.com/#/settings",
                    ),
                    awaitItem(),
                )

                fakeEnvironmentRepository.environment = Environment.Eu

                viewModel.trySendAction(AccountSecurityAction.ChangeMasterPasswordClick)
                assertEquals(
                    AccountSecurityEvent.NavigateToChangeMasterPassword(
                        "https://vault.bitwarden.eu/#/settings",
                    ),
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
        every { vaultRepository.lockVaultForCurrentUser() } just runs
        val viewModel = createViewModel()
        viewModel.trySendAction(AccountSecurityAction.LockNowClick)
        verify { vaultRepository.lockVaultForCurrentUser() }
    }

    @Test
    fun `on PendingLoginRequestsClick should emit NavigateToPendingRequests`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AccountSecurityAction.PendingLoginRequestsClick)
            assertEquals(
                AccountSecurityEvent.NavigateToPendingRequests,
                awaitItem(),
            )
        }
    }

    @Test
    fun `on VaultTimeoutTypeSelect should update the selection()`() = runTest {
        every { settingsRepository.vaultTimeout = any() } just runs
        val viewModel = createViewModel()
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
        every { settingsRepository.vaultTimeout = any() } just runs
        val viewModel = createViewModel()
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
        every { settingsRepository.vaultTimeoutAction = any() } just runs
        val viewModel = createViewModel()
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

    @Suppress("MaxLineLength")
    @Test
    fun `on TwoStepLoginClick should emit NavigateToTwoStepLogin with correct URL based on US and EU environments`() =
        runTest {
            fakeEnvironmentRepository.environment = Environment.Us
            val viewModel = createViewModel()
            viewModel.eventFlow.test {

                viewModel.trySendAction(AccountSecurityAction.TwoStepLoginClick)
                assertEquals(
                    AccountSecurityEvent.NavigateToTwoStepLogin(
                        "https://vault.bitwarden.com/#/settings",
                    ),
                    awaitItem(),
                )

                fakeEnvironmentRepository.environment = Environment.Eu

                viewModel.trySendAction(AccountSecurityAction.TwoStepLoginClick)
                assertEquals(
                    AccountSecurityEvent.NavigateToTwoStepLogin(
                        "https://vault.bitwarden.eu/#/settings",
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `on UnlockWithBiometricToggle false should call clearBiometricsKey and update the state`() =
        runTest {
            val initialState = DEFAULT_STATE.copy(isUnlockWithBiometricsEnabled = true)
            every { settingsRepository.isUnlockWithBiometricsEnabled } returns true
            every { settingsRepository.clearBiometricsKey() } just runs
            val viewModel = createViewModel(initialState)
            assertEquals(initialState, viewModel.stateFlow.value)

            viewModel.trySendAction(AccountSecurityAction.UnlockWithBiometricToggle(false))

            assertEquals(
                initialState.copy(isUnlockWithBiometricsEnabled = false),
                viewModel.stateFlow.value,
            )
            verify(exactly = 1) {
                settingsRepository.clearBiometricsKey()
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `on UnlockWithBiometricToggle false should call clearBiometricsKey, reset the vaultTimeoutAction, and update the state`() =
        runTest {
            val initialState = DEFAULT_STATE.copy(
                isUnlockWithPasswordEnabled = false,
                isUnlockWithBiometricsEnabled = true,
            )
            every { settingsRepository.isUnlockWithBiometricsEnabled } returns true
            every { settingsRepository.clearBiometricsKey() } just runs
            every { settingsRepository.vaultTimeoutAction = VaultTimeoutAction.LOGOUT } just runs
            val viewModel = createViewModel(initialState)
            assertEquals(initialState, viewModel.stateFlow.value)

            viewModel.trySendAction(AccountSecurityAction.UnlockWithBiometricToggle(false))

            assertEquals(
                initialState.copy(
                    isUnlockWithBiometricsEnabled = false,
                    vaultTimeoutAction = VaultTimeoutAction.LOGOUT,
                ),
                viewModel.stateFlow.value,
            )
            verify(exactly = 1) {
                settingsRepository.clearBiometricsKey()
                settingsRepository.vaultTimeoutAction = VaultTimeoutAction.LOGOUT
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `on UnlockWithBiometricToggle true and setupBiometricsKey error should call update the state accordingly`() =
        runTest {
            coEvery { settingsRepository.setupBiometricsKey() } returns BiometricsKeyResult.Error
            val viewModel = createViewModel()

            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())
                viewModel.trySendAction(AccountSecurityAction.UnlockWithBiometricToggle(true))
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialog = AccountSecurityDialog.Loading(R.string.saving.asText()),
                        isUnlockWithBiometricsEnabled = true,
                    ),
                    awaitItem(),
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialog = null,
                        isUnlockWithBiometricsEnabled = false,
                    ),
                    awaitItem(),
                )
            }
            coVerify(exactly = 1) {
                settingsRepository.setupBiometricsKey()
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `on UnlockWithBiometricToggle true and setupBiometricsKey success should call update the state accordingly`() =
        runTest {
            coEvery { settingsRepository.setupBiometricsKey() } returns BiometricsKeyResult.Success
            val viewModel = createViewModel()

            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())
                viewModel.trySendAction(AccountSecurityAction.UnlockWithBiometricToggle(true))
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialog = AccountSecurityDialog.Loading(R.string.saving.asText()),
                        isUnlockWithBiometricsEnabled = true,
                    ),
                    awaitItem(),
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialog = null,
                        isUnlockWithBiometricsEnabled = true,
                    ),
                    awaitItem(),
                )
            }
            coVerify(exactly = 1) {
                settingsRepository.setupBiometricsKey()
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `on UnlockWithPinToggle Disabled should set pin unlock to false and clear the PIN in settings`() {
        val initialState = DEFAULT_STATE.copy(
            isUnlockWithPinEnabled = true,
        )
        every { settingsRepository.clearUnlockPin() } just runs
        val viewModel = createViewModel(initialState = initialState)
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
    fun `on UnlockWithPinToggle Disabled should set pin unlock to false, reset the vaultTimeoutAction, and clear the PIN in settings`() {
        val initialState = DEFAULT_STATE.copy(
            isUnlockWithPasswordEnabled = false,
            isUnlockWithPinEnabled = true,
        )
        every { settingsRepository.clearUnlockPin() } just runs
        every { settingsRepository.vaultTimeoutAction = VaultTimeoutAction.LOGOUT } just runs
        val viewModel = createViewModel(initialState = initialState)
        viewModel.trySendAction(
            AccountSecurityAction.UnlockWithPinToggle.Disabled,
        )
        assertEquals(
            initialState.copy(
                vaultTimeoutAction = VaultTimeoutAction.LOGOUT,
                isUnlockWithPinEnabled = false,
            ),
            viewModel.stateFlow.value,
        )
        verify {
            settingsRepository.clearUnlockPin()
            settingsRepository.vaultTimeoutAction = VaultTimeoutAction.LOGOUT
        }
    }

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
        every { settingsRepository.storeUnlockPin(any(), any()) } just runs

        val viewModel = createViewModel(initialState = initialState)
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
        every { authRepository.logout() } just runs
        val viewModel = createViewModel()
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
    fun `on ApprovePasswordlessLoginsToggle enabled should update settings and set isApprovePasswordlessLoginsEnabled to true`() =
        runTest {
            every { settingsRepository.isApprovePasswordlessLoginsEnabled = true } just runs
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    AccountSecurityAction.ApprovePasswordlessLoginsToggle.Enabled,
                )
                expectNoEvents()
                verify(exactly = 1) { settingsRepository.isApprovePasswordlessLoginsEnabled = true }
            }
            assertTrue(viewModel.stateFlow.value.isApproveLoginRequestsEnabled)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `on ApprovePasswordlessLoginsToggle pending enabled should set isApprovePasswordlessLoginsEnabled to true`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    AccountSecurityAction.ApprovePasswordlessLoginsToggle.PendingEnabled,
                )
                expectNoEvents()
            }
            assertTrue(viewModel.stateFlow.value.isApproveLoginRequestsEnabled)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `on ApprovePasswordlessLoginsToggle disabled should update settings and set isApprovePasswordlessLoginsEnabled to false`() =
        runTest {
            every { settingsRepository.isApprovePasswordlessLoginsEnabled = false } just runs
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    AccountSecurityAction.ApprovePasswordlessLoginsToggle.Disabled,
                )
                expectNoEvents()
                verify(exactly = 1) {
                    settingsRepository.isApprovePasswordlessLoginsEnabled = false
                }
            }
            assertFalse(viewModel.stateFlow.value.isApproveLoginRequestsEnabled)
        }

    @Test
    fun `on PushNotificationConfirm should send NavigateToApplicationDataSettings event`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(AccountSecurityAction.PushNotificationConfirm)
                assertEquals(
                    AccountSecurityEvent.NavigateToApplicationDataSettings,
                    awaitItem(),
                )
            }
        }

    @Suppress("LongParameterList")
    private fun createViewModel(
        initialState: AccountSecurityState? = DEFAULT_STATE,
        authRepository: AuthRepository = this.authRepository,
        vaultRepository: VaultRepository = this.vaultRepository,
        environmentRepository: EnvironmentRepository = this.fakeEnvironmentRepository,
        settingsRepository: SettingsRepository = this.settingsRepository,
        policyManager: PolicyManager = this.policyManager,
    ): AccountSecurityViewModel = AccountSecurityViewModel(
        authRepository = authRepository,
        vaultRepository = vaultRepository,
        settingsRepository = settingsRepository,
        environmentRepository = environmentRepository,
        policyManager = policyManager,
        savedStateHandle = SavedStateHandle().apply {
            set("state", initialState)
        },
    )
}

private const val FINGERPRINT: String = "fingerprint"

private val DEFAULT_STATE: AccountSecurityState = AccountSecurityState(
    dialog = null,
    fingerprintPhrase = FINGERPRINT.asText(),
    isApproveLoginRequestsEnabled = false,
    isUnlockWithBiometricsEnabled = false,
    isUnlockWithPasswordEnabled = true,
    isUnlockWithPinEnabled = false,
    vaultTimeout = VaultTimeout.ThirtyMinutes,
    vaultTimeoutAction = VaultTimeoutAction.LOCK,
    vaultTimeoutPolicyMinutes = null,
    vaultTimeoutPolicyAction = null,
)

private val DEFAULT_USER_STATE = UserState(
    activeUserId = "activeUserId",
    accounts = listOf(
        UserState.Account(
            userId = "activeUserId",
            name = "Active User",
            email = "active@bitwarden.com",
            avatarColorHex = "#aa00aa",
            environment = Environment.Us,
            isPremium = true,
            isLoggedIn = true,
            isVaultUnlocked = true,
            needsPasswordReset = false,
            isBiometricsEnabled = false,
            organizations = emptyList(),
            needsMasterPassword = false,
            trustedDevice = null,
        ),
    ),
)
