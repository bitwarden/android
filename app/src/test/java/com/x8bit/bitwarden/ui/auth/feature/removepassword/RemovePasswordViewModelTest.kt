package com.x8bit.bitwarden.ui.auth.feature.removepassword

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.RemovePasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.vault.datasource.network.model.OrganizationType
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RemovePasswordViewModelTest : BaseViewModelTest() {
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(DEFAULT_USER_STATE)
    private val authRepository: AuthRepository = mockk {
        every { userStateFlow } returns mutableUserStateFlow
    }

    @Test
    fun `ContinueClick with blank input should show error dialog`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(RemovePasswordAction.ContinueClick)
        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = RemovePasswordState.DialogState.Error(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.validation_field_required
                        .asText(R.string.master_password.asText()),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `ContinueClick with input and remove password error should show error dialog`() = runTest {
        val password = "123"
        val initialState = DEFAULT_STATE.copy(input = password)
        val viewModel = createViewModel(state = initialState)
        coEvery {
            authRepository.removePassword(masterPassword = password)
        } returns RemovePasswordResult.Error

        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
            viewModel.trySendAction(RemovePasswordAction.ContinueClick)
            assertEquals(
                initialState.copy(
                    dialogState = RemovePasswordState.DialogState.Loading(
                        title = R.string.deleting.asText(),
                    ),
                ),
                awaitItem(),
            )
            assertEquals(
                initialState.copy(
                    dialogState = RemovePasswordState.DialogState.Error(
                        title = R.string.an_error_has_occurred.asText(),
                        message = R.string.generic_error_message.asText(),
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `ContinueClick with input and remove password success should dismiss dialog`() = runTest {
        val password = "123"
        val initialState = DEFAULT_STATE.copy(input = password)
        val viewModel = createViewModel(state = initialState)
        coEvery {
            authRepository.removePassword(masterPassword = password)
        } returns RemovePasswordResult.Success

        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
            viewModel.trySendAction(RemovePasswordAction.ContinueClick)
            assertEquals(
                initialState.copy(
                    dialogState = RemovePasswordState.DialogState.Loading(
                        title = R.string.deleting.asText(),
                    ),
                ),
                awaitItem(),
            )
            assertEquals(initialState, awaitItem())
        }
    }

    @Test
    fun `InputChanged updates the state`() {
        val input = "123"
        val viewModel = createViewModel()
        viewModel.trySendAction(RemovePasswordAction.InputChanged(input = input))
        assertEquals(DEFAULT_STATE.copy(input = input), viewModel.stateFlow.value)
    }

    @Test
    fun `DialogDismiss calls clears the dialog state`() = runTest {
        val initialState = DEFAULT_STATE.copy(
            dialogState = RemovePasswordState.DialogState.Error(
                title = "title".asText(),
                message = "message".asText(),
            ),
        )
        val viewModel = createViewModel(initialState)

        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
            viewModel.trySendAction(RemovePasswordAction.DialogDismiss)
            assertEquals(DEFAULT_STATE, awaitItem())
        }
    }

    private fun createViewModel(
        state: RemovePasswordState? = null,
    ): RemovePasswordViewModel =
        RemovePasswordViewModel(
            authRepository = authRepository,
            savedStateHandle = SavedStateHandle(mapOf("state" to state)),
        )
}

private const val ORGANIZATION_NAME: String = "My org"
private val DEFAULT_STATE = RemovePasswordState(
    input = "",
    dialogState = null,
    description = R.string
        .organization_is_using_sso_with_a_self_hosted_key_server
        .asText(ORGANIZATION_NAME),
)

private const val USER_ID: String = "user_id"
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
    organizations = listOf(
        Organization(
            id = "orgId",
            name = ORGANIZATION_NAME,
            shouldManageResetPassword = false,
            shouldUseKeyConnector = true,
            role = OrganizationType.USER,
            shouldUsersGetPremium = false,
        ),
    ),
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
