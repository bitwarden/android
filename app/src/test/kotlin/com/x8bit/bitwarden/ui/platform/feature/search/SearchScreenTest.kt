package com.x8bit.bitwarden.ui.platform.feature.search

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.core.net.toUri
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.send.SendType
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.assertMasterPasswordDialogDisplayed
import com.bitwarden.ui.util.assertNoDialogExists
import com.bitwarden.ui.util.isProgressBar
import com.bitwarden.vault.CipherType
import com.x8bit.bitwarden.data.platform.manager.util.AppResumeStateManager
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import com.x8bit.bitwarden.ui.platform.feature.search.model.AutofillSelectionOption
import com.x8bit.bitwarden.ui.platform.feature.search.util.createMockDisplayItemForCipher
import com.x8bit.bitwarden.ui.platform.feature.search.util.createMockDisplayItemForSend
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.AddEditSendRoute
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.ModeType
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendItemType
import com.x8bit.bitwarden.ui.tools.feature.send.viewsend.ViewSendRoute
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditArgs
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemArgs
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import com.x8bit.bitwarden.ui.vault.model.VaultAddEditType
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@Suppress("LargeClass")
class SearchScreenTest : BitwardenComposeTest() {
    private val mutableEventFlow = bufferedMutableSharedFlow<SearchEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<SearchViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }
    private val intentManager: IntentManager = mockk {
        every { shareText(any()) } just runs
        every { launchUri(any()) } just runs
    }

    private val appResumeStateManager: AppResumeStateManager = mockk(relaxed = true)

    private var onNavigateBackCalled = false
    private var onNavigateToAddEditSendRoute: AddEditSendRoute? = null
    private var onNavigateToViewSendRoute: ViewSendRoute? = null
    private var onNavigateToEditCipherArgs: VaultAddEditArgs? = null
    private var onNavigateToViewCipherArgs: VaultItemArgs? = null

    @Before
    fun setup() {
        setContent(
            appResumeStateManager = appResumeStateManager,
            intentManager = intentManager,
        ) {
            SearchScreen(
                viewModel = viewModel,
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToAddEditSend = { onNavigateToAddEditSendRoute = it },
                onNavigateToViewSend = { onNavigateToViewSendRoute = it },
                onNavigateToEditCipher = { onNavigateToEditCipherArgs = it },
                onNavigateToViewCipher = { onNavigateToViewCipherArgs = it },
            )
        }
    }

    @Test
    fun `NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(SearchEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `NavigateToEditSend should call onNavigateToAddEditSend`() {
        val sendId = "sendId"
        val sendType = SendItemType.FILE
        mutableEventFlow.tryEmit(
            SearchEvent.NavigateToEditSend(sendId = sendId, sendType = sendType),
        )
        assertEquals(
            AddEditSendRoute(sendType = sendType, modeType = ModeType.EDIT, sendId = sendId),
            onNavigateToAddEditSendRoute,
        )
    }

    @Test
    fun `NavigateToViewSend should call onNavigateToViewSend`() {
        val sendId = "sendId1234"
        val sendType = SendItemType.TEXT
        mutableEventFlow.tryEmit(
            SearchEvent.NavigateToViewSend(sendId = sendId, sendType = sendType),
        )
        assertEquals(
            ViewSendRoute(sendId = sendId, sendType = sendType),
            onNavigateToViewSendRoute,
        )
    }

    @Test
    fun `NavigateToEditCipher should call onNavigateToEditCipher`() {
        val cipherId = "cipherId"
        val cipherType = VaultItemCipherType.LOGIN
        val args = VaultAddEditArgs(
            vaultAddEditType = VaultAddEditType.EditItem(vaultItemId = cipherId),
            vaultItemCipherType = cipherType,
        )
        mutableEventFlow.tryEmit(SearchEvent.NavigateToEditCipher(cipherId, cipherType))
        assertEquals(args, onNavigateToEditCipherArgs)
    }

    @Test
    fun `NavigateToViewCipher should call onNavigateToViewCipher`() {
        val cipherId = "cipherId"
        val cipherType = VaultItemCipherType.LOGIN
        val args = VaultItemArgs(vaultItemId = cipherId, cipherType = cipherType)
        mutableEventFlow.tryEmit(SearchEvent.NavigateToViewCipher(cipherId, cipherType))
        assertEquals(args, onNavigateToViewCipherArgs)
    }

    @Test
    fun `NavigateToUrl should call launchUri on the IntentManager`() {
        val url = "www.test.com"
        mutableEventFlow.tryEmit(SearchEvent.NavigateToUrl(url))
        verify(exactly = 1) {
            intentManager.launchUri(url.toUri())
        }
    }

    @Test
    fun `on ShowSnackbar event should display the snackbar`() {
        val message = "message"
        val data = BitwardenSnackbarData(message = message.asText())
        mutableEventFlow.tryEmit(SearchEvent.ShowSnackbar(data = data))
        composeTestRule
            .onNodeWithText(text = message)
            .assertIsDisplayed()
    }

    @Test
    fun `ShowShareSheet should call onNavigateBack`() {
        val sendUrl = "www.test.com"
        mutableEventFlow.tryEmit(SearchEvent.ShowShareSheet(sendUrl))
        verify {
            intentManager.shareText(sendUrl)
        }
    }

    @Test
    fun `clicking back button should send BackClick action`() {
        composeTestRule.onNodeWithContentDescription(label = "Back").performClick()
        verify { viewModel.trySendAction(SearchAction.BackClick) }
    }

    @Test
    fun `progressbar should be displayed according to state`() {
        mutableStateFlow.update { DEFAULT_STATE }
        composeTestRule.onNode(isProgressBar).assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(viewState = SearchState.ViewState.Empty(message = null))
        }

        composeTestRule.onNode(isProgressBar).assertDoesNotExist()
    }

    @Test
    fun `error text and retry should be displayed according to state`() {
        val errorMessage = "error_message"
        mutableStateFlow.update { DEFAULT_STATE }
        composeTestRule.onNodeWithText(errorMessage).assertIsNotDisplayed()

        mutableStateFlow.update {
            it.copy(viewState = SearchState.ViewState.Empty(message = null))
        }
        composeTestRule.onNodeWithText(errorMessage).assertIsNotDisplayed()

        mutableStateFlow.update {
            it.copy(viewState = SearchState.ViewState.Error(errorMessage.asText()))
        }
        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
    }

    @Test
    fun `empty text should be displayed according to state`() {
        val emptyMessage = "empty_message"
        mutableStateFlow.update {
            DEFAULT_STATE.copy(
                viewState = SearchState.ViewState.Empty(message = emptyMessage.asText()),
            )
        }
        composeTestRule.onNodeWithText(text = emptyMessage).assertIsDisplayed()
    }

    @Test
    fun `display items text should be displayed according to state`() {
        val items = "mockName-1"
        mutableStateFlow.update { DEFAULT_STATE }
        composeTestRule.onNodeWithText(text = items).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                viewState = SearchState.ViewState.Content(
                    displayItems = listOf(
                        createMockDisplayItemForCipher(number = 1),
                    ),
                ),
            )
        }
        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(items))
        composeTestRule.onNodeWithText(text = items).assertIsDisplayed()
    }

    @Test
    fun `clicking on a display item should send ItemClick action`() {
        mutableStateFlow.update {
            it.copy(
                viewState = SearchState.ViewState.Content(
                    displayItems = listOf(
                        createMockDisplayItemForCipher(number = 1),
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(text = "mockName-1")
            .assertIsDisplayed()
            .performClick()
        verify {
            viewModel.trySendAction(
                SearchAction.ItemClick(
                    itemId = "mockId-1",
                    itemType = SearchState.DisplayItem.ItemType.Vault(
                        type = CipherType.LOGIN,
                    ),
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking on a display item with autofill options should open the autofill option selection dialog`() {
        mutableStateFlow.value = createStateForAutofill()
        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onNodeWithText(text = "mockName-1")
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onNodeWithText("Do you want to autofill or view this item?")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Autofill")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Autofill and save")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("View")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Cancel")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        verify(exactly = 0) { viewModel.trySendAction(any()) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking on cancel in selection dialog should close dialog`() {
        mutableStateFlow.value = createStateForAutofill()
        composeTestRule
            .onNodeWithText(text = "mockName-1")
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onNodeWithText("Cancel")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking on autofill option in selection dialog when no reprompt required should send AutofillItemClick and close dialog`() {
        mutableStateFlow.value = createStateForAutofill()
        composeTestRule
            .onNodeWithText(text = "mockName-1")
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onNodeWithText("Autofill")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        verify { viewModel.trySendAction(SearchAction.AutofillItemClick(itemId = "mockId-1")) }
        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking on autofill option in selection dialog when reprompt is required should show master password dialog`() {
        mutableStateFlow.value = createStateForAutofill(isRepromptRequired = true)
        composeTestRule
            .onNodeWithText(text = "mockName-1")
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onNodeWithText("Autofill")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule.assertMasterPasswordDialogDisplayed()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking on autofill-and-save option in selection dialog when no reprompt required should send AutofillAndSaveItemClick and close dialog`() {
        mutableStateFlow.value = createStateForAutofill()
        composeTestRule
            .onNodeWithText(text = "mockName-1")
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onNodeWithText("Autofill and save")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        verify { viewModel.trySendAction(SearchAction.AutofillAndSaveItemClick(itemId = "mockId-1")) }
        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking on autofill-and-save option in selection dialog when reprompt is required should show master password dialog`() {
        mutableStateFlow.value = createStateForAutofill(isRepromptRequired = true)
        composeTestRule
            .onNodeWithText(text = "mockName-1")
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onNodeWithText("Autofill and save")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule.assertMasterPasswordDialogDisplayed()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking on view option in selection dialog when no reprompt required should send ItemClick and close dialog`() {
        mutableStateFlow.value = createStateForAutofill()
        composeTestRule
            .onNodeWithText(text = "mockName-1")
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onNodeWithText("View")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                SearchAction.ItemClick(
                    itemId = "mockId-1",
                    itemType = SearchState.DisplayItem.ItemType.Vault(
                        type = CipherType.LOGIN,
                    ),
                ),
            )
        }
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `clicking on item when reprompt is required should show master password dialog`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = SearchState.ViewState.Content(
                displayItems = listOf(
                    createMockDisplayItemForCipher(number = 1).copy(
                        shouldDisplayMasterPasswordReprompt = true,
                    ),
                ),
            ),
            totpData = mockk(),
        )
        composeTestRule
            .onNodeWithText(text = "mockName-1")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.assertMasterPasswordDialogDisplayed()
    }

    @Test
    fun `clicking cancel on the master password dialog should close the dialog`() {
        mutableStateFlow.value = createStateForAutofill(isRepromptRequired = true)
        composeTestRule
            .onNodeWithText(text = "mockName-1")
            .assertIsDisplayed()
            .performClick()
        composeTestRule
            .onNodeWithText("Autofill")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule
            .onNodeWithText("Cancel")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking submit on the master password dialog for autofill should close the dialog and send MasterPasswordRepromptSubmit`() {
        mutableStateFlow.value = createStateForAutofill(isRepromptRequired = true)
        composeTestRule
            .onNodeWithText(text = "mockName-1")
            .assertIsDisplayed()
            .performClick()
        composeTestRule
            .onNodeWithText("Autofill")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Master password")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performTextInput("password")
        composeTestRule
            .onAllNodesWithText(text = "Submit")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                SearchAction.MasterPasswordRepromptSubmit(
                    password = "password",
                    masterPasswordRepromptData = MasterPasswordRepromptData.Autofill(
                        cipherId = "mockId-1",
                    ),
                ),
            )
        }
        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking submit on the master password dialog for autofill-and-save should close the dialog and send MasterPasswordRepromptSubmit`() {
        mutableStateFlow.value = createStateForAutofill(isRepromptRequired = true)
        composeTestRule
            .onNodeWithText(text = "mockName-1")
            .assertIsDisplayed()
            .performClick()
        composeTestRule
            .onNodeWithText("Autofill and save")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Master password")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performTextInput("password")
        composeTestRule
            .onAllNodesWithText(text = "Submit")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                SearchAction.MasterPasswordRepromptSubmit(
                    password = "password",
                    masterPasswordRepromptData = MasterPasswordRepromptData.AutofillAndSave(
                        cipherId = "mockId-1",
                    ),
                ),
            )
        }
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `topBar search placeholder should be displayed according to state`() {
        mutableStateFlow.update { DEFAULT_STATE }
        composeTestRule.onNodeWithText(text = "Search vault").assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(searchType = SearchTypeData.Vault.SecureNotes)
        }
        composeTestRule.onNodeWithText(text = "Search Secure notes").assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(searchType = SearchTypeData.Vault.Cards)
        }
        composeTestRule.onNodeWithText(text = "Search Cards").assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(searchType = SearchTypeData.Vault.Identities)
        }
        composeTestRule.onNodeWithText(text = "Search Identities").assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(searchType = SearchTypeData.Vault.Trash)
        }
        composeTestRule.onNodeWithText(text = "Search Trash").assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(searchType = SearchTypeData.Vault.VerificationCodes)
        }
        composeTestRule.onNodeWithText(text = "Search Verification codes").assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(searchType = SearchTypeData.Vault.SshKeys)
        }
        composeTestRule.onNodeWithText(text = "Search SSH keys").assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(searchType = SearchTypeData.Vault.Logins)
        }
        composeTestRule.onNodeWithText(text = "Search Logins").assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(searchType = SearchTypeData.Vault.NoFolder)
        }
        composeTestRule.onNodeWithText(text = "Search No Folder").assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                searchType = SearchTypeData.Vault.Collection(
                    collectionId = "mockId",
                    collectionName = "mockName",
                ),
            )
        }
        composeTestRule.onNodeWithText(text = "Search mockName").assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                searchType = SearchTypeData.Vault.Folder(
                    folderId = "mockId",
                    folderName = "mockName",
                ),
            )
        }
        composeTestRule.onNodeWithText(text = "Search mockName").assertIsDisplayed()
    }

    @Test
    fun `on cipher item overflow click should display options dialog`() {
        val number = 1
        mutableStateFlow.update {
            it.copy(
                viewState = SearchState.ViewState.Content(
                    displayItems = listOf(createMockDisplayItemForCipher(number = number)),
                ),
            )
        }
        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onNodeWithContentDescription("Options")
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onAllNodesWithText("mockName-$number")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `on cipher item overflow option click should emit the appropriate action`() {
        mutableStateFlow.update {
            it.copy(
                viewState = SearchState.ViewState.Content(
                    displayItems = listOf(createMockDisplayItemForCipher(number = 1)),
                ),
            )
        }

        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onNodeWithContentDescription("Options")
            .assertIsDisplayed()
            .performClick()
        composeTestRule
            .onNodeWithText("View")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(
                SearchAction.OverflowOptionClick(
                    overflowAction = ListingItemOverflowAction.VaultAction.ViewClick(
                        cipherId = "mockId-1",
                        cipherType = CipherType.LOGIN,
                        requiresPasswordReprompt = true,
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Options")
            .assertIsDisplayed()
            .performClick()
        composeTestRule
            .onNodeWithText("Edit")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(
                SearchAction.OverflowOptionClick(
                    overflowAction = ListingItemOverflowAction.VaultAction.EditClick(
                        cipherId = "mockId-1",
                        cipherType = CipherType.LOGIN,
                        requiresPasswordReprompt = true,
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Options")
            .assertIsDisplayed()
            .performClick()
        composeTestRule
            .onNodeWithText("Copy username")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(
                SearchAction.OverflowOptionClick(
                    overflowAction = ListingItemOverflowAction.VaultAction.CopyUsernameClick(
                        username = "mockUsername-1",
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Options")
            .assertIsDisplayed()
            .performClick()
        composeTestRule
            .onNodeWithText("Copy password")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(
                SearchAction.OverflowOptionClick(
                    overflowAction = ListingItemOverflowAction.VaultAction.CopyPasswordClick(
                        requiresPasswordReprompt = true,
                        cipherId = "mockId-1",
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Options")
            .assertIsDisplayed()
            .performClick()
        composeTestRule
            .onNodeWithText("Launch")
            .assert(hasAnyAncestor(isDialog()))
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(
                SearchAction.OverflowOptionClick(
                    overflowAction = ListingItemOverflowAction.VaultAction.LaunchClick(
                        url = "www.mockuri1.com",
                    ),
                ),
            )
        }

        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on cipher item overflow item click when reprompt required should show the master password dialog`() {
        mutableStateFlow.update {
            it.copy(
                viewState = SearchState.ViewState.Content(
                    displayItems = listOf(
                        createMockDisplayItemForCipher(number = 1)
                            .copy(shouldDisplayMasterPasswordReprompt = true),
                    ),
                ),
            )
        }
        composeTestRule
            .onNodeWithContentDescription("Options")
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onNodeWithText("Edit")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()

        verify(exactly = 0) { viewModel.trySendAction(any()) }

        composeTestRule.assertMasterPasswordDialogDisplayed()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking submit on the master password dialog for overflow item should close the dialog and send MasterPasswordRepromptSubmit`() {
        mutableStateFlow.update {
            it.copy(
                viewState = SearchState.ViewState.Content(
                    displayItems = listOf(
                        createMockDisplayItemForCipher(number = 1)
                            .copy(shouldDisplayMasterPasswordReprompt = true),
                    ),
                ),
            )
        }
        composeTestRule
            .onNodeWithContentDescription("Options")
            .assertIsDisplayed()
            .performClick()
        composeTestRule
            .onNodeWithText("Edit")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Master password")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performTextInput("password")
        composeTestRule
            .onAllNodesWithText(text = "Submit")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                SearchAction.MasterPasswordRepromptSubmit(
                    password = "password",
                    masterPasswordRepromptData = MasterPasswordRepromptData.OverflowItem(
                        action = ListingItemOverflowAction.VaultAction.EditClick(
                            cipherId = "mockId-1",
                            cipherType = CipherType.LOGIN,
                            requiresPasswordReprompt = true,
                        ),
                    ),
                ),
            )
        }
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `on send item overflow click should display dialog`() {
        val number = 1
        mutableStateFlow.update {
            it.copy(
                viewState = SearchState.ViewState.Content(
                    displayItems = listOf(createMockDisplayItemForSend(number = number)),
                ),
            )
        }
        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onNodeWithContentDescription("Options")
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onAllNodesWithText("mockName-$number")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `on send item overflow option click should emit the appropriate action`() {
        mutableStateFlow.update {
            it.copy(
                viewState = SearchState.ViewState.Content(
                    displayItems = listOf(createMockDisplayItemForSend(number = 1)),
                ),
            )
        }

        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onNodeWithContentDescription(label = "Options")
            .assertIsDisplayed()
            .performClick()
        composeTestRule
            .onNodeWithText(text = "View")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(
                SearchAction.OverflowOptionClick(
                    overflowAction = ListingItemOverflowAction.SendAction.ViewClick(
                        sendId = "mockId-1",
                        sendType = SendType.FILE,
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Options")
            .assertIsDisplayed()
            .performClick()
        composeTestRule
            .onNodeWithText("Edit")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(
                SearchAction.OverflowOptionClick(
                    overflowAction = ListingItemOverflowAction.SendAction.EditClick(
                        sendId = "mockId-1",
                        sendType = SendType.FILE,
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Options")
            .assertIsDisplayed()
            .performClick()
        composeTestRule
            .onNodeWithText("Copy link")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(
                SearchAction.OverflowOptionClick(
                    overflowAction = ListingItemOverflowAction.SendAction.CopyUrlClick(
                        sendUrl = "https://vault.bitwarden.com/#/send/mockAccessId-1/mockKey-1",
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Options")
            .assertIsDisplayed()
            .performClick()
        composeTestRule
            .onNodeWithText("Share link")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(
                SearchAction.OverflowOptionClick(
                    overflowAction = ListingItemOverflowAction.SendAction.ShareUrlClick(
                        sendUrl = "https://vault.bitwarden.com/#/send/mockAccessId-1/mockKey-1",
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Options")
            .assertIsDisplayed()
            .performClick()
        composeTestRule
            .onNodeWithText("Remove password")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(
                SearchAction.OverflowOptionClick(
                    overflowAction = ListingItemOverflowAction.SendAction.RemovePasswordClick(
                        sendId = "mockId-1",
                    ),
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on send item delete overflow option click should display delete confirmation dialog and emits DeleteSendConfirmClick on confirmation`() {
        val sendId = "mockId-1"
        val message = "Are you sure you want to delete this Send?"
        mutableStateFlow.update {
            it.copy(
                viewState = SearchState.ViewState.Content(
                    displayItems = listOf(createMockDisplayItemForSend(number = 1)),
                ),
            )
        }
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText(message).assertDoesNotExist()

        composeTestRule
            .onNodeWithContentDescription("Options")
            .assertIsDisplayed()
            .performClick()
        composeTestRule
            .onNodeWithText("Delete")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule
            .onNodeWithText(message)
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))
        composeTestRule
            .onNodeWithText("Yes")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(
                SearchAction.OverflowOptionClick(
                    overflowAction = ListingItemOverflowAction.SendAction.DeleteClick(
                        sendId = sendId,
                    ),
                ),
            )
        }
    }

    @Test
    fun `error dialog should be displayed according to state`() {
        val errorMessage = "Fail"
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText(errorMessage).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState = SearchState.DialogState.Error(
                    title = null,
                    message = errorMessage.asText(),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(errorMessage)
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))
    }

    @Test
    fun `loading dialog should be displayed according to state`() {
        val loadingMessage = "syncing"
        composeTestRule.assertNoDialogExists()
        composeTestRule.onNodeWithText(loadingMessage).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(dialogState = SearchState.DialogState.Loading(loadingMessage.asText()))
        }

        composeTestRule
            .onNodeWithText(loadingMessage)
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))
    }
}

private val DEFAULT_STATE: SearchState = SearchState(
    searchTerm = "",
    searchType = SearchTypeData.Vault.All,
    viewState = SearchState.ViewState.Loading,
    dialogState = null,
    vaultFilterData = null,
    baseWebSendUrl = "www.test.com",
    baseIconUrl = "www.test.com",
    isIconLoadingDisabled = false,
    hasMasterPassword = true,
    totpData = null,
    autofillSelectionData = null,
    isPremium = true,
    restrictItemTypesPolicyOrgIds = persistentListOf(),
)

private fun createStateForAutofill(
    isRepromptRequired: Boolean = false,
): SearchState = DEFAULT_STATE
    .copy(
        viewState = SearchState.ViewState.Content(
            displayItems = listOf(
                createMockDisplayItemForCipher(number = 1)
                    .copy(
                        autofillSelectionOptions = AutofillSelectionOption.entries,
                        shouldDisplayMasterPasswordReprompt = isRepromptRequired,
                    ),
            ),
        ),
    )
