package com.x8bit.bitwarden.ui.vault.feature.vault

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VaultScreenTest : BaseComposeTest() {

    private var onNavigateToVaultAddItemScreenCalled = false
    private var onNavigateToVaultItemId: String? = null
    private var onNavigateToVaultEditItemId: String? = null
    private var onNavigateToVaultItemListingType: VaultItemListingType? = null
    private var onDimBottomNavBarRequestCalled = false

    private val mutableEventFlow = MutableSharedFlow<VaultEvent>(
        extraBufferCapacity = Int.MAX_VALUE,
    )
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<VaultViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            VaultScreen(
                viewModel = viewModel,
                onNavigateToVaultAddItemScreen = { onNavigateToVaultAddItemScreenCalled = true },
                onNavigateToVaultItemScreen = { onNavigateToVaultItemId = it },
                onNavigateToVaultEditItemScreen = { onNavigateToVaultEditItemId = it },
                onNavigateToVaultItemListingScreen = { onNavigateToVaultItemListingType = it },
                onDimBottomNavBarRequest = { onDimBottomNavBarRequestCalled = true },
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `account icon click should show the account switcher and trigger the nav bar dim request`() {
        composeTestRule.onNodeWithText("active@bitwarden.com").assertDoesNotExist()
        composeTestRule.onNodeWithText("locked@bitwarden.com").assertDoesNotExist()
        composeTestRule.onNodeWithText("Add account").assertDoesNotExist()
        assertFalse(onDimBottomNavBarRequestCalled)

        composeTestRule.onNodeWithText("AU").performClick()

        composeTestRule.onNodeWithText("active@bitwarden.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("locked@bitwarden.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add account").assertIsDisplayed()
        assertTrue(onDimBottomNavBarRequestCalled)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `account click in the account switcher should send AccountSwitchClick and close switcher`() {
        // Open the Account Switcher
        composeTestRule.onNodeWithText("AU").performClick()

        composeTestRule.onNodeWithText("locked@bitwarden.com").performClick()
        verify { viewModel.trySendAction(VaultAction.AccountSwitchClick(LOCKED_ACCOUNT_SUMMARY)) }
        composeTestRule.onNodeWithText("locked@bitwarden.com").assertDoesNotExist()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `Add Account click in the account switcher should send AddAccountClick and close switcher`() {
        // Open the Account Switcher
        composeTestRule.onNodeWithText("AU").performClick()

        composeTestRule.onNodeWithText("Add account").performClick()
        verify { viewModel.trySendAction(VaultAction.AddAccountClick) }
        composeTestRule.onNodeWithText("Add account").assertDoesNotExist()
    }

    @Test
    fun `search icon click should send SearchIconClick action`() {
        mutableStateFlow.update { it.copy(viewState = VaultState.ViewState.NoItems) }
        composeTestRule.onNodeWithContentDescription("Search vault").performClick()
        verify { viewModel.trySendAction(VaultAction.SearchIconClick) }
    }

    @Test
    fun `floating action button click should send AddItemClick action`() {
        mutableStateFlow.update { it.copy(viewState = VaultState.ViewState.NoItems) }
        composeTestRule.onNodeWithContentDescription("Add item").performClick()
        verify { viewModel.trySendAction(VaultAction.AddItemClick) }
    }

    @Test
    fun `add an item button click should send AddItemClick action`() {
        mutableStateFlow.update { it.copy(viewState = VaultState.ViewState.NoItems) }
        composeTestRule.onNodeWithText("Add an Item").performClick()
        verify { viewModel.trySendAction(VaultAction.AddItemClick) }
    }

    @Test
    fun `NavigateToAddItemScreen event should call onNavigateToVaultAddItemScreen`() {
        mutableEventFlow.tryEmit(VaultEvent.NavigateToAddItemScreen)
        assertTrue(onNavigateToVaultAddItemScreenCalled)
    }

    @Test
    fun `NavigateToVaultItem event should call onNavigateToVaultItemScreen`() {
        val id = "id4321"
        mutableEventFlow.tryEmit(VaultEvent.NavigateToVaultItem(itemId = id))
        assertEquals(id, onNavigateToVaultItemId)
    }

    @Test
    fun `NavigateToEditVaultItem event should call onNavigateToVaultEditItemScreen`() {
        val id = "id1234"
        mutableEventFlow.tryEmit(VaultEvent.NavigateToEditVaultItem(itemId = id))
        assertEquals(id, onNavigateToVaultEditItemId)
    }

    @Test
    fun `NavigateNavigateToCardGroup event should call onNavigateToVaultItemListingType`() {
        mutableEventFlow.tryEmit(VaultEvent.NavigateToItemListing(VaultItemListingType.Card))
        assertEquals(VaultItemListingType.Card, onNavigateToVaultItemListingType)
    }

    @Test
    fun `NavigateToIdentityGroup event should call onNavigateToVaultItemListingType`() {
        mutableEventFlow.tryEmit(VaultEvent.NavigateToItemListing(VaultItemListingType.Identity))
        assertEquals(VaultItemListingType.Identity, onNavigateToVaultItemListingType)
    }

    @Test
    fun `NavigateToLoginGroup event should call onNavigateToVaultItemListingType`() {
        mutableEventFlow.tryEmit(VaultEvent.NavigateToItemListing(VaultItemListingType.Login))
        assertEquals(VaultItemListingType.Login, onNavigateToVaultItemListingType)
    }

    @Test
    fun `NavigateToSecureNotesGroup event should call onNavigateToVaultItemListingType`() {
        mutableEventFlow.tryEmit(VaultEvent.NavigateToItemListing(VaultItemListingType.SecureNote))
        assertEquals(VaultItemListingType.SecureNote, onNavigateToVaultItemListingType)
    }

    @Test
    fun `NavigateToTrash event should call onNavigateToVaultItemListingType`() {
        mutableEventFlow.tryEmit(VaultEvent.NavigateToItemListing(VaultItemListingType.Trash))
        assertEquals(VaultItemListingType.Trash, onNavigateToVaultItemListingType)
    }

    @Test
    fun `NavigateToFolder event should call onNavigateToVaultItemListingType`() {
        val mockFolderId = "mockFolderId"
        mutableEventFlow.tryEmit(
            VaultEvent.NavigateToItemListing(VaultItemListingType.Folder(mockFolderId)),
        )
        assertEquals(VaultItemListingType.Folder(mockFolderId), onNavigateToVaultItemListingType)
    }

    @Test
    fun `clicking a favorite item should send VaultItemClick with the correct item`() {
        val itemText = "Test Item"
        val username = "BitWarden"
        val vaultItem = VaultState.ViewState.VaultItem.Login(
            id = "12345",
            name = itemText.asText(),
            username = username.asText(),
        )
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                    favoriteItems = listOf(vaultItem),
                ),
            )
        }

        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(itemText))
        // Header
        composeTestRule
            .onNodeWithText("Favorites")
            .assertTextEquals("Favorites", 1.toString())
        // Item
        composeTestRule
            .onNodeWithText(itemText)
            .assertTextEquals(itemText, username)
            .performClick()
        verify {
            viewModel.trySendAction(VaultAction.VaultItemClick(vaultItem))
        }
    }

    @Test
    fun `clicking a folder item should send FolderClick with the correct item`() {
        val folderText = "Test Folder"
        val count = 3
        val folderItem = VaultState.ViewState.FolderItem(
            id = "12345",
            name = folderText.asText(),
            itemCount = count,
        )

        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                    folderItems = listOf(folderItem),
                ),
            )
        }

        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(folderText))
        composeTestRule
            .onNodeWithText(folderText)
            .assertTextEquals(folderText, count.toString())
            .performClick()
        verify {
            viewModel.trySendAction(VaultAction.FolderClick(folderItem))
        }
    }

    @Test
    fun `clicking a no folder item should send VaultItemClick with the correct item`() {
        val itemText = "Test Item"
        val userName = "BitWarden"
        val vaultItem = VaultState.ViewState.VaultItem.Login(
            id = "12345",
            name = itemText.asText(),
            username = userName.asText(),
        )
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                    noFolderItems = listOf(vaultItem),
                ),
            )
        }

        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(itemText))
        composeTestRule
            .onNodeWithText(itemText)
            .assertTextEquals(itemText, userName)
            .performClick()
        verify {
            viewModel.trySendAction(VaultAction.VaultItemClick(vaultItem))
        }
    }

    @Test
    fun `login item count should update according to state`() {
        val rowText = "Login"
        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT_VIEW_STATE)
        }
        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(rowText))
        composeTestRule.onNodeWithText(rowText).assertTextEquals(rowText, 0.toString())

        val count = 45
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                    loginItemsCount = count,
                ),
            )
        }

        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(rowText))
        composeTestRule.onNodeWithText(rowText).assertTextEquals(rowText, count.toString())
    }

    @Test
    fun `clicking a login item should send LoginGroupClick action`() {
        val rowText = "Login"
        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT_VIEW_STATE)
        }

        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(rowText))
        composeTestRule.onNodeWithText(rowText).performClick()
        verify {
            viewModel.trySendAction(VaultAction.LoginGroupClick)
        }
    }

    @Test
    fun `card item count should update according to state`() {
        val rowText = "Card"
        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT_VIEW_STATE)
        }
        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(rowText))
        composeTestRule.onNodeWithText(rowText).assertTextEquals(rowText, 0.toString())

        val count = 3
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                    cardItemsCount = count,
                ),
            )
        }

        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(rowText))
        composeTestRule.onNodeWithText(rowText).assertTextEquals(rowText, count.toString())
    }

    @Test
    fun `clicking a card item should send CardGroupClick action`() {
        val rowText = "Card"
        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT_VIEW_STATE)
        }

        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(rowText))
        composeTestRule.onNodeWithText(rowText).performClick()
        verify {
            viewModel.trySendAction(VaultAction.CardGroupClick)
        }
    }

    @Test
    fun `identity item count should update according to state`() {
        val rowText = "Identity"
        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT_VIEW_STATE)
        }
        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(rowText))
        composeTestRule.onNodeWithText(rowText).assertTextEquals(rowText, 0.toString())

        val count = 14
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                    identityItemsCount = count,
                ),
            )
        }

        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(rowText))
        composeTestRule.onNodeWithText(rowText).assertTextEquals(rowText, count.toString())
    }

    @Test
    fun `clicking an identity item should send IdentityGroupClick action`() {
        val rowText = "Identity"
        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT_VIEW_STATE)
        }

        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(rowText))
        composeTestRule.onNodeWithText(rowText).performClick()
        verify {
            viewModel.trySendAction(VaultAction.IdentityGroupClick)
        }
    }

    @Test
    fun `secure note item count should update according to state`() {
        val rowText = "Secure note"
        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT_VIEW_STATE)
        }
        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(rowText))
        composeTestRule.onNodeWithText(rowText).assertTextEquals(rowText, 0.toString())

        val count = 7
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                    secureNoteItemsCount = count,
                ),
            )
        }

        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(rowText))
        composeTestRule.onNodeWithText(rowText).assertTextEquals(rowText, count.toString())
    }

    @Test
    fun `clicking a secure note item should send SecureNoteGroupClick action`() {
        val rowText = "Secure note"
        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT_VIEW_STATE)
        }

        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(rowText))
        composeTestRule.onNodeWithText(rowText).performClick()
        verify {
            viewModel.trySendAction(VaultAction.SecureNoteGroupClick)
        }
    }

    @Test
    fun `trash count should update according to state`() {
        val rowText = "Trash"
        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT_VIEW_STATE)
        }
        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(rowText))
        // Header
        composeTestRule
            .onAllNodes(hasText(rowText))
            .filterToOne(!hasClickAction())
            .assertTextEquals(rowText, 1.toString())
        // Item
        composeTestRule
            .onAllNodes(hasText(rowText))
            .filterToOne(hasClickAction())
            .assertTextEquals(rowText, 0.toString())

        val trashCount = 5
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                    trashItemsCount = 5,
                ),
            )
        }

        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(rowText))
        // Header
        composeTestRule
            .onAllNodes(hasText(rowText))
            .filterToOne(!hasClickAction())
            .assertTextEquals(rowText, 1.toString())
        // Item
        composeTestRule
            .onAllNodes(hasText(rowText))
            .filterToOne(hasClickAction())
            .assertTextEquals(rowText, trashCount.toString())
    }

    @Test
    fun `clicking trash item should send TrashClick action`() {
        val rowText = "Trash"
        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT_VIEW_STATE)
        }

        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(rowText))
        composeTestRule.onAllNodes(hasText(rowText)).filterToOne(hasClickAction()).performClick()
        verify {
            viewModel.trySendAction(VaultAction.TrashClick)
        }
    }
}

private val ACTIVE_ACCOUNT_SUMMARY = AccountSummary(
    userId = "activeUserId",
    name = "Active User",
    email = "active@bitwarden.com",
    avatarColorHex = "#aa00aa",
    status = AccountSummary.Status.ACTIVE,
)

private val LOCKED_ACCOUNT_SUMMARY = AccountSummary(
    userId = "lockedUserId",
    name = "Locked User",
    email = "locked@bitwarden.com",
    avatarColorHex = "#00aaaa",
    status = AccountSummary.Status.LOCKED,
)

private val DEFAULT_STATE: VaultState = VaultState(
    avatarColorString = "#aa00aa",
    initials = "AU",
    accountSummaries = persistentListOf(
        ACTIVE_ACCOUNT_SUMMARY,
        LOCKED_ACCOUNT_SUMMARY,
    ),
    viewState = VaultState.ViewState.Loading,
)

private val DEFAULT_CONTENT_VIEW_STATE: VaultState.ViewState.Content = VaultState.ViewState.Content(
    loginItemsCount = 0,
    cardItemsCount = 0,
    identityItemsCount = 0,
    secureNoteItemsCount = 0,
    favoriteItems = emptyList(),
    folderItems = emptyList(),
    noFolderItems = emptyList(),
    trashItemsCount = 0,
)
