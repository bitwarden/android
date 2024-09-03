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
        viewModel.trySendAction(ExpiredRegistrationLinkAction.CloseClicked)
        viewModel.eventFlow.test {
            assertEquals(ExpiredRegistrationLinkEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `RestartRegistrationClicked sends NavigateToStartRegistration event`() = runTest {
        val viewModel = ExpiredRegistrationLinkViewModel()
        viewModel.trySendAction(ExpiredRegistrationLinkAction.RestartRegistrationClicked)
        viewModel.eventFlow.test {
            assertEquals(ExpiredRegistrationLinkEvent.NavigateToStartRegistration, awaitItem())
        }
    }

    @Test
    fun `GoToLoginClicked sends NavigateToLogin event`() = runTest {
        val viewModel = ExpiredRegistrationLinkViewModel()
        viewModel.trySendAction(ExpiredRegistrationLinkAction.GoToLoginClicked)
        viewModel.eventFlow.test {
            assertEquals(ExpiredRegistrationLinkEvent.NavigateToLogin, awaitItem())
        }
    }
}
