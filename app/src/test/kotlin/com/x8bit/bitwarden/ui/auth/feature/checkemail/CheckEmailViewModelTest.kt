package com.x8bit.bitwarden.ui.auth.feature.checkemail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CheckEmailViewModelTest : BaseViewModelTest() {

    @BeforeEach
    fun setup() {
        mockkStatic(SavedStateHandle::toCheckEmailArgs)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(SavedStateHandle::toCheckEmailArgs)
    }

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
            viewModel.trySendAction(CheckEmailAction.BackClick)
            assertEquals(
                CheckEmailEvent.NavigateBack,
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

    @Test
    fun `ChangeEmailTap should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(CheckEmailAction.ChangeEmailClick)
            assertEquals(
                CheckEmailEvent.NavigateBack,
                awaitItem(),
            )
        }
    }

    private fun createViewModel(state: CheckEmailState? = null): CheckEmailViewModel =
        CheckEmailViewModel(
            savedStateHandle = SavedStateHandle().apply {
                set(key = "state", value = state)
                every { toCheckEmailArgs() } returns CheckEmailArgs(emailAddress = EMAIL)
            },
        )
}

private const val EMAIL = "test@gmail.com"
private val DEFAULT_STATE = CheckEmailState(
    email = EMAIL,
)
