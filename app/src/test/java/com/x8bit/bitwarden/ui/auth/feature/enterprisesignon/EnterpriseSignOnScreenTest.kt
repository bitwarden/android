package com.x8bit.bitwarden.ui.auth.feature.enterprisesignon

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Before
import org.junit.Test

class EnterpriseSignOnScreenTest : BaseComposeTest() {
    private var onNavigateBackCalled = false
    private val mutableEventFlow = MutableSharedFlow<EnterpriseSignOnEvent>(
        extraBufferCapacity = Int.MAX_VALUE,
    )
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<EnterpriseSignOnViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setup() {
        composeTestRule.setContent {
            EnterpriseSignOnScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `app bar log in click should send LogInClick action`() {
        composeTestRule.onNodeWithText("Log In").performClick()
        verify { viewModel.trySendAction(EnterpriseSignOnAction.LogInClick) }
    }

    @Test
    fun `close button click should send CloseButtonClick action`() {
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        verify {
            viewModel.trySendAction(EnterpriseSignOnAction.CloseButtonClick)
        }
    }

    @Test
    fun `organization identifier input change should send OrgIdentifierInputChange action`() {
        val input = "input"
        composeTestRule.onNodeWithText("Organization identifier").performTextInput(input)
        verify {
            viewModel.trySendAction(EnterpriseSignOnAction.OrgIdentifierInputChange(input))
        }
    }

    @Test
    fun `organization identifier should change according to state`() {
        composeTestRule
            .onNodeWithText("Organization identifier")
            .assertTextEquals("Organization identifier", "")

        mutableStateFlow.update { it.copy(orgIdentifierInput = "test") }

        composeTestRule
            .onNodeWithText("Organization identifier")
            .assertTextEquals("Organization identifier", "test")
    }

    @Test
    fun `NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(EnterpriseSignOnEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `error dialog should be shown or hidden according to the state`() {
        composeTestRule.onNode(isDialog()).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState = EnterpriseSignOnState.DialogState.Error(
                    message = "Error dialog message".asText(),
                ),
            )
        }

        composeTestRule.onNode(isDialog()).assertIsDisplayed()

        composeTestRule
            .onNodeWithText("An error has occurred.")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Error dialog message")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Ok")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `loading dialog should be displayed according to state`() {
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText("Loading").assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState = EnterpriseSignOnState.DialogState.Loading(
                    message = "Loading".asText(),
                ),
            )
        }

        composeTestRule
            .onNodeWithText("Loading")
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))
    }

    @Test
    fun `error dialog OK click should send DialogDismiss action`() {
        mutableStateFlow.update {
            DEFAULT_STATE.copy(
                dialogState = EnterpriseSignOnState.DialogState.Error(
                    message = "message".asText(),
                ),
            )
        }

        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(EnterpriseSignOnAction.DialogDismiss) }
    }

    companion object {
        private val DEFAULT_STATE = EnterpriseSignOnState(
            dialogState = null,
            orgIdentifierInput = "",
        )
    }
}
