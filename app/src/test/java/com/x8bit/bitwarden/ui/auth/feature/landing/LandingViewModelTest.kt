package com.x8bit.bitwarden.ui.auth.feature.landing

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LandingViewModelTest : BaseViewModelTest() {
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
    fun `ContinueButtonClick should emit NavigateToLogin`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(LandingAction.EmailInputChanged("input"))
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(LandingAction.ContinueButtonClick)
            assertEquals(
                LandingEvent.NavigateToLogin("input"),
                awaitItem(),
            )
        }
    }

    @Test
    fun `ContinueButtonClick with empty input should do nothing`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(LandingAction.ContinueButtonClick)
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
    fun `RegionOptionSelect should update value of selected region`() = runTest {
        val inputRegion = LandingState.RegionOption.BITWARDEN_EU
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            awaitItem()
            viewModel.trySendAction(LandingAction.RegionOptionSelect(inputRegion))
            assertEquals(
                DEFAULT_STATE.copy(selectedRegion = LandingState.RegionOption.BITWARDEN_EU),
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
        savedStateHandle = savedStateHandle,
    )

    //endregion Helper methods

    companion object {
        private val DEFAULT_STATE = LandingState(
            emailInput = "",
            isContinueButtonEnabled = false,
            isRememberMeEnabled = false,
            selectedRegion = LandingState.RegionOption.BITWARDEN_US,
        )
    }
}
