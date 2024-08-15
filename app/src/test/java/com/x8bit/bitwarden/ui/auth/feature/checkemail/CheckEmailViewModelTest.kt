package com.x8bit.bitwarden.ui.auth.feature.checkemail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CheckEmailViewModelTest : BaseViewModelTest() {
    @Test
    fun `initial state should be correct`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
        }
    }

    @Test
    fun `initial state should pull from handle when present`() = runTest {
        val expectedState = DEFAULT_STATE.copy(
            email = "another@email.com",
        )
        val viewModel = createViewModel(expectedState)
        viewModel.stateFlow.test {
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `CloseTap should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(CheckEmailAction.CloseClick)
            assertEquals(
                CheckEmailEvent.NavigateBack,
                awaitItem(),
            )
        }
    }

    @Test
    fun `LoginTap should emit NavigateBackToLanding`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(CheckEmailAction.LoginClick)
            assertEquals(
                CheckEmailEvent.NavigateBackToLanding,
                awaitItem(),
            )
        }
    }

    @Test
    fun `OpenEmailTap should emit NavigateToEmailApp`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(CheckEmailAction.OpenEmailClick)
            assertEquals(
                CheckEmailEvent.NavigateToEmailApp,
                awaitItem(),
            )
        }
    }

    private fun createViewModel(state: CheckEmailState? = null): CheckEmailViewModel =
        CheckEmailViewModel(
            savedStateHandle = SavedStateHandle().also {
                it["email"] = EMAIL
                it["state"] = state
            },
        )

    companion object {
        private const val EMAIL = "test@gmail.com"
        private val DEFAULT_STATE = CheckEmailState(
            email = EMAIL,
        )
    }
}
