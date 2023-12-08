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
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.util.onAllNodesWithTextAfterScroll
import com.x8bit.bitwarden.ui.util.onNodeWithContentDescriptionAfterScroll
import com.x8bit.bitwarden.ui.util.onNodeWithTextAfterScroll
import com.x8bit.bitwarden.ui.vault.model.VaultAddEditType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import org.junit.Test

@Suppress("LargeClass")
class VaultAddItemScreenTest : BaseComposeTest() {

    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE_LOGIN)

    private val viewModel = mockk<VaultAddItemViewModel>(relaxed = true) {
        every { eventFlow } returns emptyFlow()
        every { stateFlow } returns mutableStateFlow
    }

    @Test
    fun `clicking close button should send CloseClick action`() {
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithContentDescription(label = "Close")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.CloseClick,
            )
        }
    }

    @Test
    fun `clicking save button should send SaveClick action`() {
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithText(text = "Save")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.SaveClick,
            )
        }
    }

    @Test
    fun `clicking dismiss dialog button should send DismissDialog action`() {
        mutableStateFlow.value = DEFAULT_STATE_LOGIN_DIALOG

        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.DismissDialog,
            )
        }
    }

    @Test
    fun `dialog should display when state is updated to do so`() {
        mutableStateFlow.value = DEFAULT_STATE_LOGIN

        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

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
    fun `clicking a Type Option should send TypeOptionSelect action`() {
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

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
                VaultAddItemAction.TypeOptionSelect(VaultAddItemState.ItemTypeOption.LOGIN),
            )
        }
    }

    @Test
    fun `the Type Option field should display the text of the selected item type`() {
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Type, Login")
            .assertIsDisplayed()

        mutableStateFlow.update { it.copy(selectedType = VaultAddItemState.ItemType.Card()) }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Type, Card")
            .assertIsDisplayed()
    }

    @Test
    fun `in ItemType_Login state changing Name text field should trigger NameTextChange`() {
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Name")
            .performTextInput(text = "TestName")

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.ItemType.LoginType.NameTextChange(name = "TestName"),
            )
        }
    }

    @Test
    fun `in ItemType_Login the name control should display the text provided by the state`() {
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Name")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(name = "NewName") }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Name")
            .assertTextContains("NewName")
    }

    @Test
    fun `in ItemType_Login state changing Username text field should trigger UsernameTextChange`() {
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

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
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

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
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

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
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

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
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

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
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

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
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

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
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

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
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

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
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

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
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

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
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "New URI")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.ItemType.LoginType.AddNewUriClick,
            )
        }
    }

    @Test
    fun `in ItemType_Login state clicking a Folder Option should send FolderChange action`() {
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

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
                VaultAddItemAction.ItemType.LoginType.FolderChange("Folder 1".asText()),
            )
        }
    }

    @Test
    fun `in ItemType_Login the folder control should display the text provided by the state`() {
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Folder, No Folder")
            .assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(folderName = "Folder 2".asText()) }
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Folder, Folder 2")
            .assertIsDisplayed()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login state, toggling the favorite toggle should send ToggleFavorite action`() {
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithTextAfterScroll("Favorite")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.ItemType.LoginType.ToggleFavorite(
                    isFavorite = true,
                ),
            )
        }
    }

    @Test
    fun `in ItemType_Login the favorite toggle should be enabled or disabled according to state`() {
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithTextAfterScroll("Favorite")
            .assertIsOff()

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(favorite = true) }
        }

        composeTestRule
            .onNodeWithTextAfterScroll("Favorite")
            .assertIsOn()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login state, toggling the Master password re-prompt toggle should send ToggleMasterPasswordReprompt action`() {
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithTextAfterScroll("Master password re-prompt")
            .performTouchInput {
                click(position = Offset(x = 1f, y = center.y))
            }

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.ItemType.LoginType.ToggleMasterPasswordReprompt(
                    isMasterPasswordReprompt = true,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login the master password re-prompt toggle should be enabled or disabled according to state`() {
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithTextAfterScroll("Master password re-prompt")
            .assertIsOff()

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(masterPasswordReprompt = true) }
        }

        composeTestRule
            .onNodeWithTextAfterScroll("Master password re-prompt")
            .assertIsOn()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login state, toggling the Master password re-prompt tooltip button should send TooltipClick action`() {
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Master password re-prompt help")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.ItemType.LoginType.TooltipClick,
            )
        }
    }

    @Test
    fun `in ItemType_Login state changing Notes text field should trigger NotesTextChange`() {
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onAllNodesWithTextAfterScroll("Notes")
            .filterToOne(hasSetTextAction())
            .performTextInput("TestNotes")

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.ItemType.LoginType.NotesTextChange("TestNotes"),
            )
        }
    }

    @Test
    fun `in ItemType_Login the Notes control should display the text provided by the state`() {
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onAllNodesWithTextAfterScroll("Notes")
            .filterToOne(hasSetTextAction())
            .performTextInput("")

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(notes = "NewNote") }
        }

        composeTestRule
            .onAllNodesWithTextAfterScroll("Notes")
            .filterToOne(hasSetTextAction())
            .assertTextContains("NewNote")
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login state clicking New Custom Field button should trigger AddNewCustomFieldClick`() {
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "New custom field")
            .performClick()

        verify {
            viewModel.trySendAction(VaultAddItemAction.ItemType.LoginType.AddNewCustomFieldClick)
        }
    }

    @Test
    fun `in ItemType_Login state clicking a Ownership option should send OwnershipChange action`() {
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

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
                VaultAddItemAction.ItemType.LoginType.OwnershipChange("a@b.com"),
            )
        }
    }

    @Test
    fun `in ItemType_Login the Ownership control should display the text provided by the state`() {
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(
                label = "Who owns this item?, placeholder@email.com",
            )
            .assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(ownership = "Owner 2") }
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Who owns this item?, Owner 2")
            .assertIsDisplayed()
    }

    @Test
    fun `in ItemType_SecureNotes state changing Name text field should trigger NameTextChange`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Name")
            .performTextInput(text = "TestName")

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.ItemType.SecureNotesType.NameTextChange(name = "TestName"),
            )
        }
    }

    @Test
    fun `in ItemType_SecureNotes the name control should display the text provided by the state`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Name")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateSecureNotesType(currentState) { copy(name = "NewName") }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "Name")
            .assertTextContains("NewName")
    }

    @Test
    fun `in ItemType_SecureNotes state clicking a Folder Option should send FolderChange action`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

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
                VaultAddItemAction.ItemType.SecureNotesType.FolderChange("Folder 1".asText()),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_SecureNotes the folder control should display the text provided by the state`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Folder, No Folder")
            .assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateSecureNotesType(currentState) { copy(folderName = "Folder 2".asText()) }
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Folder, Folder 2")
            .assertIsDisplayed()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_SecureNotes state, toggling the favorite toggle should send ToggleFavorite action`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithTextAfterScroll("Favorite")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.ItemType.SecureNotesType.ToggleFavorite(
                    isFavorite = true,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_SecureNotes the favorite toggle should be enabled or disabled according to state`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithTextAfterScroll("Favorite")
            .assertIsOff()

        mutableStateFlow.update { currentState ->
            updateSecureNotesType(currentState) { copy(favorite = true) }
        }

        composeTestRule
            .onNodeWithTextAfterScroll("Favorite")
            .assertIsOn()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_SecureNotes state, toggling the Master password re-prompt toggle should send ToggleMasterPasswordReprompt action`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithTextAfterScroll("Master password re-prompt")
            .performTouchInput {
                click(position = Offset(x = 1f, y = center.y))
            }

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.ItemType.SecureNotesType.ToggleMasterPasswordReprompt(
                    isMasterPasswordReprompt = true,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_SecureNotes the master password re-prompt toggle should be enabled or disabled according to state`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithTextAfterScroll("Master password re-prompt")
            .assertIsOff()

        mutableStateFlow.update { currentState ->
            updateSecureNotesType(currentState) { copy(masterPasswordReprompt = true) }
        }

        composeTestRule
            .onNodeWithTextAfterScroll("Master password re-prompt")
            .assertIsOn()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_SecureNotes state, toggling the Master password re-prompt tooltip button should send TooltipClick action`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Master password re-prompt help")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.ItemType.SecureNotesType.TooltipClick,
            )
        }
    }

    @Test
    fun `in ItemType_SecureNotes state changing Notes text field should trigger NotesTextChange`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onAllNodesWithTextAfterScroll("Notes")
            .filterToOne(hasSetTextAction())
            .performTextInput("TestNotes")

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.ItemType.SecureNotesType.NotesTextChange("TestNotes"),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_SecureNotes the Notes control should display the text provided by the state`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onAllNodesWithTextAfterScroll("Notes")
            .filterToOne(hasSetTextAction())
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateSecureNotesType(currentState) { copy(notes = "NewNote") }
        }

        composeTestRule
            .onAllNodesWithTextAfterScroll("Notes")
            .filterToOne(hasSetTextAction())
            .assertTextContains("NewNote")
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_SecureNotes state clicking New Custom Field button should trigger AddNewCustomFieldClick`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "New custom field")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.ItemType.SecureNotesType.AddNewCustomFieldClick,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_SecureNotes state clicking a Ownership option should send OwnershipChange action`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

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
                VaultAddItemAction.ItemType.SecureNotesType.OwnershipChange("a@b.com"),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_SecureNotes the Ownership control should display the text provided by the state`() {
        mutableStateFlow.value = DEFAULT_STATE_SECURE_NOTES

        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Who owns this item?, placeholder@email.com")
            .assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateSecureNotesType(currentState) { copy(ownership = "Owner 2") }
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll(label = "Who owns this item?, Owner 2")
            .assertIsDisplayed()
    }

    //region Helper functions

    private fun updateLoginType(
        currentState: VaultAddItemState,
        transform: VaultAddItemState.ItemType.Login.() -> VaultAddItemState.ItemType.Login,
    ): VaultAddItemState {
        val updatedType = when (val currentType = currentState.selectedType) {
            is VaultAddItemState.ItemType.Login -> currentType.transform()
            else -> currentType
        }
        return currentState.copy(selectedType = updatedType)
    }

    @Suppress("MaxLineLength")
    private fun updateSecureNotesType(
        currentState: VaultAddItemState,
        transform: VaultAddItemState.ItemType.SecureNotes.() -> VaultAddItemState.ItemType.SecureNotes,
    ): VaultAddItemState {
        val updatedType = when (val currentType = currentState.selectedType) {
            is VaultAddItemState.ItemType.SecureNotes -> currentType.transform()
            else -> currentType
        }
        return currentState.copy(selectedType = updatedType)
    }

    //endregion Helper functions

    companion object {
        private val DEFAULT_STATE_LOGIN_DIALOG = VaultAddItemState(
            selectedType = VaultAddItemState.ItemType.Login(),
            dialog = VaultAddItemState.DialogState.Error("test".asText()),
            vaultAddEditType = VaultAddEditType.AddItem,
        )

        private val DEFAULT_STATE_LOGIN = VaultAddItemState(
            vaultAddEditType = VaultAddEditType.AddItem,
            selectedType = VaultAddItemState.ItemType.Login(),
            dialog = null,
        )

        private val DEFAULT_STATE_SECURE_NOTES = VaultAddItemState(
            vaultAddEditType = VaultAddEditType.AddItem,
            selectedType = VaultAddItemState.ItemType.SecureNotes(),
            dialog = null,
        )
    }
}
