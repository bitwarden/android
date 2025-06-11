package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccount

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeleteAccountScreenTest : BitwardenComposeTest() {

    private var onNavigateBackCalled = false
    private var onNavigateToDeleteAccountConfirmationScreenCalled = false

    private val mutableEventFlow = bufferedMutableSharedFlow<DeleteAccountEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<DeleteAccountViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        setContent {
            DeleteAccountScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToDeleteAccountConfirmation = {
                    onNavigateToDeleteAccountConfirmationScreenCalled = true
                },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `on NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(DeleteAccountEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `NavigateToDeleteAccountConfirmationScreen should call onNavigateToDeleteAccountConfirmationScreenCalled`() {
        mutableEventFlow.tryEmit(DeleteAccountEvent.NavigateToDeleteAccountConfirmationScreen)
        assertTrue(onNavigateToDeleteAccountConfirmationScreenCalled)
    }

    @Test
    fun `cancel click should emit CancelClick`() {
        composeTestRule.onNodeWithText("Cancel").performScrollTo().performClick()
        verify { viewModel.trySendAction(DeleteAccountAction.CancelClick) }
    }

    @Test
    fun `loading dialog presence should update with dialog state`() {
        composeTestRule
            .onAllNodesWithText("Loading")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(dialog = DeleteAccountState.DeleteAccountDialog.Loading)
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
            it.copy(dialog = DeleteAccountState.DeleteAccountDialog.Error(message.asText()))
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
            it.copy(dialog = DeleteAccountState.DeleteAccountDialog.DeleteSuccess)
        }

        composeTestRule
            .onAllNodesWithText(message)
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertExists()
    }

    @Test
    fun `delete success dialog dismiss should emit DeleteAccountAction`() {
        mutableStateFlow.update {
            it.copy(dialog = DeleteAccountState.DeleteAccountDialog.DeleteSuccess)
        }

        composeTestRule
            .onAllNodesWithText(text = "Okay")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(DeleteAccountAction.AccountDeletionConfirm)
        }
    }

    @Test
    fun `delete account dialog should dismiss on cancel click`() {
        composeTestRule
            .onAllNodesWithText("Master password confirmation")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertDoesNotExist()

        composeTestRule
            .onAllNodesWithText("Delete account")
            .filterToOne(hasClickAction())
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Master password confirmation")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertExists()

        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule
            .onAllNodesWithText("Master password confirmation")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertDoesNotExist()
    }

    @Test
    fun `delete account dialog should emit DeleteAccountClick on submit click`() {
        val password = "hello world"
        composeTestRule
            .onAllNodesWithText("Master password confirmation")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertDoesNotExist()

        composeTestRule
            .onAllNodesWithText("Delete account")
            .filterToOne(hasClickAction())
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Master password confirmation")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertExists()

        composeTestRule
            .onAllNodesWithText("Submit")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsNotEnabled()

        composeTestRule
            .onAllNodesWithText("Master password")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performTextInput(password)

        composeTestRule
            .onAllNodesWithText("Submit")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsEnabled()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Master password confirmation")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertDoesNotExist()

        verify {
            viewModel.trySendAction(
                DeleteAccountAction.DeleteAccountConfirmDialogClick(
                    password,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `if isUserManagedByOrganization should display cannot delete message and hide delete button`() {
        composeTestRule
            .onNodeWithText("Cannot delete your account")
            .assertDoesNotExist()

        composeTestRule
            .onNodeWithText(
                text = "This action cannot be completed because your account " +
                    "is owned by an organization. " +
                    "Contact your organization administrator for additional details.",
            )
            .assertDoesNotExist()

        composeTestRule
            .onAllNodesWithText("Delete account")
            .filterToOne(hasClickAction())
            .assertExists()

        mutableStateFlow.update {
            it.copy(isUserManagedByOrganization = true)
        }

        composeTestRule
            .onNodeWithText("Cannot delete your account")
            .assertExists()

        composeTestRule
            .onNodeWithText(
                text = "This action cannot be completed because your account " +
                    "is owned by an organization. " +
                    "Contact your organization administrator for additional details.",
            )
            .assertExists()

        composeTestRule
            .onAllNodesWithText("Delete account")
            .filterToOne(hasClickAction())
            .assertDoesNotExist()
    }
}

private val DEFAULT_STATE: DeleteAccountState = DeleteAccountState(
    dialog = null,
    isUnlockWithPasswordEnabled = true,
    isUserManagedByOrganization = false,
)
