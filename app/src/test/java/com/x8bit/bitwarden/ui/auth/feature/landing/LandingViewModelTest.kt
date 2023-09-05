package com.x8bit.bitwarden.ui.auth.feature.landing

import app.cash.turbine.test
import com.x8bit.bitwarden.ui.auth.feature.landing.LandingAction
import com.x8bit.bitwarden.ui.auth.feature.landing.LandingEvent
import com.x8bit.bitwarden.ui.auth.feature.landing.LandingState
import com.x8bit.bitwarden.ui.auth.feature.landing.LandingViewModel
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LandingViewModelTest : BaseViewModelTest() {

    @Test
    fun `ContinueButtonClick should disable continue button`() = runTest {
        val viewModel = LandingViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(LandingAction.ContinueButtonClick)
            assertEquals(
                viewModel.stateFlow.value,
                DEFAULT_STATE.copy(isContinueButtonEnabled = false),
            )
        }
    }

    @Test
    fun `CreateAccountClick should emit NavigateToCreateAccount`() = runTest {
        val viewModel = LandingViewModel()
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
        val viewModel = LandingViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(LandingAction.RememberMeToggle(true))
            assertEquals(
                viewModel.stateFlow.value,
                DEFAULT_STATE.copy(isRememberMeEnabled = true),
            )
        }
    }

    companion object {
        private val DEFAULT_STATE = LandingState(
            initialEmailAddress = "",
            isContinueButtonEnabled = true,
            isRememberMeEnabled = false,
        )
    }
}
