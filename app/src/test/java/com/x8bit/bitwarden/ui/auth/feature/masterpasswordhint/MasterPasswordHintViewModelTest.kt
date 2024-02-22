package com.x8bit.bitwarden.ui.auth.feature.masterpasswordhint

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.PasswordHintResult
import com.x8bit.bitwarden.data.platform.manager.NetworkConnectionManager
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MasterPasswordHintViewModelTest : BaseViewModelTest() {

    private val authRepository: AuthRepository = mockk()
    private val networkConnectionManager: NetworkConnectionManager = mockk()

    @Test
    fun `initial state should be correct`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
        }
    }

    @Test
    fun `SubmitClick with valid email should show success dialog`() = runTest {
        val validEmail = "test@example.com"
        every { networkConnectionManager.isNetworkConnected } returns true
        coEvery {
            authRepository.passwordHintRequest(validEmail)
        } returns PasswordHintResult.Success

        val viewModel = createViewModel()
        viewModel.trySendAction(MasterPasswordHintAction.EmailInputChange(validEmail))

        viewModel.trySendAction(MasterPasswordHintAction.SubmitClick)

        viewModel.stateFlow.test {
            val expectedSuccessState = MasterPasswordHintState(
                dialog = MasterPasswordHintState.DialogState.PasswordHintSent,
                emailInput = validEmail,
            )
            assertEquals(expectedSuccessState, awaitItem())
        }
    }

    @Test
    fun `SubmitClick with no network connection should show error dialog`() = runTest {
        val email = "test@example.com"
        every { networkConnectionManager.isNetworkConnected } returns false
        val viewModel = createViewModel()

        val expectedErrorState = MasterPasswordHintState(
            dialog = MasterPasswordHintState.DialogState.Error(
                title = R.string.internet_connection_required_title.asText(),
                message = R.string.internet_connection_required_message.asText(),
            ),
            emailInput = email,
        )

        viewModel.trySendAction(MasterPasswordHintAction.EmailInputChange(email))
        viewModel.trySendAction(MasterPasswordHintAction.SubmitClick)

        viewModel.stateFlow.test {
            assertEquals(expectedErrorState, awaitItem())
        }
    }

    @Test
    fun `SubmitClick with empty email field should show error dialog`() = runTest {
        val emptyEmail = ""
        every { networkConnectionManager.isNetworkConnected } returns true
        val viewModel = createViewModel()

        val expectedErrorState = MasterPasswordHintState(
            dialog = MasterPasswordHintState.DialogState.Error(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.validation_field_required.asText(
                    R.string.email_address.asText(),
                ),
            ),
            emailInput = emptyEmail,
        )

        viewModel.trySendAction(MasterPasswordHintAction.EmailInputChange(emptyEmail))
        viewModel.trySendAction(MasterPasswordHintAction.SubmitClick)

        viewModel.stateFlow.test {
            assertEquals(expectedErrorState, awaitItem())
        }
    }

    @Test
    fun `SubmitClick with invalid email should show error dialog`() = runTest {
        val invalidEmail = "invalidemail"
        every { networkConnectionManager.isNetworkConnected } returns true
        val viewModel = createViewModel()

        val expectedErrorState = MasterPasswordHintState(
            dialog = MasterPasswordHintState.DialogState.Error(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.invalid_email.asText(),
            ),
            emailInput = invalidEmail,
        )

        viewModel.trySendAction(MasterPasswordHintAction.EmailInputChange(invalidEmail))
        viewModel.trySendAction(MasterPasswordHintAction.SubmitClick)

        viewModel.stateFlow.test {
            assertEquals(expectedErrorState, awaitItem())
        }
    }

    @Test
    fun `on DismissDialog should update state to remove dialog`() = runTest {
        val initialState = MasterPasswordHintState(
            dialog = MasterPasswordHintState.DialogState.Error(message = "Some error".asText()),
            emailInput = "test@example.com",
        )
        val viewModel = createViewModel(initialState)

        viewModel.trySendAction(MasterPasswordHintAction.DismissDialog)

        viewModel.stateFlow.test {
            val expectedState = initialState.copy(dialog = null)
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `on EmailInputChange should update emailInput in state`() = runTest {
        val viewModel = createViewModel()
        val newEmail = "new@example.com"

        viewModel.trySendAction(MasterPasswordHintAction.EmailInputChange(newEmail))

        viewModel.stateFlow.test {
            val expectedState = MasterPasswordHintState(
                dialog = null,
                emailInput = newEmail,
            )
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `on CloseClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(MasterPasswordHintAction.CloseClick)
            assertEquals(MasterPasswordHintEvent.NavigateBack, awaitItem())
        }
    }

    private fun createViewModel(
        state: MasterPasswordHintState? = DEFAULT_STATE,
    ): MasterPasswordHintViewModel = MasterPasswordHintViewModel(
        savedStateHandle = SavedStateHandle().apply { set("state", state) },
        authRepository = authRepository,
        networkConnectionManager = networkConnectionManager,
    )
}

private val DEFAULT_STATE: MasterPasswordHintState = MasterPasswordHintState(
    emailInput = "email",
)
