package com.x8bit.bitwarden.ui.platform.feature.search

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.core.net.toUri
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.feature.search.util.createMockDisplayItemForCipher
import com.x8bit.bitwarden.ui.platform.feature.search.util.createMockDisplayItemForSend
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.util.assertNoDialogExists
import com.x8bit.bitwarden.ui.util.isProgressBar
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SearchScreenTest : BaseComposeTest() {
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

    private var onNavigateBackCalled = false
    private var onNavigateToEditSendId: String? = null
    private var onNavigateToEditCipherId: String? = null
    private var onNavigateToViewCipherId: String? = null

    @Before
    fun setup() {
        composeTestRule.setContent {
            SearchScreen(
                viewModel = viewModel,
                intentManager = intentManager,
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToEditSend = { onNavigateToEditSendId = it },
                onNavigateToEditCipher = { onNavigateToEditCipherId = it },
                onNavigateToViewCipher = { onNavigateToViewCipherId = it },
            )
        }
    }

    @Test
    fun `NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(SearchEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `NavigateToEditSend should call onNavigateToEditSend`() {
        val sendId = "sendId"
        mutableEventFlow.tryEmit(SearchEvent.NavigateToEditSend(sendId))
        assertEquals(sendId, onNavigateToEditSendId)
    }

    @Test
    fun `NavigateToEditCipher should call onNavigateToEditCipher`() {
        val cipherId = "cipherId"
        mutableEventFlow.tryEmit(SearchEvent.NavigateToEditCipher(cipherId))
        assertEquals(cipherId, onNavigateToEditCipherId)
    }

    @Test
    fun `NavigateToViewCipher should call onNavigateToViewCipher`() {
        val cipherId = "cipherId"
        mutableEventFlow.tryEmit(SearchEvent.NavigateToViewCipher(cipherId))
        assertEquals(cipherId, onNavigateToViewCipherId)
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
            viewModel.trySendAction(SearchAction.ItemClick("mockId-1"))
        }
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
            .onNode(isDialog())
            .onChildren()
            .filterToOne(hasText("mockName-$number"))
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
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
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
)
