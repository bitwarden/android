package com.bitwarden.testharness.ui.platform.feature.credentialmanager

import app.cash.turbine.test
import com.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CredentialManagerListViewModelTest : BaseViewModelTest() {

    @Test
    fun `initial state is Unit`() = runTest {
        val viewModel = CredentialManagerListViewModel()

        viewModel.stateFlow.test {
            assertEquals(Unit, awaitItem())
        }
    }

    @Test
    fun `GetPasswordClick action emits NavigateToGetPassword event`() = runTest {
        val viewModel = CredentialManagerListViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(CredentialManagerListAction.GetPasswordClick)

            assertEquals(
                CredentialManagerListEvent.NavigateToGetPassword,
                awaitItem(),
            )
        }
    }

    @Test
    fun `CreatePasswordClick action emits NavigateToCreatePassword event`() = runTest {
        val viewModel = CredentialManagerListViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(CredentialManagerListAction.CreatePasswordClick)

            assertEquals(
                CredentialManagerListEvent.NavigateToCreatePassword,
                awaitItem(),
            )
        }
    }

    @Test
    fun `GetPasskeyClick action emits NavigateToGetPasskey event`() = runTest {
        val viewModel = CredentialManagerListViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(CredentialManagerListAction.GetPasskeyClick)

            assertEquals(
                CredentialManagerListEvent.NavigateToGetPasskey,
                awaitItem(),
            )
        }
    }

    @Test
    fun `CreatePasskeyClick action emits NavigateToCreatePasskey event`() = runTest {
        val viewModel = CredentialManagerListViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(CredentialManagerListAction.CreatePasskeyClick)

            assertEquals(
                CredentialManagerListEvent.NavigateToCreatePasskey,
                awaitItem(),
            )
        }
    }

    @Test
    fun `GetPasswordOrPasskeyClick action emits NavigateToGetPasswordOrPasskey event`() = runTest {
        val viewModel = CredentialManagerListViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(CredentialManagerListAction.GetPasswordOrPasskeyClick)

            assertEquals(
                CredentialManagerListEvent.NavigateToGetPasswordOrPasskey,
                awaitItem(),
            )
        }
    }

    @Test
    fun `BackClick action emits NavigateBack event`() = runTest {
        val viewModel = CredentialManagerListViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(CredentialManagerListAction.BackClick)

            assertEquals(
                CredentialManagerListEvent.NavigateBack,
                awaitItem(),
            )
        }
    }
}
