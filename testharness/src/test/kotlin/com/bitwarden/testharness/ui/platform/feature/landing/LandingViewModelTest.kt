package com.bitwarden.testharness.ui.platform.feature.landing

import app.cash.turbine.test
import com.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LandingViewModelTest : BaseViewModelTest() {

    @Test
    fun `initial state should be Unit`() = runTest {
        val viewModel = LandingViewModel()

        assertEquals(Unit, viewModel.stateFlow.value)
    }

    @Test
    fun `state flow emits correct Unit state`() = runTest {
        val viewModel = LandingViewModel()

        viewModel.stateFlow.test {
            val state = awaitItem()
            assertEquals(Unit, state)
        }
    }

    @Test
    fun `OnAutofillClick action emits NavigateToAutofill event`() = runTest {
        val viewModel = LandingViewModel()

        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(LandingAction.OnAutofillClick)
            assertEquals(LandingEvent.NavigateToAutofill, awaitItem())
        }
    }

    @Test
    fun `OnCredentialManagerClick action emits NavigateToCredentialManager event`() = runTest {
        val viewModel = LandingViewModel()

        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(LandingAction.OnCredentialManagerClick)
            assertEquals(LandingEvent.NavigateToCredentialManager, awaitItem())
        }
    }

    @Test
    fun `event flow emits events correctly when multiple actions sent`() = runTest {
        val viewModel = LandingViewModel()

        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(LandingAction.OnAutofillClick)
            assertEquals(LandingEvent.NavigateToAutofill, awaitItem())

            viewModel.actionChannel.trySend(LandingAction.OnCredentialManagerClick)
            assertEquals(LandingEvent.NavigateToCredentialManager, awaitItem())
        }
    }

    @Test
    fun `event flow remains empty when no actions are sent`() = runTest {
        val viewModel = LandingViewModel()

        viewModel.eventFlow.test {
            expectNoEvents()
        }
    }
}
