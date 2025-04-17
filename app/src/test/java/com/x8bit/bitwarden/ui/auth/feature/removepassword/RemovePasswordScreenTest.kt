package com.x8bit.bitwarden.ui.auth.feature.removepassword

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.util.assertNoDialogExists
import com.x8bit.bitwarden.ui.util.assertNoPopupExists
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Before
import org.junit.Test

class RemovePasswordScreenTest : BaseComposeTest() {
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    val viewModel = mockk<RemovePasswordViewModel>(relaxed = true) {
        every { eventFlow } returns bufferedMutableSharedFlow()
        every { stateFlow } returns mutableStateFlow
        every { trySendAction(action = any()) } just runs
    }

    @Before
    fun setup() {
        setContent {
            RemovePasswordScreen(
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `dialog should update according to state`() {
        val errorTitle = "message title"
        val errorMessage = "Error message"
        composeTestRule.assertNoDialogExists()
        composeTestRule.onNodeWithText(text = errorTitle).assertDoesNotExist()
        composeTestRule.onNodeWithText(text = errorMessage).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState = RemovePasswordState.DialogState.Error(
                    title = errorTitle.asText(),
                    message = errorMessage.asText(),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(text = errorTitle)
            .assert(hasAnyAncestor(isDialog()))
            .isDisplayed()
        composeTestRule
            .onNodeWithText(text = errorMessage)
            .assert(hasAnyAncestor(isDialog()))
            .isDisplayed()

        val loadingMessage = "Loading message"
        mutableStateFlow.update {
            it.copy(
                dialogState = RemovePasswordState.DialogState.Loading(
                    title = loadingMessage.asText(),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(text = loadingMessage)
            .assert(hasAnyAncestor(isPopup()))
            .isDisplayed()

        mutableStateFlow.update { it.copy(dialogState = null) }

        composeTestRule.assertNoPopupExists()
    }

    @Test
    fun `description should update according to state`() {
        val description = "description"
        composeTestRule.onNodeWithText(text = description).assertDoesNotExist()

        mutableStateFlow.update { it.copy(description = description.asText()) }

        composeTestRule.onNodeWithText(text = description).isDisplayed()
    }

    @Test
    fun `continue button should update according to state`() {
        composeTestRule.onNodeWithText(text = "Continue").performScrollTo().assertIsNotEnabled()
        mutableStateFlow.update { it.copy(input = "a") }
        composeTestRule.onNodeWithText(text = "Continue").performScrollTo().assertIsEnabled()
    }

    @Test
    fun `continue button click should emit ContinueClick`() {
        mutableStateFlow.update { it.copy(input = "a") }
        composeTestRule
            .onNodeWithText(text = "Continue")
            .performScrollTo()
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(RemovePasswordAction.ContinueClick)
        }
    }

    @Test
    fun `leave organization button click should emit LeaveOrganizationClick`() {
        mutableStateFlow.update { it.copy(input = "a") }
        composeTestRule
            .onNodeWithText(text = "Leave organization")
            .performScrollTo()
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(RemovePasswordAction.LeaveOrganizationClick)
        }
    }

    @Test
    fun `leave organization confirm press should emit LeaveOrganizationConfirm`() {
        mutableStateFlow.update {
            it.copy(
                dialogState =
                    RemovePasswordState.DialogState.LeaveConfirmationPrompt(
                        R.string.leave_organization.asText(),
                    ),
            )
        }

        composeTestRule
            .onAllNodesWithText("Confirm")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(RemovePasswordAction.ConfirmLeaveOrganizationClick)
        }
    }

    @Test
    fun `leave organization cancel press should emil DialogDismiss`() {
        mutableStateFlow.update {
            it.copy(
                dialogState = RemovePasswordState.DialogState.LeaveConfirmationPrompt(
                    R.string.leave_organization_name.asText(
                        "orgName",
                    ),
                ),
            )
        }

        composeTestRule.onAllNodesWithText("Leave organization")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(RemovePasswordAction.DialogDismiss)
        }
    }
}

private val DEFAULT_STATE = RemovePasswordState(
    input = "",
    dialogState = null,
    description = "My org".asText(),
    labelOrg = "Organization name".asText(),
    orgName = "Org X".asText(),
    labelDomain = "Confirm Key Connector domain".asText(),
    domainName = "bitwarden.com".asText(),
    organizationId = "org-id",
)
