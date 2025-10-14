package com.bitwarden.authenticator.ui.authenticator.feature.edititem

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemAlgorithm
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemType
import com.bitwarden.authenticator.ui.authenticator.feature.edititem.model.EditItemData
import com.bitwarden.authenticator.ui.platform.base.AuthenticatorComposeTest
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.assertNoDialogExists
import com.bitwarden.ui.util.isProgressBar
import com.bitwarden.ui.util.onNodeWithContentDescriptionAfterScroll
import com.bitwarden.ui.util.onNodeWithTextAfterScroll
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EditItemScreenTest : AuthenticatorComposeTest() {

    private var onNavigateBackCalled = false

    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val mutableEventFlow = bufferedMutableSharedFlow<EditItemEvent>()
    private val viewModel: EditItemViewModel = mockk {
        every { stateFlow } returns mutableStateFlow
        every { eventFlow } returns mutableEventFlow
        every { trySendAction(action = any()) } just runs
    }

    @Before
    fun setup() {
        setContent {
            EditItemScreen(
                viewModel = viewModel,
                onNavigateBack = { onNavigateBackCalled = true },
            )
        }
    }

    @Test
    fun `on NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(EditItemEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `dialogs should update according to state`() {
        composeTestRule.assertNoDialogExists()

        val loadingMessage = "loading!"
        mutableStateFlow.update {
            it.copy(dialog = EditItemState.DialogState.Loading(message = loadingMessage.asText()))
        }
        composeTestRule
            .onNodeWithText(text = loadingMessage)
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        val genericTitle = "Generic Title!"
        val genericMessage = "Generic message"
        mutableStateFlow.update {
            it.copy(
                dialog = EditItemState.DialogState.Generic(
                    title = genericTitle.asText(),
                    message = genericMessage.asText(),
                ),
            )
        }
        composeTestRule
            .onNodeWithText(text = genericTitle)
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = genericMessage)
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        mutableStateFlow.update { it.copy(dialog = null) }
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `generic dialog should dismiss upon clicking okay`() {
        mutableStateFlow.update {
            it.copy(
                dialog = EditItemState.DialogState.Generic(
                    title = "Generic Title!".asText(),
                    message = "Generic message".asText(),
                ),
            )
        }
        composeTestRule
            .onNodeWithText(text = "Okay")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(EditItemAction.DismissDialog)
        }
    }

    @Test
    fun `on close click should emit CancelClick`() {
        composeTestRule
            .onNodeWithContentDescription(label = "Close")
            .assertIsDisplayed()
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(EditItemAction.CancelClick)
        }
    }

    @Test
    fun `on save click should emit SaveClick`() {
        composeTestRule
            .onNodeWithText(text = "Save")
            .assertIsDisplayed()
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(EditItemAction.SaveClick)
        }
    }

    @Test
    fun `view state should update according to state`() {
        mutableStateFlow.update { it.copy(viewState = EditItemState.ViewState.Loading) }

        composeTestRule.onNode(isProgressBar).assertIsDisplayed()

        val errorMessage = "Error!"
        mutableStateFlow.update {
            it.copy(viewState = EditItemState.ViewState.Error(message = errorMessage.asText()))
        }
        composeTestRule.onNodeWithText(text = errorMessage).assertIsDisplayed()

        mutableStateFlow.update { it.copy(viewState = DEFAULT_CONTENT) }

        composeTestRule.onNodeWithTextAfterScroll(text = "INFORMATION").assertIsDisplayed()
        composeTestRule.onNodeWithTextAfterScroll(text = "Name").assertIsDisplayed()
        composeTestRule.onNodeWithTextAfterScroll(text = "Key").assertIsDisplayed()
        composeTestRule.onNodeWithTextAfterScroll(text = "Username").assertIsDisplayed()
        composeTestRule.onNodeWithTextAfterScroll(text = "Favorite").assertIsDisplayed()
        composeTestRule.onNodeWithTextAfterScroll(text = "Additional options").assertIsDisplayed()
    }

    @Test
    fun `editing name field should send IssuerNameTextChange`() {
        val textInput = "name"
        mutableStateFlow.update { it.copy(viewState = DEFAULT_CONTENT) }
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Name")
            .performTextInput(text = textInput)
        verify(exactly = 1) {
            viewModel.trySendAction(EditItemAction.IssuerNameTextChange(textInput))
        }
    }

    @Test
    fun `editing username field should send UsernameTextChange`() {
        val textInput = "name"
        mutableStateFlow.update { it.copy(viewState = DEFAULT_CONTENT) }
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Username")
            .performTextInput(text = textInput)
        verify(exactly = 1) {
            viewModel.trySendAction(EditItemAction.UsernameTextChange(textInput))
        }
    }

    @Test
    fun `editing key field should send TotpCodeTextChange`() {
        val textInput = "key"
        mutableStateFlow.update { it.copy(viewState = DEFAULT_CONTENT) }
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Key")
            .performTextInput(text = textInput)
        verify(exactly = 1) {
            viewModel.trySendAction(EditItemAction.TotpCodeTextChange(textInput))
        }
    }

    @Test
    fun `favorite switch toggle should send FavoriteToggleClick`() {
        mutableStateFlow.update { it.copy(viewState = DEFAULT_CONTENT) }
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Favorite")
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(EditItemAction.FavoriteToggleClick(true))
        }
    }

    @Test
    fun `advanced click should send ExpandAdvancedOptionsClick`() {
        mutableStateFlow.update { it.copy(viewState = DEFAULT_CONTENT) }
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Additional options")
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(EditItemAction.ExpandAdvancedOptionsClick)
        }
    }

    @Test
    fun `OTP type click should display dialog and selection should send TypeOptionClick`() {
        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT.copy(isAdvancedOptionsExpanded = true))
        }
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "TOTP. OTP type")
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "OTP type")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = "STEAM")
            .performClick()
        composeTestRule.assertNoDialogExists()

        verify(exactly = 1) {
            viewModel.trySendAction(EditItemAction.TypeOptionClick(AuthenticatorItemType.STEAM))
        }
    }

    @Test
    fun `OTP type click should display dialog and cancel should dismiss the dialog`() {
        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT.copy(isAdvancedOptionsExpanded = true))
        }
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "TOTP. OTP type")
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "OTP type")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = "Cancel")
            .performClick()
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `algorithm click should display dialog and selection should send TypeOptionClick`() {
        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT.copy(isAdvancedOptionsExpanded = true))
        }
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "SHA1. Algorithm")
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Algorithm")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = "SHA256")
            .performClick()
        composeTestRule.assertNoDialogExists()

        verify(exactly = 1) {
            viewModel.trySendAction(
                EditItemAction.AlgorithmOptionClick(AuthenticatorItemAlgorithm.SHA256),
            )
        }
    }

    @Test
    fun `algorithm click should display dialog and cancel should dismiss the dialog`() {
        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT.copy(isAdvancedOptionsExpanded = true))
        }
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "SHA1. Algorithm")
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Algorithm")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = "Cancel")
            .performClick()
        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `refresh period click should display dialog and selection should send RefreshPeriodOptionClick`() {
        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT.copy(isAdvancedOptionsExpanded = true))
        }
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "30 seconds. Refresh period")
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Refresh period")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = "60 seconds")
            .performClick()
        composeTestRule.assertNoDialogExists()

        verify(exactly = 1) {
            viewModel.trySendAction(
                EditItemAction.RefreshPeriodOptionClick(AuthenticatorRefreshPeriodOption.SIXTY),
            )
        }
    }

    @Test
    fun `refresh period click should display dialog and cancel should dismiss the dialog`() {
        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT.copy(isAdvancedOptionsExpanded = true))
        }
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "30 seconds. Refresh period")
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Refresh period")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = "Cancel")
            .performClick()
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `number of digits plus click should send NumberOfDigitsOptionClick`() {
        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT.copy(isAdvancedOptionsExpanded = true))
        }
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Number of digits")
            .onChildren()
            .filterToOne(hasContentDescription("+"))
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(EditItemAction.NumberOfDigitsOptionClick(7))
        }
    }

    @Test
    fun `number of digits minus click should send NumberOfDigitsOptionClick`() {
        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT.copy(isAdvancedOptionsExpanded = true))
        }
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Number of digits")
            .onChildren()
            .filterToOne(hasContentDescription("\u2212"))
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(EditItemAction.NumberOfDigitsOptionClick(5))
        }
    }
}

private val DEFAULT_STATE: EditItemState =
    EditItemState(
        itemId = "item_id",
        viewState = EditItemState.ViewState.Loading,
        dialog = null,
    )

private val DEFAULT_ITEM_DATA: EditItemData =
    EditItemData(
        refreshPeriod = AuthenticatorRefreshPeriodOption.THIRTY,
        totpCode = "",
        type = AuthenticatorItemType.TOTP,
        username = null,
        issuer = "",
        algorithm = AuthenticatorItemAlgorithm.SHA1,
        digits = 6,
        favorite = false,
    )

private val DEFAULT_CONTENT: EditItemState.ViewState.Content =
    EditItemState.ViewState.Content(
        isAdvancedOptionsExpanded = false,
        minDigitsAllowed = 5,
        maxDigitsAllowed = 10,
        itemData = DEFAULT_ITEM_DATA,
    )
