package com.x8bit.bitwarden.ui.vault.feature.itemlisting

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.baseIconUrl
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.util.isProgressBar
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VaultItemListingScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled = false
    private var onNavigateToVaultAddItemScreenCalled = false
    private var onNavigateToAddSendScreenCalled = false
    private var onNavigateToEditSendItemId: String? = null
    private var onNavigateToVaultItemId: String? = null

    private val mutableEventFlow = bufferedMutableSharedFlow<VaultItemListingEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<VaultItemListingViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            VaultItemListingScreen(
                viewModel = viewModel,
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToVaultItem = { onNavigateToVaultItemId = it },
                onNavigateToVaultAddItemScreen = { onNavigateToVaultAddItemScreenCalled = true },
                onNavigateToAddSendItem = { onNavigateToAddSendScreenCalled = true },
                onNavigateToEditSendItem = { onNavigateToEditSendItemId = it },
            )
        }
    }

    @Test
    fun `NavigateBack event should invoke NavigateBack`() {
        mutableEventFlow.tryEmit(VaultItemListingEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `clicking back button should send BackClick action`() {
        composeTestRule
            .onNodeWithContentDescription(label = "Back")
            .performClick()
        verify { viewModel.trySendAction(VaultItemListingsAction.BackClick) }
    }

    @Test
    fun `search icon click should send SearchIconClick action`() {
        composeTestRule
            .onNodeWithContentDescription("Search vault")
            .performClick()
        verify { viewModel.trySendAction(VaultItemListingsAction.SearchIconClick) }
    }

    @Test
    fun `floating action button click should send AddItemClick action`() {
        composeTestRule
            .onNodeWithContentDescription("Add item")
            .performClick()
        verify { viewModel.trySendAction(VaultItemListingsAction.AddVaultItemClick) }
    }

    @Test
    fun `add an item button click should send AddItemClick action`() {
        mutableStateFlow.update { it.copy(viewState = VaultItemListingState.ViewState.NoItems) }
        composeTestRule
            .onNodeWithText("Add an Item")
            .performClick()
        verify { viewModel.trySendAction(VaultItemListingsAction.AddVaultItemClick) }
    }

    @Test
    fun `refresh button click should send RefreshClick action`() {
        mutableStateFlow.update {
            it.copy(viewState = VaultItemListingState.ViewState.Error(message = "".asText()))
        }
        composeTestRule
            .onNodeWithText("Try again")
            .performClick()
        verify { viewModel.trySendAction(VaultItemListingsAction.RefreshClick) }
    }

    @Test
    fun `NavigateToAdd VaultItem event should call NavigateToVaultAddItemScreen`() {
        mutableEventFlow.tryEmit(VaultItemListingEvent.NavigateToAddVaultItem)
        assertTrue(onNavigateToVaultAddItemScreenCalled)
    }

    @Test
    fun `NavigateToAddSendItem should call onNavigateToAddSendScreen`() {
        mutableEventFlow.tryEmit(VaultItemListingEvent.NavigateToAddSendItem)
        assertTrue(onNavigateToAddSendScreenCalled)
    }

    @Test
    fun `NavigateToSendItem event should call onNavigateToEditSendItemId`() {
        val sendId = "sendId"
        mutableEventFlow.tryEmit(VaultItemListingEvent.NavigateToSendItem(sendId))
        assertEquals(sendId, onNavigateToEditSendItemId)
    }

    @Test
    fun `NavigateToVaultItem event should call NavigateToVaultItemScreen`() {
        val id = "id4321"
        mutableEventFlow.tryEmit(VaultItemListingEvent.NavigateToVaultItem(id = id))
        assertEquals(id, onNavigateToVaultItemId)
    }

    @Test
    fun `progressbar should be displayed according to state`() {
        mutableStateFlow.update { DEFAULT_STATE }

        composeTestRule
            .onNode(isProgressBar)
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(viewState = VaultItemListingState.ViewState.NoItems)
        }

        composeTestRule
            .onNode(isProgressBar)
            .assertDoesNotExist()
    }

    @Test
    fun `error text and retry should be displayed according to state`() {
        val message = "error_message"
        mutableStateFlow.update { DEFAULT_STATE }
        composeTestRule
            .onNodeWithText(message)
            .assertIsNotDisplayed()

        mutableStateFlow.update { it.copy(viewState = VaultItemListingState.ViewState.NoItems) }
        composeTestRule
            .onNodeWithText(message)
            .assertIsNotDisplayed()

        mutableStateFlow.update {
            it.copy(viewState = VaultItemListingState.ViewState.Error(message.asText()))
        }
        composeTestRule
            .onNodeWithText(message)
            .assertIsDisplayed()
    }

    @Test
    fun `Add an item button should be displayed according to state`() {
        mutableStateFlow.update { DEFAULT_STATE }
        composeTestRule
            .onNodeWithText(text = "Add an Item")
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(viewState = VaultItemListingState.ViewState.NoItems)
        }
        composeTestRule
            .onNodeWithText(text = "Add an Item")
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(itemListingType = VaultItemListingState.ItemListingType.Vault.Trash)
        }
        composeTestRule
            .onNodeWithText(text = "Add an Item")
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                itemListingType = VaultItemListingState.ItemListingType.Vault.Folder(
                    folderId = null,
                ),
            )
        }
        composeTestRule
            .onNodeWithText(text = "Add an Item")
            .assertDoesNotExist()
    }

    @Test
    fun `empty text should be displayed according to state`() {
        mutableStateFlow.update {
            DEFAULT_STATE.copy(viewState = VaultItemListingState.ViewState.NoItems)
        }
        composeTestRule
            .onNodeWithText(text = "There are no items in your vault.")
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(itemListingType = VaultItemListingState.ItemListingType.Vault.Trash)
        }
        composeTestRule
            .onNodeWithText(text = "There are no items in the trash.")
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                itemListingType = VaultItemListingState.ItemListingType.Vault.Folder(
                    folderId = null,
                ),
            )
        }
        composeTestRule
            .onNodeWithText(text = "There are no items in this folder.")
            .assertIsDisplayed()
    }

    @Test
    fun `floating action button should be displayed according to state`() {
        mutableStateFlow.update { DEFAULT_STATE }

        composeTestRule
            .onNodeWithContentDescription("Add item")
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(itemListingType = VaultItemListingState.ItemListingType.Vault.Trash)
        }

        composeTestRule
            .onNodeWithContentDescription("Add item")
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                itemListingType = VaultItemListingState.ItemListingType.Vault.Folder(
                    folderId = null,
                ),
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Add item")
            .assertDoesNotExist()
    }

    @Test
    fun `Items text should be displayed according to state`() {
        val items = "Items"
        mutableStateFlow.update { DEFAULT_STATE }
        composeTestRule
            .onNodeWithText(text = items)
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                viewState = VaultItemListingState.ViewState.Content(
                    displayItemList = listOf(
                        createDisplayItem(number = 1),
                    ),
                ),
            )
        }
        composeTestRule
            .onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText(items))
        composeTestRule
            .onNodeWithText(text = items)
            .assertIsDisplayed()
    }

    @Test
    fun `Items text count should be displayed according to state`() {
        val items = "Items"
        mutableStateFlow.update { DEFAULT_STATE }
        composeTestRule
            .onNodeWithText(text = items)
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                viewState = VaultItemListingState.ViewState.Content(
                    displayItemList = listOf(
                        createDisplayItem(number = 1),
                    ),
                ),
            )
        }
        composeTestRule
            .onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText(items))
        composeTestRule
            .onNodeWithText(text = items)
            .assertIsDisplayed()
            .assertTextEquals(items, 1.toString())

        mutableStateFlow.update {
            it.copy(
                viewState = VaultItemListingState.ViewState.Content(
                    displayItemList = listOf(
                        createDisplayItem(number = 1),
                        createDisplayItem(number = 2),
                        createDisplayItem(number = 3),
                        createDisplayItem(number = 4),
                    ),
                ),
            )
        }

        composeTestRule
            .onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText(items))
        composeTestRule
            .onNodeWithText(text = items)
            .assertIsDisplayed()
            .assertTextEquals(items, 4.toString())
    }

    @Test
    fun `displayItems should be displayed according to state`() {
        mutableStateFlow.update {
            it.copy(
                viewState = VaultItemListingState.ViewState.Content(
                    displayItemList = listOf(
                        createDisplayItem(number = 1),
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(text = "mockTitle-1")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = "mockSubtitle-1")
            .assertIsDisplayed()
    }

    @Test
    fun `clicking on a display item should send ItemClick action`() {
        mutableStateFlow.update {
            it.copy(
                viewState = VaultItemListingState.ViewState.Content(
                    displayItemList = listOf(
                        createDisplayItem(number = 1),
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(text = "mockTitle-1")
            .assertIsDisplayed()
            .performClick()
        verify {
            viewModel.trySendAction(VaultItemListingsAction.ItemClick("mockId-1"))
        }
    }

    @Test
    fun `topBar title should be displayed according to state`() {
        mutableStateFlow.update { DEFAULT_STATE }
        composeTestRule
            .onNodeWithText(text = "Logins")
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(itemListingType = VaultItemListingState.ItemListingType.Vault.SecureNote)
        }
        composeTestRule
            .onNodeWithText(text = "Secure notes")
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(itemListingType = VaultItemListingState.ItemListingType.Vault.Card)
        }
        composeTestRule
            .onNodeWithText(text = "Cards")
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(itemListingType = VaultItemListingState.ItemListingType.Vault.Identity)
        }
        composeTestRule
            .onNodeWithText(text = "Identities")
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(itemListingType = VaultItemListingState.ItemListingType.Vault.Trash)
        }
        composeTestRule
            .onNodeWithText(text = "Trash")
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                itemListingType = VaultItemListingState.ItemListingType.Vault.Folder(
                    folderId = "mockId",
                    folderName = "mockName",
                ),
            )
        }
        composeTestRule
            .onNodeWithText(text = "mockName")
            .assertIsDisplayed()
    }
}

private val DEFAULT_STATE = VaultItemListingState(
    itemListingType = VaultItemListingState.ItemListingType.Vault.Login,
    viewState = VaultItemListingState.ViewState.Loading,
    isIconLoadingDisabled = false,
    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
)

private fun createDisplayItem(number: Int): VaultItemListingState.DisplayItem =
    VaultItemListingState.DisplayItem(
        id = "mockId-$number",
        title = "mockTitle-$number",
        subtitle = "mockSubtitle-$number",
        iconData = IconData.Local(R.drawable.ic_card_item),
    )
