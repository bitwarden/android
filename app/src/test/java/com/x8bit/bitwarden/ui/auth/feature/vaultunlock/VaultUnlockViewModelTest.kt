package com.x8bit.bitwarden.ui.auth.feature.vaultunlock

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultUnlockViewModelTest : BaseViewModelTest() {

    private val mutableUserStateFlow = MutableStateFlow<UserState?>(DEFAULT_USER_STATE)
    private val environmentRepository = FakeEnvironmentRepository()
    private val authRepository = mockk<AuthRepository>() {
        every { userStateFlow } returns mutableUserStateFlow
        every { logout() } just runs
    }
    private val vaultRepository = mockk<VaultRepository>()

    @Test
    fun `initial state should be correct when not set`() {
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should be correct when set`() {
        val state = DEFAULT_STATE.copy(
            passwordInput = "pass",
        )
        val viewModel = createViewModel(state = state)
        assertEquals(state, viewModel.stateFlow.value)
    }

    @Test
    fun `environment url should update when environment repo emits an update`() {
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
        environmentRepository.environment = Environment.SelfHosted(
            environmentUrlData = EnvironmentUrlDataJson(base = "https://vault.bitwarden.eu"),
        )
        assertEquals(
            DEFAULT_STATE.copy(environmentUrl = "vault.bitwarden.eu".asText()),
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
    fun `UserState updates with a non-null value update the account information in the state`() {
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
                        email = "active@bitwarden.com",
                        avatarColorHex = "#00aaaa",
                        isPremium = true,
                        isVaultUnlocked = true,
                    ),
                ),
            )

        assertEquals(
            DEFAULT_STATE.copy(
                avatarColorString = "#00aaaa",
                initials = "OU",
                accountSummaries = listOf(
                    AccountSummary(
                        userId = "activeUserId",
                        name = "Other User",
                        email = "active@bitwarden.com",
                        avatarColorHex = "#00aaaa",
                        status = AccountSummary.Status.ACTIVE,
                    ),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on AddAccountClick should emit NavigateToLoginScreen`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultUnlockAction.AddAccountClick)
            assertEquals(VaultUnlockEvent.NavigateToLoginScreen, awaitItem())
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
        viewModel.trySendAction(VaultUnlockAction.PasswordInputChanged(passwordInput = password))
        assertEquals(
            DEFAULT_STATE.copy(passwordInput = password),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on SwitchAccountClick should emit ShowToast`() = runTest {
        val viewModel = createViewModel()
        val accountSummary = mockk<AccountSummary> {
            every { status } returns AccountSummary.Status.ACTIVE
        }
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultUnlockAction.SwitchAccountClick(accountSummary))
            assertEquals(VaultUnlockEvent.ShowToast("Not yet implemented.".asText()), awaitItem())
        }
    }

    @Test
    fun `on UnlockClick should display error dialog on AuthenticationError`() = runTest {
        val password = "abcd1234"
        val initialState = DEFAULT_STATE.copy(passwordInput = password)
        val viewModel = createViewModel(state = initialState)
        coEvery {
            vaultRepository.unlockVaultAndSyncForCurrentUser(password)
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
            vaultRepository.unlockVaultAndSyncForCurrentUser(password)
        }
    }

    @Test
    fun `on UnlockClick should display error dialog on GenericError`() = runTest {
        val password = "abcd1234"
        val initialState = DEFAULT_STATE.copy(passwordInput = password)
        val viewModel = createViewModel(state = initialState)
        coEvery {
            vaultRepository.unlockVaultAndSyncForCurrentUser(password)
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
            vaultRepository.unlockVaultAndSyncForCurrentUser(password)
        }
    }

    @Test
    fun `on UnlockClick should display error dialog on InvalidStateError`() = runTest {
        val password = "abcd1234"
        val initialState = DEFAULT_STATE.copy(passwordInput = password)
        val viewModel = createViewModel(state = initialState)
        coEvery {
            vaultRepository.unlockVaultAndSyncForCurrentUser(password)
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
            vaultRepository.unlockVaultAndSyncForCurrentUser(password)
        }
    }

    @Test
    fun `on UnlockClick should display clear dialog on success`() = runTest {
        val password = "abcd1234"
        val initialState = DEFAULT_STATE.copy(passwordInput = password)
        val viewModel = createViewModel(state = initialState)
        coEvery {
            vaultRepository.unlockVaultAndSyncForCurrentUser(password)
        } returns VaultUnlockResult.Success

        viewModel.trySendAction(VaultUnlockAction.UnlockClick)
        assertEquals(
            initialState.copy(dialog = null),
            viewModel.stateFlow.value,
        )
        coVerify {
            vaultRepository.unlockVaultAndSyncForCurrentUser(password)
        }
    }

    private fun createViewModel(
        state: VaultUnlockState? = DEFAULT_STATE,
        environmentRepo: EnvironmentRepository = environmentRepository,
        vaultRepo: VaultRepository = vaultRepository,
    ): VaultUnlockViewModel = VaultUnlockViewModel(
        savedStateHandle = SavedStateHandle().apply { set("state", state) },
        authRepository = authRepository,
        vaultRepo = vaultRepo,
        environmentRepo = environmentRepo,
    )
}

private val DEFAULT_STATE: VaultUnlockState = VaultUnlockState(
    accountSummaries = listOf(
        AccountSummary(
            userId = "activeUserId",
            name = "Active User",
            email = "active@bitwarden.com",
            avatarColorHex = "#aa00aa",
            status = AccountSummary.Status.ACTIVE,
        ),
    ),
    avatarColorString = "#aa00aa",
    email = "active@bitwarden.com",
    initials = "AU",
    dialog = null,
    environmentUrl = Environment.Us.label,
    passwordInput = "",
)

private val DEFAULT_USER_STATE = UserState(
    activeUserId = "activeUserId",
    accounts = listOf(
        UserState.Account(
            userId = "activeUserId",
            name = "Active User",
            email = "active@bitwarden.com",
            avatarColorHex = "#aa00aa",
            isPremium = true,
            isVaultUnlocked = true,
        ),
    ),
)
