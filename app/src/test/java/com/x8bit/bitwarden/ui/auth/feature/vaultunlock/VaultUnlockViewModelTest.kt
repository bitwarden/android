package com.x8bit.bitwarden.ui.auth.feature.vaultunlock

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.LogoutReason
import com.x8bit.bitwarden.data.auth.repository.model.SwitchAccountResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.data.autofill.fido2.manager.Fido2CredentialManager
import com.x8bit.bitwarden.data.autofill.fido2.model.createMockFido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.autofill.fido2.model.createMockFido2GetCredentialsRequest
import com.x8bit.bitwarden.data.platform.manager.AppResumeManager
import com.x8bit.bitwarden.data.platform.manager.BiometricsEncryptionManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.data.vault.manager.VaultLockManager
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.model.UnlockType
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.util.unlockScreenInputLabel
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
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
import org.junit.jupiter.api.Assertions.assertFalse
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
        every { logout(reason = any()) } just runs
        every { logout(userId = any(), reason = any()) } just runs
        every { switchAccount(any()) } returns SwitchAccountResult.AccountSwitched
    }
    private val vaultRepository: VaultRepository = mockk(relaxed = true) {
        every { lockVault(any(), any()) } just runs
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
    private val fido2CredentialManager: Fido2CredentialManager = mockk {
        every { isUserVerified } returns true
        every { isUserVerified = any() } just runs
    }

    private val specialCircumstanceManager: SpecialCircumstanceManager = mockk {
        every { specialCircumstance } returns null
        every { specialCircumstance = any() } answers { }
    }

    private val appResumeManager: AppResumeManager = mockk {
        every { getResumeSpecialCircumstance() } returns null
    }

    private val vaultLockManager: VaultLockManager = mockk(relaxed = true) {
        every { isFromLockFlow } returns false
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
                    hasMasterPassword = false,
                ),
            ),
        )
        createViewModel()

        verify(exactly = 1) {
            authRepository.logout(
                reason = LogoutReason.InvalidState(source = "VaultUnlockViewModel"),
            )
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
                    hasMasterPassword = false,
                ),
            ),
        )
        createViewModel()

        verify(exactly = 0) {
            authRepository.logout(reason = any())
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
                    hasMasterPassword = false,
                ),
            ),
        )
        createViewModel()

        verify(exactly = 0) {
            authRepository.logout(reason = any())
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
    fun `showAccountMenu should be true when unlockType is not STANDARD`() {
        val viewModel = createViewModel(unlockType = UnlockType.TDE)
        assertFalse(viewModel.stateFlow.value.showAccountMenu)
    }

    @Test
    fun `showAccountMenu should be false when unlocking for FIDO 2 credential discovery`() {
        every {
            specialCircumstanceManager.specialCircumstance
        } returns SpecialCircumstance.Fido2GetCredentials(
            createMockFido2GetCredentialsRequest(number = 1),
        )
        val viewModel = createViewModel()

        assertFalse(viewModel.stateFlow.value.showAccountMenu)
    }

    @Test
    fun `showAccountMenu should be false when unlocking for FIDO 2 credential authentication`() {
        every {
            specialCircumstanceManager.specialCircumstance
        } returns SpecialCircumstance.Fido2Assertion(
            createMockFido2CredentialAssertionRequest(number = 1),
        )
        val viewModel = createViewModel()

        assertFalse(viewModel.stateFlow.value.showAccountMenu)
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
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = FirstTimeState(showImportLoginsCard = true),
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
                        hasMasterPassword = true,
                        isUsingKeyConnector = false,
                        onboardingStatus = OnboardingStatus.COMPLETE,
                        firstTimeState = FirstTimeState(showImportLoginsCard = true),
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

    @Suppress("MaxLineLength")
    @Test
    fun `UserState updates with a FIDO2 GetCredentialsRequest should switch accounts when the requested user is not the active user`() {
        val mockFido2GetCredentialsRequest = createMockFido2GetCredentialsRequest(number = 1)
        val initialState = DEFAULT_STATE.copy(
            fido2GetCredentialsRequest = mockFido2GetCredentialsRequest,
            accountSummaries = listOf(
                DEFAULT_ACCOUNT.copy(isVaultUnlocked = false)
                    .toAccountSummary(isActive = true),
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

        verify {
            authRepository.switchAccount(mockFido2GetCredentialsRequest.userId)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `UserState updates with a FIDO2 GetCredentialsRequest should not switch accounts when the requested user is the active user`() {
        val mockFido2GetCredentialsRequest = createMockFido2GetCredentialsRequest(
            number = 1,
            userId = DEFAULT_USER_STATE.activeUserId,
        )
        val initialState = DEFAULT_STATE.copy(
            fido2GetCredentialsRequest = mockFido2GetCredentialsRequest,
            accountSummaries = listOf(
                DEFAULT_ACCOUNT.copy(isVaultUnlocked = false)
                    .toAccountSummary(isActive = true),
            ),
            userId = mockFido2GetCredentialsRequest.userId,
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

        verify(exactly = 0) {
            authRepository.switchAccount(any())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `UserState updates with a FIDO2 CredentialAssertionRequest should switch accounts when the requested user is not the active user`() {
        val mockFido2CredentialAssertionRequest =
            createMockFido2CredentialAssertionRequest(number = 1)
        val initialState = DEFAULT_STATE.copy(
            fido2CredentialAssertionRequest = mockFido2CredentialAssertionRequest,
            accountSummaries = listOf(
                DEFAULT_ACCOUNT.copy(isVaultUnlocked = false)
                    .toAccountSummary(isActive = true),
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

        verify {
            authRepository.switchAccount(mockFido2CredentialAssertionRequest.userId)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `UserState updates with a FIDO2 CredentialAssertionRequest should not switch accounts when the requested user is the active user`() {
        val mockFido2CredentialAssertionRequest =
            createMockFido2CredentialAssertionRequest(
                number = 1,
                userId = DEFAULT_USER_STATE.activeUserId,
            )
        val initialState = DEFAULT_STATE.copy(
            fido2CredentialAssertionRequest = mockFido2CredentialAssertionRequest,
            accountSummaries = listOf(
                DEFAULT_ACCOUNT.copy(isVaultUnlocked = false)
                    .toAccountSummary(isActive = true),
            ),
            userId = mockFido2CredentialAssertionRequest.userId,
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

        verify(exactly = 0) {
            authRepository.switchAccount(any())
        }
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
    @Suppress("MaxLineLength")
    fun `on BiometricsUnlockClick should not emit PromptForBiometrics when isFromLockFlow is true`() =
        runTest {
            val initialState =
                DEFAULT_STATE.copy(
                    isBiometricsValid = true,
                    isBiometricEnabled = true,
                    isFromLockFlow = true,
                )
            val viewModel = createViewModel(
                state = initialState,
            )

            viewModel.eventFlow.test {
                expectNoEvents()
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `on BiometricsUnlockClick should disable isBiometricsValid and show message when cipher is null and integrity check returns false`() {
        val initialState = DEFAULT_STATE.copy(isBiometricsValid = true)
        val viewModel = createViewModel(state = initialState)
        every { encryptionManager.getOrCreateCipher(USER_ID) } returns null
        every { encryptionManager.isAccountBiometricIntegrityValid(USER_ID) } returns false

        viewModel.trySendAction(VaultUnlockAction.BiometricsUnlockClick)
        assertEquals(
            initialState.copy(
                isBiometricsValid = false,
                showBiometricInvalidatedMessage = true,
            ),
            viewModel.stateFlow.value,
        )
        verify { encryptionManager.getOrCreateCipher(USER_ID) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on BiometricsUnlockClick should disable isBiometricsValid and not show message when cipher is null and integrity check returns true`() {
        val initialState = DEFAULT_STATE.copy(isBiometricsValid = true)
        val viewModel = createViewModel(state = initialState)
        every { encryptionManager.getOrCreateCipher(USER_ID) } returns null
        every { encryptionManager.isAccountBiometricIntegrityValid(USER_ID) } returns true

        viewModel.trySendAction(VaultUnlockAction.BiometricsUnlockClick)
        assertEquals(
            initialState.copy(
                isBiometricsValid = false,
                showBiometricInvalidatedMessage = false,
            ),
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

    @Suppress("MaxLineLength")
    @Test
    fun `on DismissDialog should emit Fido2GetCredentialsError when state has Fido2GetCredentialsRequest`() =
        runTest {
            val initialState = DEFAULT_STATE.copy(
                fido2GetCredentialsRequest = createMockFido2GetCredentialsRequest(number = 1),
            )
            val viewModel = createViewModel(state = initialState)
            viewModel.trySendAction(VaultUnlockAction.DismissDialog)
            viewModel.eventFlow.test {
                assertEquals(
                    VaultUnlockEvent.Fido2GetCredentialsError(
                        R.string.passkey_operation_failed_because_user_could_not_be_verified.asText(),
                    ),
                    awaitItem(),
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `on DismissDialog should emit Fido2CredentialAssertionError when state has Fido2CredentialAssertionRequest`() =
        runTest {
            val initialState = DEFAULT_STATE.copy(
                fido2CredentialAssertionRequest = createMockFido2CredentialAssertionRequest(
                    number = 1,
                ),
            )
            val viewModel = createViewModel(state = initialState)
            viewModel.trySendAction(VaultUnlockAction.DismissDialog)
            viewModel.eventFlow.test {
                assertEquals(
                    VaultUnlockEvent.Fido2CredentialAssertionError(
                        R.string.passkey_operation_failed_because_user_could_not_be_verified
                            .asText(),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `on ConfirmLogoutClick should call logout on the AuthRepository`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(VaultUnlockAction.ConfirmLogoutClick)
        verify(exactly = 1) {
            authRepository.logout(
                reason = LogoutReason.Click(source = "VaultUnlockViewModel"),
            )
        }
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

        verify { vaultRepository.lockVault(userId = accountUserId, isUserInitiated = true) }
    }

    @Test
    fun `on LogoutAccountClick should call logout for the given account`() {
        val accountUserId = "userId"
        val accountSummary = mockk<AccountSummary> {
            every { userId } returns accountUserId
        }
        val viewModel = createViewModel()

        viewModel.trySendAction(VaultUnlockAction.LogoutAccountClick(accountSummary))

        verify(exactly = 1) {
            authRepository.logout(
                userId = accountUserId,
                reason = LogoutReason.Click(source = "VaultUnlockViewModel"),
            )
        }
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
    fun `switching accounts should prompt for biometrics if new account has biometrics enabled`() =
        runTest {
            val account = DEFAULT_ACCOUNT.copy(
                isVaultUnlocked = false,
                isBiometricsEnabled = true,
            )
            val initialState = DEFAULT_STATE.copy(isBiometricsValid = true)
            val viewModel = createViewModel(state = initialState)
            mutableUserStateFlow.update {
                it?.copy(
                    activeUserId = account.userId,
                    accounts = listOf(account),
                )
            }

            viewModel.eventFlow.test {
                assertEquals(VaultUnlockEvent.PromptForBiometrics(CIPHER), awaitItem())
                expectNoEvents()
            }
            // The initial state causes this to be called as well as the change.
            verify(exactly = 2) {
                encryptionManager.getOrCreateCipher(USER_ID)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `switching accounts should not prompt for biometrics if new account has biometrics enabled`() =
        runTest {
            val account = DEFAULT_ACCOUNT.copy(
                isVaultUnlocked = false,
                isBiometricsEnabled = true,
            )
            val initialState = DEFAULT_STATE.copy(isBiometricsValid = true)
            val viewModel = createViewModel(state = initialState)
            mutableUserStateFlow.update {
                it?.copy(
                    activeUserId = account.userId,
                    accounts = listOf(account),
                    hasPendingAccountAddition = true,
                )
            }

            viewModel.eventFlow.test {
                expectNoEvents()
            }
            // Only the call for the initial state should be called.
            verify(exactly = 1) {
                encryptionManager.getOrCreateCipher(USER_ID)
            }
        }

    @Test
    fun `on UnlockClick for empty password should display error dialog`() {
        val password = ""
        val initialState = DEFAULT_STATE.copy(
            input = password,
            vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
        )
        val viewModel = createViewModel(state = initialState)

        viewModel.trySendAction(VaultUnlockAction.UnlockClick)
        assertEquals(
            initialState.copy(
                dialog = VaultUnlockState.VaultUnlockDialog.Error(
                    R.string.an_error_has_occurred.asText(),
                    R.string.validation_field_required.asText(
                        initialState.vaultUnlockType.unlockScreenInputLabel,
                    ),
                ),
            ),
            viewModel.stateFlow.value,
        )
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
        } returns VaultUnlockResult.AuthenticationError(error = Throwable("Fail"))

        viewModel.trySendAction(VaultUnlockAction.UnlockClick)
        assertEquals(
            initialState.copy(
                dialog = VaultUnlockState.VaultUnlockDialog.Error(
                    R.string.an_error_has_occurred.asText(),
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
        val error = Throwable("Fail")
        coEvery {
            vaultRepository.unlockVaultWithMasterPassword(password)
        } returns VaultUnlockResult.GenericError(error = error)

        viewModel.trySendAction(VaultUnlockAction.UnlockClick)
        assertEquals(
            initialState.copy(
                dialog = VaultUnlockState.VaultUnlockDialog.Error(
                    R.string.an_error_has_occurred.asText(),
                    R.string.generic_error_message.asText(),
                    throwable = error,
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
        val error = Throwable("Fail")
        val viewModel = createViewModel(state = initialState)
        coEvery {
            vaultRepository.unlockVaultWithMasterPassword(password)
        } returns VaultUnlockResult.InvalidStateError(error = error)

        viewModel.trySendAction(VaultUnlockAction.UnlockClick)
        assertEquals(
            initialState.copy(
                dialog = VaultUnlockState.VaultUnlockDialog.Error(
                    R.string.an_error_has_occurred.asText(),
                    R.string.generic_error_message.asText(),
                    throwable = error,
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
        val error = Throwable("Fail")
        resultFlow.tryEmit(VaultUnlockResult.GenericError(error = error))

        assertEquals(
            initialState.copy(dialog = null),
            viewModel.stateFlow.value,
        )
        coVerify {
            vaultRepository.unlockVaultWithMasterPassword(password)
        }
    }

    @Test
    fun `on UnlockClick for empty PIN should display error dialog`() {
        val password = ""
        val initialState = DEFAULT_STATE.copy(
            input = password,
            vaultUnlockType = VaultUnlockType.PIN,
        )
        val viewModel = createViewModel(state = initialState)

        viewModel.trySendAction(VaultUnlockAction.UnlockClick)
        assertEquals(
            initialState.copy(
                dialog = VaultUnlockState.VaultUnlockDialog.Error(
                    R.string.an_error_has_occurred.asText(),
                    R.string.validation_field_required.asText(
                        initialState.vaultUnlockType.unlockScreenInputLabel,
                    ),
                ),
            ),
            viewModel.stateFlow.value,
        )
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
        } returns VaultUnlockResult.AuthenticationError(error = Throwable("Fail"))

        viewModel.trySendAction(VaultUnlockAction.UnlockClick)
        assertEquals(
            initialState.copy(
                dialog = VaultUnlockState.VaultUnlockDialog.Error(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.invalid_pin.asText(),
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
        val error = Throwable("Fail")
        coEvery {
            vaultRepository.unlockVaultWithPin(pin)
        } returns VaultUnlockResult.GenericError(error = error)

        viewModel.trySendAction(VaultUnlockAction.UnlockClick)
        assertEquals(
            initialState.copy(
                dialog = VaultUnlockState.VaultUnlockDialog.Error(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.generic_error_message.asText(),
                    throwable = error,
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
        val error = Throwable("Fail")
        coEvery {
            vaultRepository.unlockVaultWithPin(pin)
        } returns VaultUnlockResult.InvalidStateError(error = error)

        viewModel.trySendAction(VaultUnlockAction.UnlockClick)
        assertEquals(
            initialState.copy(
                dialog = VaultUnlockState.VaultUnlockDialog.Error(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.generic_error_message.asText(),
                    throwable = error,
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
        val error = Throwable("Fail")
        resultFlow.tryEmit(VaultUnlockResult.GenericError(error = error))

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
        every { authRepository.logout(reason = any()) } just runs
        val viewModel = createViewModel()

        viewModel.trySendAction(VaultUnlockAction.BiometricsLockOut)

        verify(exactly = 1) {
            authRepository.logout(reason = LogoutReason.Biometrics.Lockout)
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
        val error = Throwable("Fail")
        coEvery {
            vaultRepository.unlockVaultWithBiometrics(cipher = CIPHER)
        } returns VaultUnlockResult.AuthenticationError(error = error)

        viewModel.trySendAction(VaultUnlockAction.BiometricsUnlockSuccess(CIPHER))

        assertEquals(
            initialState.copy(
                dialog = VaultUnlockState.VaultUnlockDialog.Error(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.generic_error_message.asText(),
                    throwable = error,
                ),
            ),
            viewModel.stateFlow.value,
        )
        coVerify(exactly = 1) {
            vaultRepository.unlockVaultWithBiometrics(cipher = CIPHER)
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
        val error = Throwable("Fail")
        coEvery {
            vaultRepository.unlockVaultWithBiometrics(cipher = CIPHER)
        } returns VaultUnlockResult.GenericError(error = error)

        viewModel.trySendAction(VaultUnlockAction.BiometricsUnlockSuccess(CIPHER))

        assertEquals(
            initialState.copy(
                dialog = VaultUnlockState.VaultUnlockDialog.Error(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.generic_error_message.asText(),
                    throwable = error,
                ),
            ),
            viewModel.stateFlow.value,
        )
        coVerify(exactly = 1) {
            vaultRepository.unlockVaultWithBiometrics(cipher = CIPHER)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on BiometricsUnlockSuccess should disable biometrics and display error dialog on unlockVaultWithBiometrics BiometricDecodingError`() {
        val initialState = DEFAULT_STATE.copy(isBiometricEnabled = true)
        mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
            accounts = listOf(DEFAULT_ACCOUNT.copy(isBiometricsEnabled = true)),
        )
        val viewModel = createViewModel(state = initialState)
        coEvery {
            vaultRepository.unlockVaultWithBiometrics(cipher = CIPHER)
        } returns VaultUnlockResult.BiometricDecodingError(error = Throwable("Fail"))
        every { encryptionManager.clearBiometrics(userId = USER_ID) } just runs

        viewModel.trySendAction(VaultUnlockAction.BiometricsUnlockSuccess(CIPHER))

        assertEquals(
            initialState.copy(
                isBiometricsValid = false,
                dialog = VaultUnlockState.VaultUnlockDialog.Error(
                    title = R.string.biometrics_failed.asText(),
                    message = R.string.biometrics_decoding_failure.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
        coVerify(exactly = 1) {
            encryptionManager.clearBiometrics(userId = USER_ID)
            vaultRepository.unlockVaultWithBiometrics(cipher = CIPHER)
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
        val error = Throwable("Fail")
        coEvery {
            vaultRepository.unlockVaultWithBiometrics(cipher = CIPHER)
        } returns VaultUnlockResult.InvalidStateError(error = error)

        viewModel.trySendAction(VaultUnlockAction.BiometricsUnlockSuccess(CIPHER))

        assertEquals(
            initialState.copy(
                dialog = VaultUnlockState.VaultUnlockDialog.Error(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.generic_error_message.asText(),
                    throwable = error,
                ),
            ),
            viewModel.stateFlow.value,
        )
        coVerify(exactly = 1) {
            vaultRepository.unlockVaultWithBiometrics(cipher = CIPHER)
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
            vaultRepository.unlockVaultWithBiometrics(cipher = CIPHER)
        } returns VaultUnlockResult.Success

        viewModel.trySendAction(VaultUnlockAction.BiometricsUnlockSuccess(CIPHER))

        assertEquals(
            initialState.copy(dialog = null),
            viewModel.stateFlow.value,
        )
        coVerify(exactly = 1) {
            vaultRepository.unlockVaultWithBiometrics(cipher = CIPHER)
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
            vaultRepository.unlockVaultWithBiometrics(cipher = CIPHER)
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
        val error = Throwable("Fail")
        resultFlow.tryEmit(VaultUnlockResult.GenericError(error = error))
        assertEquals(initialState.copy(dialog = null), viewModel.stateFlow.value)
        coVerify(exactly = 1) {
            vaultRepository.unlockVaultWithBiometrics(cipher = CIPHER)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveVaultUnlockResult should set FIDO 2 user verification state to verified when result is Success`() {
        val viewModel = createViewModel(
            state = DEFAULT_STATE.copy(
                fido2GetCredentialsRequest = mockk(relaxed = true),
            ),
        )
        viewModel.trySendAction(
            VaultUnlockAction.Internal.ReceiveVaultUnlockResult(
                userId = "activeUserId",
                vaultUnlockResult = VaultUnlockResult.Success,
                isBiometricLogin = true,
            ),
        )

        verify { fido2CredentialManager.isUserVerified = true }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveVaultUnlockResult should set FIDO 2 user verification state to not verified when result is not Success`() {
        val viewModel = createViewModel()

        viewModel.trySendAction(
            VaultUnlockAction.Internal.ReceiveVaultUnlockResult(
                userId = "activeUserId",
                vaultUnlockResult = VaultUnlockResult.InvalidStateError(error = null),
                isBiometricLogin = false,
            ),
        )

        verify { fido2CredentialManager.isUserVerified = false }
    }

    @Test
    fun `on BiometricsNoLongerSupported should show correct dialog state`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(VaultUnlockAction.BiometricsNoLongerSupported)
        assertEquals(
            DEFAULT_STATE.copy(
                dialog = VaultUnlockState.VaultUnlockDialog.BiometricsNoLongerSupported,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on DismissBiometricsNoLongerSupportedDialog should dismiss dialog state and log the user out`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(VaultUnlockAction.DismissBiometricsNoLongerSupportedDialog)
        assertEquals(
            DEFAULT_STATE.copy(
                dialog = null,
            ),
            viewModel.stateFlow.value,
        )
        verify(exactly = 1) {
            authRepository.logout(reason = LogoutReason.Biometrics.NoLongerSupported)
            authRepository.hasPendingAccountAddition = true
        }
    }

    @Suppress("LongParameterList")
    private fun createViewModel(
        state: VaultUnlockState? = null,
        unlockType: UnlockType = UnlockType.STANDARD,
        environmentRepo: EnvironmentRepository = environmentRepository,
        vaultRepo: VaultRepository = vaultRepository,
        biometricsEncryptionManager: BiometricsEncryptionManager = encryptionManager,
        lockManager: VaultLockManager = vaultLockManager,
    ): VaultUnlockViewModel = VaultUnlockViewModel(
        savedStateHandle = SavedStateHandle().apply {
            set("state", state)
            set("unlock_type", unlockType)
        },
        authRepository = authRepository,
        vaultRepo = vaultRepo,
        environmentRepo = environmentRepo,
        biometricsEncryptionManager = biometricsEncryptionManager,
        fido2CredentialManager = fido2CredentialManager,
        specialCircumstanceManager = specialCircumstanceManager,
        appResumeManager = appResumeManager,
        vaultLockManager = lockManager,
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
    showBiometricInvalidatedMessage = false,
    userId = USER_ID,
    vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
    hasMasterPassword = true,
    isFromLockFlow = false,
)

private val TRUSTED_DEVICE: UserState.TrustedDevice = UserState.TrustedDevice(
    isDeviceTrusted = false,
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
    hasMasterPassword = true,
    isUsingKeyConnector = false,
    onboardingStatus = OnboardingStatus.COMPLETE,
    firstTimeState = FirstTimeState(showImportLoginsCard = true),
)

private val DEFAULT_USER_STATE = UserState(
    activeUserId = USER_ID,
    accounts = listOf(DEFAULT_ACCOUNT),
)
