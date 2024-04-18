package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccountconfirmation

import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeleteAccountConfirmationScreenTest : BaseComposeTest() {
    private var onNavigateBackCalled = false

    private val mutableEventFlow = bufferedMutableSharedFlow<DeleteAccountConfirmationEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<DeleteAccountConfirmationViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            DeleteAccountConfirmationScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `on NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(DeleteAccountConfirmationEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `loading dialog presence should update with dialog state`() {
        composeTestRule
            .onAllNodesWithText("Loading")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertDoesNotExist()

        mutableStateFlow.update {
            DEFAULT_STATE.copy(
                dialog = DeleteAccountConfirmationState.DeleteAccountConfirmationDialog.Loading(),
            )
        }

        composeTestRule
            .onAllNodesWithText("Loading")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertExists()
    }

    @Test
    fun `error dialog presence should update with dialog state`() {
        val message = "hello world"
        composeTestRule
            .onAllNodesWithText(message)
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertDoesNotExist()

        mutableStateFlow.update {
            DEFAULT_STATE.copy(
                dialog = DeleteAccountConfirmationState.DeleteAccountConfirmationDialog.Error(
                    message = message.asText(),
                ),
            )
        }

        composeTestRule
            .onAllNodesWithText(message)
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertExists()
    }

    @Test
    fun `delete success dialog presence should update with dialog state`() {
        val message = "Your account has been permanently deleted"
        composeTestRule
            .onAllNodesWithText(message)
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertDoesNotExist()

        mutableStateFlow.update {
            DEFAULT_STATE.copy(
                dialog =
                DeleteAccountConfirmationState.DeleteAccountConfirmationDialog.DeleteSuccess(),
            )
        }

        composeTestRule
            .onAllNodesWithText(message)
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertExists()
    }

    @Test
    fun `delete success dialog dismiss should emit DeleteAccountAction`() {
        mutableStateFlow.update {
            DEFAULT_STATE.copy(
                dialog =
                DeleteAccountConfirmationState.DeleteAccountConfirmationDialog.DeleteSuccess(),
            )
        }

        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(DeleteAccountConfirmationAction.DeleteAccountAcknowledge)
        }
    }
}

private val DEFAULT_STATE: DeleteAccountConfirmationState =
    DeleteAccountConfirmationState(dialog = null)
