package com.x8bit.bitwarden.ui.auth.feature.vaultunlock

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.SwitchAccountResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.data.platform.manager.BiometricsEncryptionManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.model.UnlockType
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toAccountSummary
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import javax.crypto.Cipher

@Suppress("LargeClass")
class VaultUnlockViewModelTest : BaseViewModelTest() {

    private val mutableUserStateFlow = MutableStateFlow<UserState?>(DEFAULT_USER_STATE)
    private val environmentRepository = FakeEnvironmentRepository()
    private val authRepository = mockk<AuthRepository> {
        every { activeUserId } answers { mutableUserStateFlow.value?.activeUserId }
        every { userStateFlow } returns mutableUserStateFlow
        every { hasPendingAccountAddition } returns false
        every { hasPendingAccountAddition = any() } just runs
        every { logout() } just runs
        every { logout(any()) } just runs
        every { switchAccount(any()) } returns SwitchAccountResult.AccountSwitched
    }
    private val vaultRepository: VaultRepository = mockk(relaxed = true) {
        every { lockVault(any()) } just runs
    }
    private val encryptionManager: BiometricsEncryptionManager = mockk {
        every { getOrCreateCipher(USER_ID) } returns CIPHER
        every {
            isBiometricIntegrityValid(
                userId = DEFAULT_USER_STATE.activeUserId,
                cipher = CIPHER,
            )
        } returns true
        every {
            isBiometricIntegrityValid(
                userId = DEFAULT_USER_STATE.activeUserId,
                cipher = null,
            )
        } returns false
    }

    @Test
    fun `on init with biometrics enabled and valid should emit PromptForBiometrics`() = runTest {
        val initialState = DEFAULT_STATE.copy(
            isBiometricEnabled = true,
            isBiometricsValid = true,
        )
        val viewModel = createViewModel(state = initialState)

        viewModel.eventFlow.test {
            assertEquals(VaultUnlockEvent.PromptForBiometrics(CIPHER), awaitItem())
        }
    }

    @Test
    fun `initial state should be correct when not set`() {
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
        verify { encryptionManager.getOrCreateCipher(USER_ID) }
    }

    @Test
    fun `initial state should be correct when set`() {
        val state = DEFAULT_STATE.copy(
            input = "pass",
        )
        val viewModel = createViewModel(state = state)
        assertEquals(state, viewModel.stateFlow.value)
    }

    @Test
    fun `on init should logout when has no master password, no pin, and no biometrics`() {
        mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
            accounts = listOf(
                DEFAULT_ACCOUNT.copy(
                    vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
                    isBiometricsEnabled = false,
                    trustedDevice = TRUSTED_DEVICE,
                ),
            ),
        )
        createViewModel()

        verify(exactly = 1) {
            authRepository.logout()
        }
    }

    @Test
    fun `on init should not logout when has no master password and no pin, with biometrics`() {
        mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
            accounts = listOf(
                DEFAULT_ACCOUNT.copy(
                    vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
                    isBiometricsEnabled = true,
                    trustedDevice = TRUSTED_DEVICE,
                ),
            ),
        )
        createViewModel()

        verify(exactly = 0) {
            authRepository.logout()
        }
    }

    @Test
    fun `on init should not logout when has no master password and no biometrics, with pin`() {
        mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
            accounts = listOf(
                DEFAULT_ACCOUNT.copy(
                    vaultUnlockType = VaultUnlockType.PIN,
                    isBiometricsEnabled = false,
                    trustedDevice = TRUSTED_DEVICE,
                ),
            ),
        )
        createViewModel()

        verify(exactly = 0) {
            authRepository.logout()
        }
    }

    @Test
    fun `environment url should update when environment repo emits an update`() {
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
        environmentRepository.environment = Environment.SelfHosted(
            environmentUrlData = EnvironmentUrlDataJson(base = "https://vault.qa.bitwarden.pw"),
        )
        assertEquals(
            DEFAULT_STATE.copy(environmentUrl = "vault.qa.bitwarden.pw"),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `UserState updates with a null value should do nothing`() {
        val viewModel = createViewModel()
        assertEquals(
            DEFAULT_STATE,
            viewModel.stateFlow.value,
        )

        mutableUserStateFlow.value = null

        assertEquals(
            DEFAULT_STATE,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `UserState updates with a non-null unlocked account should not update the state`() {
        val viewModel = createViewModel()
        assertEquals(
            DEFAULT_STATE,
            viewModel.stateFlow.value,
        )

        mutableUserStateFlow.value =
            DEFAULT_USER_STATE.copy(
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "Other User",
                        email = "active+test@bitwarden.com",
                        avatarColorHex = "#00aaaa",
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

        assertEquals(
            DEFAULT_STATE,
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `UserState updates with a non-null locked account should update the account information in the state`() {
        val viewModel = createViewModel()
        assertEquals(
            DEFAULT_STATE,
            viewModel.stateFlow.value,
        )

        mutableUserStateFlow.value =
            DEFAULT_USER_STATE.copy(
                accounts = listOf(
                    UserState.Account(
                        userId = "activeUserId",
                        name = "Other User",
                        email = "active+test@bitwarden.com",
                        avatarColorHex = "#00aaaa",
                        environment = Environment.Us,
                        isPremium = true,
                        isLoggedIn = true,
                        isVaultUnlocked = false,
                        needsPasswordReset = false,
                        isBiometricsEnabled = true,
                        organizations = emptyList(),
                        needsMasterPassword = false,
                        trustedDevice = null,
                    ),
                ),
            )

        assertEquals(
            DEFAULT_STATE.copy(
                avatarColorString = "#00aaaa",
                initials = "OU",
                email = "active+test@bitwarden.com",
                accountSummaries = listOf(
                    AccountSummary(
                        userId = "activeUserId",
                        name = "Other User",
                        email = "active+test@bitwarden.com",
                        avatarColorHex = "#00aaaa",
                        environmentLabel = "bitwarden.com",
                        isActive = true,
                        isLoggedIn = true,
                        isVaultUnlocked = false,
                    ),
                ),
                isBiometricEnabled = true,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `UserState updates with a non-null locked account should clear view state input`() {
        val password = "abc1234"
        val initialState = DEFAULT_STATE.copy(
            input = password,
            accountSummaries = listOf(
                DEFAULT_ACCOUNT.copy(isVaultUnlocked = false)
                    .toAccountSummary(true),
            ),
        )
        val viewModel = createViewModel(state = initialState)

        assertEquals(
            initialState,
            viewModel.stateFlow.value,
        )

        mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
            accounts = listOf(
                DEFAULT_ACCOUNT.copy(isVaultUnlocked = false),
            ),
        )

        assertEquals(
            initialState.copy(input = ""),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on BiometricsUnlockClick should emit PromptForBiometrics when cipher is non-null`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.eventFlow.test {
                viewModel.trySendAction(VaultUnlockAction.BiometricsUnlockClick)
                assertEquals(VaultUnlockEvent.PromptForBiometrics(CIPHER), awaitItem())
            }
            verify { encryptionManager.getOrCreateCipher(USER_ID) }
        }

    @Test
    fun `on BiometricsUnlockClick should disable isBiometricsValid when cipher is null`() {
        val initialState = DEFAULT_STATE.copy(isBiometricsValid = true)
        val viewModel = createViewModel(state = initialState)
        every { encryptionManager.getOrCreateCipher(USER_ID) } returns null

        viewModel.trySendAction(VaultUnlockAction.BiometricsUnlockClick)
        assertEquals(
            initialState.copy(isBiometricsValid = false),
            viewModel.stateFlow.value,
        )
        verify { encryptionManager.getOrCreateCipher(USER_ID) }
    }

    @Test
    fun `on AddAccountClick should set hasPendingAccountAddition to true on the AuthRepository`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(VaultUnlockAction.AddAccountClick)
        verify {
            authRepository.hasPendingAccountAddition = true
        }
    }

    @Test
    fun `on DismissDialog should clear the dialog state`() = runTest {
        val initialState = DEFAULT_STATE.copy(dialog = VaultUnlockState.VaultUnlockDialog.Loading)
        val viewModel = createViewModel(state = initialState)
        viewModel.trySendAction(VaultUnlockAction.DismissDialog)
        assertEquals(
            initialState.copy(dialog = null),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on ConfirmLogoutClick should call logout on the AuthRepository`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(VaultUnlockAction.ConfirmLogoutClick)
        verify { authRepository.logout() }
    }

    @Test
    fun `on PasswordInputChanged should update the password input state`() = runTest {
        val viewModel = createViewModel()
        val password = "abcd1234"
        viewModel.trySendAction(VaultUnlockAction.InputChanged(input = password))
        assertEquals(
            DEFAULT_STATE.copy(input = password),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on LockAccountClick should call lockVault for the given account`() {
        val accountUserId = "userId"
        val accountSummary = mockk<AccountSummary> {
            every { userId } returns accountUserId
        }
        val viewModel = createViewModel()

        viewModel.trySendAction(VaultUnlockAction.LockAccountClick(accountSummary))

        verify { vaultRepository.lockVault(userId = accountUserId) }
    }

    @Test
    fun `on LogoutAccountClick should call logout for the given account`() {
        val accountUserId = "userId"
        val accountSummary = mockk<AccountSummary> {
            every { userId } returns accountUserId
        }
        val viewModel = createViewModel()

        viewModel.trySendAction(VaultUnlockAction.LogoutAccountClick(accountSummary))

        verify { authRepository.logout(userId = accountUserId) }
    }

    @Test
    fun `on SwitchAccountClick should switch to the given account`() = runTest {
        val viewModel = createViewModel()
        val updatedUserId = "updatedUserId"
        viewModel.trySendAction(
            VaultUnlockAction.SwitchAccountClick(
                accountSummary = mockk {
                    every { userId } returns updatedUserId
                },
            ),
        )
        verify { authRepository.switchAccount(userId = updatedUserId) }
    }

    @Test
    fun `on UnlockClick for password unlock should display error dialog on AuthenticationError`() {
        val password = "abcd1234"
        val initialState = DEFAULT_STATE.copy(
            input = password,
            vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
        )
        val viewModel = createViewModel(state = initialState)
        coEvery {
            vaultRepository.unlockVaultWithMasterPassword(password)
        } returns VaultUnlockResult.AuthenticationError

        viewModel.trySendAction(VaultUnlockAction.UnlockClick)
        assertEquals(
            initialState.copy(
                dialog = VaultUnlockState.VaultUnlockDialog.Error(
                    R.string.invalid_master_password.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
        coVerify {
            vaultRepository.unlockVaultWithMasterPassword(password)
        }
    }

    @Test
    fun `on UnlockClick for password unlock should display error dialog on GenericError`() {
        val password = "abcd1234"
        val initialState = DEFAULT_STATE.copy(
            input = password,
            vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
        )
        val viewModel = createViewModel(state = initialState)
        coEvery {
            vaultRepository.unlockVaultWithMasterPassword(password)
        } returns VaultUnlockResult.GenericError

        viewModel.trySendAction(VaultUnlockAction.UnlockClick)
        assertEquals(
            initialState.copy(
                dialog = VaultUnlockState.VaultUnlockDialog.Error(
                    R.string.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
        coVerify {
            vaultRepository.unlockVaultWithMasterPassword(password)
        }
    }

    @Test
    fun `on UnlockClick for password unlock should display error dialog on InvalidStateError`() {
        val password = "abcd1234"
        val initialState = DEFAULT_STATE.copy(
            input = password,
            vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
        )
        val viewModel = createViewModel(state = initialState)
        coEvery {
            vaultRepository.unlockVaultWithMasterPassword(password)
        } returns VaultUnlockResult.InvalidStateError

        viewModel.trySendAction(VaultUnlockAction.UnlockClick)
        assertEquals(
            initialState.copy(
                dialog = VaultUnlockState.VaultUnlockDialog.Error(
                    R.string.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
        coVerify {
            vaultRepository.unlockVaultWithMasterPassword(password)
        }
    }

    @Test
    fun `on UnlockClick for password unlock should clear dialog on success`() {
        val password = "abcd1234"
        val initialState = DEFAULT_STATE.copy(
            input = password,
            vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
        )
        val viewModel = createViewModel(state = initialState)
        coEvery {
            vaultRepository.unlockVaultWithMasterPassword(password)
        } returns VaultUnlockResult.Success

        viewModel.trySendAction(VaultUnlockAction.UnlockClick)
        assertEquals(
            initialState.copy(dialog = null),
            viewModel.stateFlow.value,
        )
        coVerify {
            vaultRepository.unlockVaultWithMasterPassword(password)
        }
    }

    @Test
    fun `on UnlockClick for password unlock should clear dialog when user has changed`() {
        val password = "abcd1234"
        val initialState = DEFAULT_STATE.copy(
            input = password,
            vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
        )
        val resultFlow = bufferedMutableSharedFlow<VaultUnlockResult>()
        val viewModel = createViewModel(state = initialState)
        coEvery {
            vaultRepository.unlockVaultWithMasterPassword(password)
        } coAnswers { resultFlow.first() }

        viewModel.trySendAction(VaultUnlockAction.UnlockClick)
        assertEquals(
            initialState.copy(dialog = VaultUnlockState.VaultUnlockDialog.Loading),
            viewModel.stateFlow.value,
        )

        val updatedUserId = "updatedUserId"
        mutableUserStateFlow.update {
            it?.copy(
                activeUserId = updatedUserId,
                accounts = listOf(DEFAULT_ACCOUNT.copy(userId = updatedUserId)),
            )
        }
        resultFlow.tryEmit(VaultUnlockResult.GenericError)

        assertEquals(
            initialState.copy(dialog = null),
            viewModel.stateFlow.value,
        )
        coVerify {
            vaultRepository.unlockVaultWithMasterPassword(password)
        }
    }

    @Test
    fun `on UnlockClick for PIN unlock should display error dialog on AuthenticationError`() {
        val pin = "1234"
        val initialState = DEFAULT_STATE.copy(
            input = pin,
            vaultUnlockType = VaultUnlockType.PIN,
        )
        val viewModel = createViewModel(state = initialState)
        coEvery {
            vaultRepository.unlockVaultWithPin(pin)
        } returns VaultUnlockResult.AuthenticationError

        viewModel.trySendAction(VaultUnlockAction.UnlockClick)
        assertEquals(
            initialState.copy(
                dialog = VaultUnlockState.VaultUnlockDialog.Error(
                    R.string.invalid_pin.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
        coVerify {
            vaultRepository.unlockVaultWithPin(pin)
        }
    }

    @Test
    fun `on UnlockClick for PIN unlock should display error dialog on GenericError`() {
        val pin = "1234"
        val initialState = DEFAULT_STATE.copy(
            input = pin,
            vaultUnlockType = VaultUnlockType.PIN,
        )
        val viewModel = createViewModel(state = initialState)
        coEvery {
            vaultRepository.unlockVaultWithPin(pin)
        } returns VaultUnlockResult.GenericError

        viewModel.trySendAction(VaultUnlockAction.UnlockClick)
        assertEquals(
            initialState.copy(
                dialog = VaultUnlockState.VaultUnlockDialog.Error(
                    R.string.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
        coVerify {
            vaultRepository.unlockVaultWithPin(pin)
        }
    }

    @Test
    fun `on UnlockClick for PIN unlock should display error dialog on InvalidStateError`() {
        val pin = "1234"
        val initialState = DEFAULT_STATE.copy(
            input = pin,
            vaultUnlockType = VaultUnlockType.PIN,
        )
        val viewModel = createViewModel(state = initialState)
        coEvery {
            vaultRepository.unlockVaultWithPin(pin)
        } returns VaultUnlockResult.InvalidStateError

        viewModel.trySendAction(VaultUnlockAction.UnlockClick)
        assertEquals(
            initialState.copy(
                dialog = VaultUnlockState.VaultUnlockDialog.Error(
                    R.string.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
        coVerify {
            vaultRepository.unlockVaultWithPin(pin)
        }
    }

    @Test
    fun `on UnlockClick for PIN unlock should clear dialog on success`() {
        val pin = "1234"
        val initialState = DEFAULT_STATE.copy(
            input = pin,
            vaultUnlockType = VaultUnlockType.PIN,
        )
        val viewModel = createViewModel(state = initialState)
        coEvery {
            vaultRepository.unlockVaultWithPin(pin)
        } returns VaultUnlockResult.Success

        viewModel.trySendAction(VaultUnlockAction.UnlockClick)
        assertEquals(
            initialState.copy(dialog = null),
            viewModel.stateFlow.value,
        )
        coVerify {
            vaultRepository.unlockVaultWithPin(pin)
        }
    }

    @Test
    fun `on UnlockClick for PIN unlock should clear dialog when user has changed`() {
        val pin = "1234"
        val initialState = DEFAULT_STATE.copy(
            input = pin,
            vaultUnlockType = VaultUnlockType.PIN,
        )
        val resultFlow = bufferedMutableSharedFlow<VaultUnlockResult>()
        val viewModel = createViewModel(state = initialState)
        coEvery {
            vaultRepository.unlockVaultWithPin(pin)
        } coAnswers { resultFlow.first() }

        viewModel.trySendAction(VaultUnlockAction.UnlockClick)
        assertEquals(
            initialState.copy(dialog = VaultUnlockState.VaultUnlockDialog.Loading),
            viewModel.stateFlow.value,
        )

        val updatedUserId = "updatedUserId"
        mutableUserStateFlow.update {
            it?.copy(
                activeUserId = updatedUserId,
                accounts = listOf(DEFAULT_ACCOUNT.copy(userId = updatedUserId)),
            )
        }
        resultFlow.tryEmit(VaultUnlockResult.GenericError)

        assertEquals(
            initialState.copy(dialog = null),
            viewModel.stateFlow.value,
        )
        coVerify {
            vaultRepository.unlockVaultWithPin(pin)
        }
    }

    @Test
    fun `on BiometricsLockOut should log the current user out`() = runTest {
        every { authRepository.logout() } just runs
        val viewModel = createViewModel()

        viewModel.trySendAction(VaultUnlockAction.BiometricsLockOut)

        verify(exactly = 1) {
            authRepository.logout()
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on BiometricsUnlockSuccess should display error dialog on unlockVaultWithBiometrics AuthenticationError`() {
        val initialState = DEFAULT_STATE.copy(isBiometricEnabled = true)
        mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
            accounts = listOf(DEFAULT_ACCOUNT.copy(isBiometricsEnabled = true)),
        )
        val viewModel = createViewModel(state = initialState)
        coEvery {
            vaultRepository.unlockVaultWithBiometrics()
        } returns VaultUnlockResult.AuthenticationError

        viewModel.trySendAction(VaultUnlockAction.BiometricsUnlockSuccess(CIPHER))

        assertEquals(
            initialState.copy(
                dialog = VaultUnlockState.VaultUnlockDialog.Error(
                    R.string.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
        coVerify {
            vaultRepository.unlockVaultWithBiometrics()
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on BiometricsUnlockSuccess should display error dialog on unlockVaultWithBiometrics GenericError`() {
        val initialState = DEFAULT_STATE.copy(isBiometricEnabled = true)
        mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
            accounts = listOf(DEFAULT_ACCOUNT.copy(isBiometricsEnabled = true)),
        )
        val viewModel = createViewModel(state = initialState)
        coEvery {
            vaultRepository.unlockVaultWithBiometrics()
        } returns VaultUnlockResult.GenericError

        viewModel.trySendAction(VaultUnlockAction.BiometricsUnlockSuccess(CIPHER))

        assertEquals(
            initialState.copy(
                dialog = VaultUnlockState.VaultUnlockDialog.Error(
                    R.string.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
        coVerify {
            vaultRepository.unlockVaultWithBiometrics()
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on BiometricsUnlockSuccess should display error dialog on unlockVaultWithBiometrics InvalidStateError`() {
        val initialState = DEFAULT_STATE.copy(isBiometricEnabled = true)
        mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
            accounts = listOf(DEFAULT_ACCOUNT.copy(isBiometricsEnabled = true)),
        )
        val viewModel = createViewModel(state = initialState)
        coEvery {
            vaultRepository.unlockVaultWithBiometrics()
        } returns VaultUnlockResult.InvalidStateError

        viewModel.trySendAction(VaultUnlockAction.BiometricsUnlockSuccess(CIPHER))

        assertEquals(
            initialState.copy(
                dialog = VaultUnlockState.VaultUnlockDialog.Error(
                    R.string.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
        coVerify {
            vaultRepository.unlockVaultWithBiometrics()
        }
    }

    @Test
    fun `on BiometricsUnlockSuccess should clear dialog on unlockVaultWithBiometrics success`() {
        val initialState = DEFAULT_STATE.copy(isBiometricEnabled = true)
        mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
            accounts = listOf(DEFAULT_ACCOUNT.copy(isBiometricsEnabled = true)),
        )
        val viewModel = createViewModel(state = initialState)
        coEvery {
            vaultRepository.unlockVaultWithBiometrics()
        } returns VaultUnlockResult.Success

        viewModel.trySendAction(VaultUnlockAction.BiometricsUnlockSuccess(CIPHER))

        assertEquals(
            initialState.copy(dialog = null),
            viewModel.stateFlow.value,
        )
        coVerify {
            vaultRepository.unlockVaultWithBiometrics()
        }
    }

    @Test
    fun `on BiometricsUnlockSuccess should clear dialog when user has changed`() {
        val initialState = DEFAULT_STATE.copy(isBiometricEnabled = true)
        mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
            accounts = listOf(DEFAULT_ACCOUNT.copy(isBiometricsEnabled = true)),
        )
        val resultFlow = bufferedMutableSharedFlow<VaultUnlockResult>()
        val viewModel = createViewModel(state = initialState)
        coEvery {
            vaultRepository.unlockVaultWithBiometrics()
        } coAnswers { resultFlow.first() }

        viewModel.trySendAction(VaultUnlockAction.BiometricsUnlockSuccess(CIPHER))

        assertEquals(
            initialState.copy(dialog = VaultUnlockState.VaultUnlockDialog.Loading),
            viewModel.stateFlow.value,
        )
        val updatedUserId = "updatedUserId"
        mutableUserStateFlow.update {
            it?.copy(
                activeUserId = updatedUserId,
                accounts = listOf(DEFAULT_ACCOUNT.copy(userId = updatedUserId)),
            )
        }
        resultFlow.tryEmit(VaultUnlockResult.GenericError)
        assertEquals(initialState.copy(dialog = null), viewModel.stateFlow.value)
        coVerify {
            vaultRepository.unlockVaultWithBiometrics()
        }
    }

    @Test
    fun `on BiometricsUnlockSuccess should set isBiometricsValid to false with null cipher`() {
        val initialState = DEFAULT_STATE.copy(
            isBiometricEnabled = true,
            isBiometricsValid = true,
        )
        mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
            accounts = listOf(DEFAULT_ACCOUNT.copy(isBiometricsEnabled = true)),
        )
        val viewModel = createViewModel(state = initialState)
        coEvery {
            vaultRepository.unlockVaultWithBiometrics()
        } returns VaultUnlockResult.Success

        viewModel.trySendAction(VaultUnlockAction.BiometricsUnlockSuccess(cipher = null))

        assertEquals(
            initialState.copy(
                dialog = null,
                isBiometricsValid = false,
            ),
            viewModel.stateFlow.value,
        )
    }

    private fun createViewModel(
        state: VaultUnlockState? = null,
        unlockType: UnlockType = UnlockType.STANDARD,
        environmentRepo: EnvironmentRepository = environmentRepository,
        vaultRepo: VaultRepository = vaultRepository,
        biometricsEncryptionManager: BiometricsEncryptionManager = encryptionManager,
    ): VaultUnlockViewModel = VaultUnlockViewModel(
        savedStateHandle = SavedStateHandle().apply {
            set("state", state)
            set("unlock_type", unlockType)
        },
        authRepository = authRepository,
        vaultRepo = vaultRepo,
        environmentRepo = environmentRepo,
        biometricsEncryptionManager = biometricsEncryptionManager,
    )
}

private val CIPHER = mockk<Cipher>()
private const val USER_ID: String = "activeUserId"
private val DEFAULT_STATE: VaultUnlockState = VaultUnlockState(
    accountSummaries = listOf(
        AccountSummary(
            userId = "activeUserId",
            name = "Active User",
            email = "active@bitwarden.com",
            avatarColorHex = "#aa00aa",
            environmentLabel = "bitwarden.com",
            isActive = true,
            isLoggedIn = true,
            isVaultUnlocked = true,
        ),
    ),
    avatarColorString = "#aa00aa",
    email = "active@bitwarden.com",
    hideInput = false,
    initials = "AU",
    dialog = null,
    environmentUrl = Environment.Us.label,
    input = "",
    isBiometricsValid = true,
    isBiometricEnabled = false,
    showAccountMenu = true,
    userId = USER_ID,
    vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
)

private val TRUSTED_DEVICE: UserState.TrustedDevice = UserState.TrustedDevice(
    isDeviceTrusted = false,
    hasMasterPassword = false,
    hasAdminApproval = false,
    hasLoginApprovingDevice = false,
    hasResetPasswordPermission = false,
)

private val DEFAULT_ACCOUNT = UserState.Account(
    userId = USER_ID,
    name = "Active User",
    email = "active@bitwarden.com",
    environment = Environment.Us,
    avatarColorHex = "#aa00aa",
    isPremium = true,
    isLoggedIn = true,
    isVaultUnlocked = true,
    needsPasswordReset = false,
    isBiometricsEnabled = false,
    organizations = emptyList(),
    needsMasterPassword = false,
    trustedDevice = null,
)

private val DEFAULT_USER_STATE = UserState(
    activeUserId = USER_ID,
    accounts = listOf(DEFAULT_ACCOUNT),
)
