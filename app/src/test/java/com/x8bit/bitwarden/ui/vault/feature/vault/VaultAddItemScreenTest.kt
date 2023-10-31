package com.x8bit.bitwarden.ui.vault.feature.vault

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import org.junit.Test

class VaultAddItemScreenTest : BaseComposeTest() {
    private val mutableStateFlow = MutableStateFlow(
        VaultAddItemState(
            selectedType = VaultAddItemState.ItemType.Login(),
        ),
    )

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
    fun `clicking a Type Option should send TypeOptionSelect action`() {
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        // Opens the menu
        composeTestRule
            .onNodeWithContentDescription(label = "Type, Login")
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
            .onNodeWithContentDescription(label = "Type, Login")
            .assertIsDisplayed()

        mutableStateFlow.update { it.copy(selectedType = VaultAddItemState.ItemType.Card) }

        composeTestRule
            .onNodeWithContentDescription(label = "Type, Card")
            .assertIsDisplayed()
    }

    @Test
    fun `in ItemType_Login state changing Name text field should trigger NameTextChange`() {
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithText(text = "Name")
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
            .onNodeWithText(text = "Name")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(name = "NewName") }
        }

        composeTestRule
            .onNodeWithText(text = "Name")
            .assertTextContains("NewName")
    }

    @Test
    fun `in ItemType_Login state changing Username text field should trigger UsernameTextChange`() {
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithText(text = "Username")
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
            .onNodeWithText(text = "Username")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(username = "NewUsername") }
        }

        composeTestRule
            .onNodeWithText(text = "Username")
            .assertTextContains("NewUsername")
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login state clicking Username generator action should trigger OpenUsernameGeneratorClick`() {
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithContentDescription(label = "Generate username")
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
            .onNodeWithText(text = "Password")
            .onSiblings()
            .onFirst()
            .performScrollTo()
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
            .onNodeWithText(text = "Password")
            .onSiblings()
            .onLast()
            .performScrollTo()
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
            .onNodeWithText(text = "Password")
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
            .onNodeWithText(text = "Password")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(password = "NewPassword") }
        }

        composeTestRule
            .onNodeWithText(text = "Password")
            .assertTextContains("•••••••••••")
    }

    @Test
    fun `in ItemType_Login state clicking Set up TOTP button should trigger SetupTotpClick`() {
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithText(text = "Set up TOTP")
            .performScrollTo()
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
            .onNodeWithText("URI")
            .performScrollTo()
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
            .onNodeWithText(text = "URI")
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(uri = "NewURI") }
        }

        composeTestRule
            .onNodeWithText(text = "URI")
            .assertTextContains("NewURI")
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login state clicking the URI settings action should trigger UriSettingsClick`() {
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithText(text = "URI")
            .onSiblings()
            .filterToOne(hasContentDescription(value = "Options"))
            .performScrollTo()
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
            .onNodeWithText(text = "New URI")
            .performScrollTo()
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
            .onNodeWithContentDescription(label = "Folder, No Folder")
            .performScrollTo()
            .performClick()

        // Choose the option from the menu
        composeTestRule
            .onAllNodesWithText(text = "Folder 1")
            .onLast()
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAddItemAction.ItemType.LoginType.FolderChange("Folder 1"),
            )
        }
    }

    @Test
    fun `in ItemType_Login the folder control should display the text provided by the state`() {
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithContentDescription(label = "Folder, No Folder")
            .performScrollTo()
            .assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(folder = "Folder 2") }
        }

        composeTestRule
            .onNodeWithContentDescription(label = "Folder, Folder 2")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login state, toggling the favorite toggle should send ToggleFavorite action`() {
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule.onNodeWithText("Favorite")
            .performScrollTo()
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
            .onNodeWithText(text = "Favorite")
            .assertIsOff()

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(favorite = true) }
        }

        composeTestRule
            .onNodeWithText(text = "Favorite")
            .assertIsOn()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login state, toggling the Master password re-prompt toggle should send ToggleMasterPasswordReprompt action`() {
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithText("Master password re-prompt")
            .performScrollTo()
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
            .onNodeWithText("Master password re-prompt")
            .assertIsOff()

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(masterPasswordReprompt = true) }
        }

        composeTestRule
            .onNodeWithText("Master password re-prompt")
            .assertIsOn()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login state, toggling the Master password re-prompt tooltip button should send TooltipClick action`() {
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule.onNodeWithContentDescription(label = "Master password re-prompt help")
            .performScrollTo()
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
            .onNode(hasSetTextAction() and hasText("Notes"))
            .performScrollTo()
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
            .onNode(hasSetTextAction() and hasText("Notes"))
            .assertTextContains("")

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(notes = "NewNote") }
        }

        composeTestRule
            .onNode(hasSetTextAction() and hasText("Notes"))
            .assertTextContains("NewNote")
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in ItemType_Login state clicking New Custom Field button should trigger AddNewCustomFieldClick`() {
        composeTestRule.setContent {
            VaultAddItemScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeTestRule
            .onNodeWithText(text = "New custom field")
            .performScrollTo()
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
            .onNodeWithContentDescription(label = "Who owns this item?, placeholder@email.com")
            .performScrollTo()
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
            .onNodeWithContentDescription(label = "Who owns this item?, placeholder@email.com")
            .performScrollTo()
            .assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(ownership = "Owner 2") }
        }

        composeTestRule
            .onNodeWithContentDescription(label = "Who owns this item?, Owner 2")
            .performScrollTo()
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

    //endregion Helper functions
}
