package com.x8bit.bitwarden.ui.auth.feature.expiredregistrationlink

import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExpiredRegistrationLinkViewModelTest : BaseViewModelTest() {

    @Test
    fun `CloseClicked sends NavigateBack event`() = runTest {
        val viewModel = ExpiredRegistrationLinkViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ExpiredRegistrationLinkAction.CloseClicked)
            assertEquals(ExpiredRegistrationLinkEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `RestartRegistrationClicked sends NavigateToStartRegistration event`() = runTest {
        val viewModel = ExpiredRegistrationLinkViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ExpiredRegistrationLinkAction.RestartRegistrationClicked)
            assertEquals(ExpiredRegistrationLinkEvent.NavigateToStartRegistration, awaitItem())
        }
    }

    @Test
    fun `GoToLoginClicked sends NavigateToLogin event`() = runTest {
        val viewModel = ExpiredRegistrationLinkViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ExpiredRegistrationLinkAction.GoToLoginClicked)
            assertEquals(ExpiredRegistrationLinkEvent.NavigateToLogin, awaitItem())
        }
    }
}
