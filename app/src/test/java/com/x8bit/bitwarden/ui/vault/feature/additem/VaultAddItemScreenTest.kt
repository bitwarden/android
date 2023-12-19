package com.x8bit.bitwarden.ui.vault.feature.additem

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onSiblings
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.util.isProgressBar
import com.x8bit.bitwarden.ui.util.onAllNodesWithTextAfterScroll
import com.x8bit.bitwarden.ui.util.onNodeWithContentDescriptionAfterScroll
import com.x8bit.bitwarden.ui.util.onNodeWithTextAfterScroll
import com.x8bit.bitwarden.ui.vault.feature.additem.model.CustomFieldType
import com.x8bit.bitwarden.ui.vault.model.VaultAddEditType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@Suppress("LargeClass")
class VaultAddItemScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled = false

    private val mutableEventFlow = MutableSharedFlow<VaultAddItemEvent>(Int.MAX_VALUE)
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE_LOGIN)

    private val viewModel = mockk<VaultAddItemViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setup() {
        composeTestRule.setContent {
            VaultAddItemScreen(
                viewModel = viewModel,
                onNavigateBack = { onNavigateBackCalled = true },
            )
        }
    }

    @Test
    fun `on NavigateBack event should invoke onNavigateBack`() {
        mutableEventFlow.tryEmit(VaultAddItemEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `clicking close button should send CloseClick action`() {
        composeTestRule
            .onNodeWithContentDescription(label = "Close")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.Common.CloseClick,
            )
        }
    }

    @Test
    fun `clicking save button should send SaveClick action`() {
        composeTestRule
            .onNodeWithText(text = "Save")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.Common.SaveClick,
            )
        }
    }

    @Test
    fun `clicking dismiss dialog button should send DismissDialog action`() {
        mutableStateFlow.value = DEFAULT_STATE_LOGIN_DIALOG

        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.Common.DismissDialog,
            )
        }
    }

    @Test
    fun `dialog should display when state is updated to do so`() {
        mutableStateFlow.value = DEFAULT_STATE_LOGIN

        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsNotDisplayed()

        mutableStateFlow.value = DEFAULT_STATE_LOGIN_DIALOG

        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `error text and retry should be displayed according to state`() {
        val message = "error_message"
        mutableStateFlow.update {
            it.copy(viewState = VaultAddItemState.ViewState.Loading)
        }
        composeTestRule.onNodeWithText(message).assertIsNotDisplayed()

        mutableStateFlow.update {
            it.copy(
                viewState = VaultAddItemState.ViewState.Content(
                    common = VaultAddItemState.ViewState.Content.Common(),
                    type = VaultAddItemState.ViewState.Content.ItemType.Login(),
                ),
            )
        }
        composeTestRule.onNodeWithText(message).assertIsNotDisplayed()

        mutableStateFlow.update {
            it.copy(viewState = VaultAddItemState.ViewState.Error(message.asText()))
        }
        composeTestRule.onNodeWithText(message).assertIsDisplayed()
    }

    @Test
    fun `progressbar should be displayed according to state`() {
        mutableStateFlow.update {
            it.copy(viewState = VaultAddItemState.ViewState.Loading)
        }
        composeTestRule.onNode(isProgressBar).assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(viewState = VaultAddItemState.ViewState.Error("Fail".asText()))
        }
        composeTestRule.onNode(isProgressBar).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                viewState = VaultAddItemState.ViewState.Content(
                    common = VaultAddItemState.ViewState.Content.Common(),
                    type = VaultAddItemState.ViewState.Content.ItemType.Login(),
                ),
            )
        }
        composeTestRule.onNode(isProgressBar).assertDoesNotExist()
    }

    @Test
    fun `clicking a Type Option should send TypeOptionSelect action`() {
        // Opens the menu
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Type, Login")
            .performClick()

        // Choose the option from the menu
        composeTestRule
            .onAllNodesWithText(text = "Login")
            .onLast()
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.Common.TypeOptionSelect(VaultAddItemState.ItemTypeOption.LOGIN),
            )
        }
    }

    @Test
    fun `the Type Option field should display the text of the selected item type`() {
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Type, Login")
            .assertIsDisplayed()

        mutableStateFlow.update { it.copy(
            viewState = VaultAddItemState.ViewState.Content(
                common = VaultAddItemState.ViewState.Content.Common(),
                type = VaultAddItemState.ViewState.Content.ItemType.Card,
            ),
        ) }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Type, Card")
            .assertIsDisplayed()
    }

    @Test
    fun `in ItemType_Login state changing Username text field should trigger UsernameTextChange`() {
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Username")
            .performTextInput(text = "TestUsername")

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.ItemType.LoginType.UsernameTextChange(username = "TestUsername"),
            )
        }
    }

    @Test
    fun `in ItemType_Login the Username control should display the text provided by the state`() {
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Username")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(username = "NewUsername") }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Username")
            .assertTextContains("NewUsername")
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login state clicking Username generator action should trigger OpenUsernameGeneratorClick`() {
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Generate username")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.ItemType.LoginType.OpenUsernameGeneratorClick,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login state clicking Password checker action should trigger PasswordCheckerClick`() {
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Password")
            .onSiblings()
            .onFirst()
            .performClick()

        verify {
            viewModel.trySendAction(VaultAddItemAction.ItemType.LoginType.PasswordCheckerClick)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login state click Password text field generator action should trigger OpenPasswordGeneratorClick`() {
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Password")
            .onSiblings()
            .onLast()
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.ItemType.LoginType.OpenPasswordGeneratorClick,
            )
        }
    }

    @Test
    fun `in ItemType_Login state changing Password text field should trigger PasswordTextChange`() {
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Password")
            .performTextInput(text = "TestPassword")

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.ItemType.LoginType.PasswordTextChange("TestPassword"),
            )
        }
    }

    @Test
    fun `in ItemType_Login the Password control should display the text provided by the state`() {
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Password")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(password = "NewPassword") }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Password")
            .assertTextContains("•••••••••••")
    }

    @Test
    fun `in ItemType_Login state clicking Set up TOTP button should trigger SetupTotpClick`() {
        composeTestRule
            .onNodeWithTextAfterScroll(text = "Set up TOTP")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.ItemType.LoginType.SetupTotpClick,
            )
        }
    }

    @Test
    fun `in ItemType_Login state changing URI text field should trigger UriTextChange`() {
        composeTestRule
            .onNodeWithTextAfterScroll("URI")
            .performTextInput("TestURI")

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.ItemType.LoginType.UriTextChange("TestURI"),
            )
        }
    }

    @Test
    fun `in ItemType_Login the URI control should display the text provided by the state`() {
        composeTestRule
            .onNodeWithTextAfterScroll("URI")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(uri = "NewURI") }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "URI")
            .assertTextContains("NewURI")
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login state clicking the URI settings action should trigger UriSettingsClick`() {
        composeTestRule
            .onNodeWithTextAfterScroll(text = "URI")
            .onSiblings()
            .filterToOne(hasContentDescription(value = "Options"))
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.ItemType.LoginType.UriSettingsClick,
            )
        }
    }

    @Test
    fun `in ItemType_Login state clicking the New URI button should trigger AddNewUriClick`() {
        composeTestRule
            .onNodeWithTextAfterScroll(text = "New URI")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.ItemType.LoginType.AddNewUriClick,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking New Custom Field button should allow creation of Linked type`() {
        mutableStateFlow.value = DEFAULT_STATE_LOGIN

        composeTestRule
            .onNodeWithTextAfterScroll(text = "New custom field")
            .performClick()

        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(text = "Linked")
            .performClick()

        composeTestRule
            .onNodeWithText("Name")
            .performTextInput("TestLinked")

        composeTestRule
            .onNodeWithText("Ok")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.Common.AddNewCustomFieldClick(
                    customFieldType = CustomFieldType.LINKED,
                    name = "TestLinked",
                ),
            )
        }
    }

    @Test
    fun `clicking a Ownership option should send OwnershipChange action`() {
        // Opens the menu
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(
                label = "Who owns this item?, placeholder@email.com",
            )
            .performClick()

        // Choose the option from the menu
        composeTestRule
            .onAllNodesWithText(text = "a@b.com")
            .onLast()
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.Common.OwnershipChange("a@b.com"),
            )
        }
    }

    @Test
    fun `the Ownership control should display the text provided by the state`() {
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(
                label = "Who owns this item?, placeholder@email.com",
            )
            .assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateCommonContent(currentState) { copy(ownership = "Owner 2") }
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Who owns this item?, Owner 2")
            .assertIsDisplayed()
    }

    @Test
    fun `changing Name text field should trigger NameTextChange`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Name")
            .performTextInput(text = "TestName")

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.Common.NameTextChange(name = "TestName"),
            )
        }
    }

    @Test
    fun `the name control should display the text provided by the state`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Name")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateCommonContent(currentState) { copy(name = "NewName") }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Name")
            .assertTextContains("NewName")
    }

    @Test
    fun `clicking a Folder Option should send FolderChange action`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        // Opens the menu
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Folder, No Folder")
            .performClick()

        // Choose the option from the menu
        composeTestRule
            .onAllNodesWithText(text = "Folder 1")
            .onLast()
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.Common.FolderChange("Folder 1".asText()),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `the folder control should display the text provided by the state`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Folder, No Folder")
            .assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateCommonContent(currentState) { copy(folderName = "Folder 2".asText()) }
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Folder, Folder 2")
            .assertIsDisplayed()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toggling the favorite toggle should send ToggleFavorite action`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule
            .onNodeWithTextAfterScroll("Favorite")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.Common.ToggleFavorite(
                    isFavorite = true,
                ),
            )
        }
    }

    @Test
    fun `the favorite toggle should be enabled or disabled according to state`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule
            .onNodeWithTextAfterScroll("Favorite")
            .assertIsOff()

        mutableStateFlow.update { currentState ->
            updateCommonContent(currentState) { copy(favorite = true) }
        }

        composeTestRule
            .onNodeWithTextAfterScroll("Favorite")
            .assertIsOn()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toggling the Master password re-prompt toggle should send ToggleMasterPasswordReprompt action`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule
            .onNodeWithTextAfterScroll("Master password re-prompt")
            .performTouchInput {
                click(position = Offset(x = 1f, y = center.y))
            }

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.Common.ToggleMasterPasswordReprompt(
                    isMasterPasswordReprompt = true,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `the master password re-prompt toggle should be enabled or disabled according to state`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule
            .onNodeWithTextAfterScroll("Master password re-prompt")
            .assertIsOff()

        mutableStateFlow.update { currentState ->
            updateCommonContent(currentState) { copy(masterPasswordReprompt = true) }
        }

        composeTestRule
            .onNodeWithTextAfterScroll("Master password re-prompt")
            .assertIsOn()
    }

    @Test
    fun `toggling the Master password re-prompt tooltip button should send TooltipClick action`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Master password re-prompt help")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.Common.TooltipClick,
            )
        }
    }

    @Test
    fun `changing Notes text field should trigger NotesTextChange`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule
            .onAllNodesWithTextAfterScroll("Notes")
            .filterToOne(hasSetTextAction())
            .performTextInput("TestNotes")

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.Common.NotesTextChange("TestNotes"),
            )
        }
    }

    @Test
    fun `the Notes control should display the text provided by the state`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule
            .onAllNodesWithTextAfterScroll("Notes")
            .filterToOne(hasSetTextAction())
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateCommonContent(currentState) { copy(notes = "NewNote") }
        }

        composeTestRule
            .onAllNodesWithTextAfterScroll("Notes")
            .filterToOne(hasSetTextAction())
            .assertTextContains("NewNote")
    }

    @Suppress("MaxLineLength")
    @Test
    fun `Ownership option should send OwnershipChange action`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        // Opens the menu
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Who owns this item?, placeholder@email.com")
            .performClick()

        // Choose the option from the menu
        composeTestRule
            .onAllNodesWithText(text = "a@b.com")
            .onLast()
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.Common.OwnershipChange("a@b.com"),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_SecureNotes the Ownership control should display the text provided by the state`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Who owns this item?, placeholder@email.com")
            .assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateCommonContent(currentState) { copy(ownership = "Owner 2") }
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Who owns this item?, Owner 2")
            .assertIsDisplayed()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking New Custom Field button should allow creation of Text type`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule
            .onNodeWithTextAfterScroll(text = "New custom field")
            .performClick()

        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(text = "Text")
            .performClick()

        composeTestRule
            .onNodeWithText("Name")
            .performTextInput("TestText")

        composeTestRule
            .onNodeWithText("Ok")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.Common.AddNewCustomFieldClick(
                    customFieldType = CustomFieldType.TEXT,
                    name = "TestText",
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking New Custom Field button should not display linked type`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule
            .onNodeWithTextAfterScroll(text = "New custom field")
            .performClick()

        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(text = "Linked")
            .assertIsNotDisplayed()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking New Custom Field button should allow creation of Boolean type`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule
            .onNodeWithTextAfterScroll(text = "New custom field")
            .performClick()

        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(text = "Boolean")
            .performClick()

        composeTestRule
            .onNodeWithText("Name")
            .performTextInput("TestBoolean")

        composeTestRule
            .onNodeWithText("Ok")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.Common.AddNewCustomFieldClick(
                    customFieldType = CustomFieldType.BOOLEAN,
                    name = "TestBoolean",
                ),
            )
        }
    }

    @Test
    fun `clicking New Custom Field button should allow creation of Hidden type`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule
            .onNodeWithTextAfterScroll(text = "New custom field")
            .performClick()

        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(text = "Hidden")
            .performClick()

        composeTestRule
            .onNodeWithText("Name")
            .performTextInput("TestHidden")

        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.Common.AddNewCustomFieldClick(
                    customFieldType = CustomFieldType.HIDDEN,
                    name = "TestHidden",
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking and changing the custom text field will send a CustomFieldValueChange event`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES_CUSTOM_FIELDS

        composeTestRule
            .onNodeWithTextAfterScroll("TestText")
            .performTextClearance()

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.Common.CustomFieldValueChange(
                    VaultAddItemState.Custom.TextField("Test ID", "TestText", ""),
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking and changing the custom hidden field will send a CustomFieldValueChange event`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES_CUSTOM_FIELDS

        composeTestRule
            .onNodeWithTextAfterScroll("TestHidden")
            .performTextClearance()

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.Common.CustomFieldValueChange(
                    VaultAddItemState.Custom.HiddenField("Test ID", "TestHidden", ""),
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking and changing the custom boolean field will send a CustomFieldValueChange event`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES_CUSTOM_FIELDS

        composeTestRule
            .onNodeWithTextAfterScroll("TestBoolean")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.Common.CustomFieldValueChange(
                    VaultAddItemState.Custom.BooleanField("Test ID", "TestBoolean", true),
                ),
            )
        }
    }

    //region Helper functions

    @Suppress("MaxLineLength")
    private fun updateLoginType(
        currentState: VaultAddItemState,
        transform: VaultAddItemState.ViewState.Content.ItemType.Login.() ->
    VaultAddItemState.ViewState.Content.ItemType.Login,
    ): VaultAddItemState {
        val updatedType = when (val viewState = currentState.viewState) {
            is VaultAddItemState.ViewState.Content -> {
                when (val type = viewState.type) {
                    is VaultAddItemState.ViewState.Content.ItemType.Login -> {
                        viewState.copy(
                            type = type.transform(),
                        )
                    }
                    else -> viewState
                }
            }
            else -> viewState
        }
        return currentState.copy(viewState = updatedType)
    }

    @Suppress("MaxLineLength")
    private fun updateCommonContent(
        currentState: VaultAddItemState,
        transform: VaultAddItemState.ViewState.Content.Common.()
        -> VaultAddItemState.ViewState.Content.Common,
    ): VaultAddItemState {
        val updatedType = when (val viewState = currentState.viewState) {
            is VaultAddItemState.ViewState.Content ->
                viewState.copy(common = viewState.common.transform())
            else -> viewState
        }
        return currentState.copy(viewState = updatedType)
    }

    //endregion Helper functions

    companion object {
        private val DEFAULT_STATE_LOGIN_DIALOG = VaultAddItemState(
            viewState = VaultAddItemState.ViewState.Content(
                common = VaultAddItemState.ViewState.Content.Common(),
                type = VaultAddItemState.ViewState.Content.ItemType.Login(),
            ),
            dialog = VaultAddItemState.DialogState.Error("test".asText()),
            vaultAddEditType = VaultAddEditType.AddItem,
        )

        private val DEFAULT_STATE_LOGIN = VaultAddItemState(
            vaultAddEditType = VaultAddEditType.AddItem,
            viewState = VaultAddItemState.ViewState.Content(
                common = VaultAddItemState.ViewState.Content.Common(),
                type = VaultAddItemState.ViewState.Content.ItemType.Login(),
            ),
            dialog = null,
        )

        @Suppress("MaxLineLength")
        private val DEFAULT_STATE_SECURE_NOTES_CUSTOM_FIELDS = VaultAddItemState(
            viewState = VaultAddItemState.ViewState.Content(
                common = VaultAddItemState.ViewState.Content.Common(
                    customFieldData = listOf(
                        VaultAddItemState.Custom.BooleanField("Test ID", "TestBoolean", false),
                        VaultAddItemState.Custom.TextField("Test ID", "TestText", "TestTextVal"),
                        VaultAddItemState.Custom.HiddenField("Test ID", "TestHidden", "TestHiddenVal"),
                    ),
                ),
                type = VaultAddItemState.ViewState.Content.ItemType.SecureNotes,
            ),
            dialog = null,
            vaultAddEditType = VaultAddEditType.AddItem,
        )

        private val DEFAULT_STATE_SECURE_NOTES = VaultAddItemState(
            vaultAddEditType = VaultAddEditType.AddItem,
            viewState = VaultAddItemState.ViewState.Content(
                common = VaultAddItemState.ViewState.Content.Common(),
                type = VaultAddItemState.ViewState.Content.ItemType.SecureNotes,
            ),
            dialog = null,
        )
    }
}
