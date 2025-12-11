package com.x8bit.bitwarden.ui.vault.feature.leaveorganization

import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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

class LeaveOrganizationScreenTest : BitwardenComposeTest() {

    private var onNavigateBackCalled = false
    private var onNavigateToVaultCalled = false

    private val mutableEventFlow = bufferedMutableSharedFlow<LeaveOrganizationEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<LeaveOrganizationViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        setContent {
            LeaveOrganizationScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToVault = { onNavigateToVaultCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `NavigateBack event should call onNavigateBack`() {
        mutableEventFlow.tryEmit(LeaveOrganizationEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `NavigateToVault event should call onNavigateToVault`() {
        mutableEventFlow.tryEmit(LeaveOrganizationEvent.NavigateToVault)
        assertTrue(onNavigateToVaultCalled)
    }

    @Test
    fun `back button click should emit NavigateBack event`() {
        composeTestRule
            .onNodeWithContentDescription("Back")
            .performClick()
        verify { viewModel.trySendAction(LeaveOrganizationAction.BackClick) }
    }

    @Test
    fun `leave organization button click should emit LeaveOrganizationClick action`() {
        composeTestRule
            .onAllNodesWithText("Leave $ORGANIZATION_NAME")
            .filterToOne(hasClickAction())
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(LeaveOrganizationAction.LeaveOrganizationClick) }
    }

    @Test
    fun `help link button click should emit HelpLinkClick action`() {
        composeTestRule
            .onNodeWithText("How to manage My vault")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(LeaveOrganizationAction.HelpLinkClick) }
    }

    @Test
    fun `organization name should be displayed in title`() {
        composeTestRule
            .onNodeWithText("Are you sure you want to leave $ORGANIZATION_NAME?")
            .assertExists()
    }

    @Test
    fun `organization name should be displayed in button`() {
        composeTestRule
            .onAllNodesWithText("Leave $ORGANIZATION_NAME")
            .filterToOne(hasClickAction())
            .assertExists()
    }

    @Test
    fun `warning text should be displayed`() {
        composeTestRule
            .onNodeWithText(
                text = "By declining, your personal items will stay in your account, but you’ll " +
                    "lose access to shared items and organization features.\n\nContact your " +
                    "admin to regain access.",
            )
            .assertExists()
    }

    @Test
    fun `loading dialog should not be displayed by default`() {
        composeTestRule
            .onAllNodesWithText("Loading")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertDoesNotExist()
    }

    @Test
    fun `loading dialog should be displayed when dialogState is Loading`() {
        mutableStateFlow.update {
            it.copy(dialogState = LeaveOrganizationState.DialogState.Loading)
        }

        composeTestRule
            .onAllNodesWithText("Loading")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertExists()
    }

    @Test
    fun `error dialog should not be displayed by default`() {
        composeTestRule
            .onAllNodesWithText("An error has occurred")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertDoesNotExist()
    }

    @Test
    fun `error dialog should be displayed when dialogState is Error`() {
        val errorMessage = "Something went wrong"
        mutableStateFlow.update {
            it.copy(
                dialogState = LeaveOrganizationState.DialogState.Error(
                    message = errorMessage.asText(),
                    error = Throwable("Test error"),
                ),
            )
        }

        composeTestRule
            .onAllNodesWithText("An error has occurred")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertExists()

        composeTestRule
            .onAllNodesWithText(errorMessage)
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertExists()
    }

    @Test
    fun `error dialog dismiss should emit DismissDialog action`() {
        mutableStateFlow.update {
            it.copy(
                dialogState = LeaveOrganizationState.DialogState.Error(
                    message = "Error message".asText(),
                ),
            )
        }

        composeTestRule
            .onAllNodesWithText("Okay")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(LeaveOrganizationAction.DismissDialog)
        }
    }
}

private const val ORGANIZATION_ID = "organization-id-1"
private const val ORGANIZATION_NAME = "Test Organization"

private val DEFAULT_STATE = LeaveOrganizationState(
    organizationId = ORGANIZATION_ID,
    organizationName = ORGANIZATION_NAME,
    dialogState = null,
)
