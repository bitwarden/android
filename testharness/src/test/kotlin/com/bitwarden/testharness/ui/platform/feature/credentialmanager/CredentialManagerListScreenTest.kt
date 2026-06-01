package com.bitwarden.testharness.ui.platform.feature.credentialmanager

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for the [CredentialManagerListScreen] composable in the testharness module.
 *
 * Verifies that the Credential Manager list screen displays navigation options correctly,
 * sends correct actions to the ViewModel when buttons are clicked, and invokes callbacks
 * when the ViewModel emits events, following UDF patterns.
 */
class CredentialManagerListScreenTest : BaseComposeTest() {
    private var haveCalledOnNavigateBack = false
    private var haveCalledOnNavigateToGetPassword = false
    private var haveCalledOnNavigateToCreatePassword = false
    private var haveCalledOnNavigateToGetPasskey = false
    private var haveCalledOnNavigateToCreatePasskey = false
    private var haveCalledOnNavigateToGetPasswordOrPasskey = false

    private val mutableEventFlow = MutableSharedFlow<CredentialManagerListEvent>(replay = 1)
    private val mutableStateFlow = MutableStateFlow(Unit)
    private val viewModel = mockk<CredentialManagerListViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setup() {
        setTestContent {
            CredentialManagerListScreen(
                onNavigateBack = { haveCalledOnNavigateBack = true },
                onNavigateToGetPassword = { haveCalledOnNavigateToGetPassword = true },
                onNavigateToCreatePassword = { haveCalledOnNavigateToCreatePassword = true },
                onNavigateToGetPasskey = { haveCalledOnNavigateToGetPasskey = true },
                onNavigateToCreatePasskey = { haveCalledOnNavigateToCreatePasskey = true },
                onNavigateToGetPasswordOrPasskey = {
                    haveCalledOnNavigateToGetPasswordOrPasskey = true
                },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `get password button is displayed`() {
        composeTestRule
            .onNodeWithText("Get Password")
            .assertIsDisplayed()
    }

    @Test
    fun `create password button is displayed`() {
        composeTestRule
            .onNodeWithText("Create Password")
            .assertIsDisplayed()
    }

    @Test
    fun `get passkey button is displayed`() {
        composeTestRule
            .onNodeWithText("Get Passkey")
            .assertIsDisplayed()
    }

    @Test
    fun `create passkey button is displayed`() {
        composeTestRule
            .onNodeWithText("Create Passkey")
            .assertIsDisplayed()
    }

    @Test
    fun `get password or passkey button is displayed`() {
        composeTestRule
            .onNodeWithText("Get Password or Passkey")
            .assertIsDisplayed()
    }

    @Test
    fun `back button click should send BackClick action`() {
        composeTestRule
            .onNodeWithContentDescription("Back")
            .performClick()

        verify {
            viewModel.trySendAction(CredentialManagerListAction.BackClick)
        }
    }

    @Test
    fun `NavigateBack event should call onNavigateBack`() {
        mutableEventFlow.tryEmit(CredentialManagerListEvent.NavigateBack)

        assertTrue(haveCalledOnNavigateBack)
    }

    @Test
    fun `get password button click should send GetPasswordClick action`() {
        composeTestRule
            .onNodeWithText("Get Password")
            .performClick()

        verify {
            viewModel.trySendAction(CredentialManagerListAction.GetPasswordClick)
        }
    }

    @Test
    fun `create password button click should send CreatePasswordClick action`() {
        composeTestRule
            .onNodeWithText("Create Password")
            .performClick()

        verify {
            viewModel.trySendAction(CredentialManagerListAction.CreatePasswordClick)
        }
    }

    @Test
    fun `get passkey button click should send GetPasskeyClick action`() {
        composeTestRule
            .onNodeWithText("Get Passkey")
            .performClick()

        verify {
            viewModel.trySendAction(CredentialManagerListAction.GetPasskeyClick)
        }
    }

    @Test
    fun `create passkey button click should send CreatePasskeyClick action`() {
        composeTestRule
            .onNodeWithText("Create Passkey")
            .performClick()

        verify {
            viewModel.trySendAction(CredentialManagerListAction.CreatePasskeyClick)
        }
    }

    @Test
    fun `get password or passkey button click should send GetPasswordOrPasskeyClick action`() {
        composeTestRule
            .onNodeWithText("Get Password or Passkey")
            .performClick()

        verify {
            viewModel.trySendAction(CredentialManagerListAction.GetPasswordOrPasskeyClick)
        }
    }

    @Test
    fun `NavigateToGetPassword event should call onNavigateToGetPassword`() {
        mutableEventFlow.tryEmit(CredentialManagerListEvent.NavigateToGetPassword)

        assertTrue(haveCalledOnNavigateToGetPassword)
    }

    @Test
    fun `NavigateToCreatePassword event should call onNavigateToCreatePassword`() {
        mutableEventFlow.tryEmit(CredentialManagerListEvent.NavigateToCreatePassword)

        assertTrue(haveCalledOnNavigateToCreatePassword)
    }

    @Test
    fun `NavigateToGetPasskey event should call onNavigateToGetPasskey`() {
        mutableEventFlow.tryEmit(CredentialManagerListEvent.NavigateToGetPasskey)

        assertTrue(haveCalledOnNavigateToGetPasskey)
    }

    @Test
    fun `NavigateToCreatePasskey event should call onNavigateToCreatePasskey`() {
        mutableEventFlow.tryEmit(CredentialManagerListEvent.NavigateToCreatePasskey)

        assertTrue(haveCalledOnNavigateToCreatePasskey)
    }

    @Test
    fun `NavigateToGetPasswordOrPasskey event should call onNavigateToGetPasswordOrPasskey`() {
        mutableEventFlow.tryEmit(CredentialManagerListEvent.NavigateToGetPasswordOrPasskey)

        assertTrue(haveCalledOnNavigateToGetPasswordOrPasskey)
    }
}
