package com.x8bit.bitwarden.ui.auth.feature.removepassword

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.vault.datasource.network.model.OrganizationType
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
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
    fun `ContinueClick calls does nothing`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(RemovePasswordAction.ContinueClick)
            expectNoEvents()
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
            shouldUseKeyConnector = true,
            role = OrganizationType.USER,
        ),
    ),
    needsMasterPassword = false,
    trustedDevice = null,
    hasMasterPassword = true,
)

private val DEFAULT_USER_STATE = UserState(
    activeUserId = USER_ID,
    accounts = listOf(DEFAULT_ACCOUNT),
)
