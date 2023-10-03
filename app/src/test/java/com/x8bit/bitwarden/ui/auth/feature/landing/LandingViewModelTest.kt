package com.x8bit.bitwarden.ui.auth.feature.landing

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LandingViewModelTest : BaseViewModelTest() {

    @Test
    fun `initial state should be correct`() = runTest {
        val viewModel = LandingViewModel(SavedStateHandle())
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
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
        val viewModel = LandingViewModel(handle)
        viewModel.stateFlow.test {
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `ContinueButtonClick should emit NavigateToLogin`() = runTest {
        val viewModel = LandingViewModel(SavedStateHandle())
        viewModel.trySendAction(LandingAction.EmailInputChanged("input"))
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(LandingAction.ContinueButtonClick)
            assertEquals(
                LandingEvent.NavigateToLogin("input", "bitwarden.com"),
                awaitItem(),
            )
        }
    }

    @Test
    fun `ContinueButtonClick with empty input should do nothing`() = runTest {
        val viewModel = LandingViewModel(SavedStateHandle())
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(LandingAction.ContinueButtonClick)
        }
    }

    @Test
    fun `CreateAccountClick should emit NavigateToCreateAccount`() = runTest {
        val viewModel = LandingViewModel(SavedStateHandle())
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
        val viewModel = LandingViewModel(SavedStateHandle())
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
            val viewModel = LandingViewModel(SavedStateHandle())
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
        val viewModel = LandingViewModel(SavedStateHandle())
        viewModel.stateFlow.test {
            awaitItem()
            viewModel.trySendAction(LandingAction.RegionOptionSelect(inputRegion))
            assertEquals(
                DEFAULT_STATE.copy(selectedRegion = LandingState.RegionOption.BITWARDEN_EU),
                awaitItem(),
            )
        }
    }

    companion object {
        private val DEFAULT_STATE = LandingState(
            emailInput = "",
            isContinueButtonEnabled = false,
            isRememberMeEnabled = false,
            selectedRegion = LandingState.RegionOption.BITWARDEN_US,
        )
    }
}
