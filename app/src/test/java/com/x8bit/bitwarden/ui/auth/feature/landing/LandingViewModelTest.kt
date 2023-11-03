package com.x8bit.bitwarden.ui.auth.feature.landing

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.BasicDialogState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LandingViewModelTest : BaseViewModelTest() {

    private val fakeEnvironmentRepository = FakeEnvironmentRepository()

    @Test
    fun `initial state should be correct when there is no remembered email`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
        }
    }

    @Test
    fun `initial state should be correct when there is a remembered email`() = runTest {
        val rememberedEmail = "remembered@gmail.com"
        val viewModel = createViewModel(rememberedEmail = rememberedEmail)
        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(
                    emailInput = rememberedEmail,
                    isContinueButtonEnabled = true,
                    isRememberMeEnabled = true,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `initial state should pull from saved state handle when present`() = runTest {
        val expectedState = DEFAULT_STATE.copy(
            emailInput = "test",
            isContinueButtonEnabled = false,
            isRememberMeEnabled = true,
        )
        val handle = SavedStateHandle(mapOf("state" to expectedState))
        val viewModel = createViewModel(savedStateHandle = handle)
        viewModel.stateFlow.test {
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `ContinueButtonClick with valid email should emit NavigateToLogin`() = runTest {
        val validEmail = "email@bitwarden.com"
        val viewModel = createViewModel()
        viewModel.trySendAction(LandingAction.EmailInputChanged(validEmail))
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(LandingAction.ContinueButtonClick)
            assertEquals(
                LandingEvent.NavigateToLogin(validEmail),
                awaitItem(),
            )
        }
    }

    @Test
    fun `ContinueButtonClick with invalid email should display an error dialog`() = runTest {
        val invalidEmail = "bitwarden.com"
        val viewModel = createViewModel()
        viewModel.trySendAction(LandingAction.EmailInputChanged(invalidEmail))
        val initialState = DEFAULT_STATE.copy(
            emailInput = invalidEmail,
            isContinueButtonEnabled = true,
        )
        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())

            viewModel.actionChannel.trySend(LandingAction.ContinueButtonClick)
            assertEquals(
                initialState.copy(
                    errorDialogState = BasicDialogState.Shown(
                        title = R.string.an_error_has_occurred.asText(),
                        message = R.string.invalid_email.asText(),
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `CreateAccountClick should emit NavigateToCreateAccount`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(LandingAction.CreateAccountClick)
            assertEquals(
                LandingEvent.NavigateToCreateAccount,
                awaitItem(),
            )
        }
    }

    @Test
    fun `RememberMeToggle should update value of isRememberMeToggled`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(LandingAction.RememberMeToggle(true))
            assertEquals(
                viewModel.stateFlow.value,
                DEFAULT_STATE.copy(isRememberMeEnabled = true),
            )
        }
    }

    @Test
    fun `EmailInputUpdated should update value of email input and continue button state`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.stateFlow.test {
                // Ignore initial state
                awaitItem()

                val nonEmptyInput = "input"
                viewModel.trySendAction(LandingAction.EmailInputChanged(nonEmptyInput))
                assertEquals(
                    DEFAULT_STATE.copy(
                        emailInput = nonEmptyInput,
                        isContinueButtonEnabled = true,
                    ),
                    awaitItem(),
                )

                val emptyInput = ""
                viewModel.trySendAction(LandingAction.EmailInputChanged(emptyInput))
                assertEquals(
                    DEFAULT_STATE.copy(
                        emailInput = emptyInput,
                        isContinueButtonEnabled = false,
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `external environment updates should update the selected environment type`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())

            fakeEnvironmentRepository.environment = Environment.Eu

            assertEquals(
                DEFAULT_STATE.copy(
                    selectedEnvironmentType = Environment.Type.EU,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `EnvironmentTypeSelect should update value of selected region for US or EU`() = runTest {
        val inputEnvironmentType = Environment.Type.EU
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            awaitItem()
            viewModel.trySendAction(LandingAction.EnvironmentTypeSelect(inputEnvironmentType))
            assertEquals(
                DEFAULT_STATE.copy(selectedEnvironmentType = Environment.Type.EU),
                awaitItem(),
            )
        }
    }

    @Test
    fun `EnvironmentTypeSelect should emit NavigateToEnvironment for self-hosted`() = runTest {
        val inputEnvironmentType = Environment.Type.SELF_HOSTED
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(LandingAction.EnvironmentTypeSelect(inputEnvironmentType))
            assertEquals(
                LandingEvent.NavigateToEnvironment,
                awaitItem(),
            )
        }
    }

    //region Helper methods

    private fun createViewModel(
        rememberedEmail: String? = null,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): LandingViewModel = LandingViewModel(
        authRepository = mockk(relaxed = true) {
            every { rememberedEmailAddress } returns rememberedEmail
        },
        environmentRepository = fakeEnvironmentRepository,
        savedStateHandle = savedStateHandle,
    )

    //endregion Helper methods

    companion object {
        private val DEFAULT_STATE = LandingState(
            emailInput = "",
            isContinueButtonEnabled = false,
            isRememberMeEnabled = false,
            selectedEnvironmentType = Environment.Type.US,
            errorDialogState = BasicDialogState.Hidden,
        )
    }
}
