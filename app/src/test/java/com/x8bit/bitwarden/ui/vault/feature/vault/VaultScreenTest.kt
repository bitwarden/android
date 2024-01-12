package com.x8bit.bitwarden.ui.vault.feature.vault

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.IntentHandler
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary
import com.x8bit.bitwarden.ui.util.assertLockOrLogoutDialogIsDisplayed
import com.x8bit.bitwarden.ui.util.assertLogoutConfirmationDialogIsDisplayed
import com.x8bit.bitwarden.ui.util.assertNoDialogExists
import com.x8bit.bitwarden.ui.util.assertScrollableNodeDoesNotExist
import com.x8bit.bitwarden.ui.util.assertSwitcherIsDisplayed
import com.x8bit.bitwarden.ui.util.assertSwitcherIsNotDisplayed
import com.x8bit.bitwarden.ui.util.onNodeWithTextAfterScroll
import com.x8bit.bitwarden.ui.util.performAccountClick
import com.x8bit.bitwarden.ui.util.performAccountIconClick
import com.x8bit.bitwarden.ui.util.performAccountLongClick
import com.x8bit.bitwarden.ui.util.performAddAccountClick
import com.x8bit.bitwarden.ui.util.performLockAccountClick
import com.x8bit.bitwarden.ui.util.performLogoutAccountClick
import com.x8bit.bitwarden.ui.util.performLogoutAccountConfirmationClick
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterData
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@Suppress("LargeClass")
class VaultScreenTest : BaseComposeTest() {

    private var onNavigateToVaultAddItemScreenCalled = false
    private var onNavigateToVaultItemId: String? = null
    private var onNavigateToVaultEditItemId: String? = null
    private var onNavigateToVaultItemListingType: VaultItemListingType? = null
    private var onDimBottomNavBarRequestCalled = false
    private val intentHandler = mockk<IntentHandler>(relaxed = true)

    private val mutableEventFlow = bufferedMutableSharedFlow<VaultEvent>()
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
                intentHandler = intentHandler,
            )
        }
    }

    @Test
    fun `app bar title should update according to state`() {
        composeTestRule.onNodeWithText("My vault").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vaults").assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(appBarTitle = R.string.vaults.asText())
        }

        composeTestRule.onNodeWithText("My vault").assertDoesNotExist()
        composeTestRule.onNodeWithText("Vaults").assertIsDisplayed()
    }

    @Test
    fun `vault filter should update according to state`() {
        composeTestRule.onNodeWithText("Vault: All").assertDoesNotExist()
        composeTestRule.onNodeWithText("Vault: My vault").assertDoesNotExist()
        composeTestRule.onNodeWithText("Vault: Test Organization").assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                vaultFilterData = VAULT_FILTER_DATA,
                viewState = DEFAULT_CONTENT_VIEW_STATE,
            )
        }

        composeTestRule.onNodeWithText("Vault: All").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vault: My vault").assertDoesNotExist()
        composeTestRule.onNodeWithText("Vault: Test Organization").assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                vaultFilterData = VAULT_FILTER_DATA.copy(
                    selectedVaultFilterType = VaultFilterType.MyVault,
                ),
            )
        }

        composeTestRule.onNodeWithText("Vault: All").assertDoesNotExist()
        composeTestRule.onNodeWithText("Vault: My vault").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vault: Test Organization").assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                vaultFilterData = VAULT_FILTER_DATA.copy(
                    selectedVaultFilterType = ORGANIZATION_VAULT_FILTER,
                ),
            )
        }

        composeTestRule.onNodeWithText("Vault: All").assertDoesNotExist()
        composeTestRule.onNodeWithText("Vault: My vault").assertDoesNotExist()
        composeTestRule.onNodeWithText("Vault: Test Organization").assertIsDisplayed()
    }

    @Test
    fun `vault filter menu click should display the filter selection dialog`() {
        // Display the vault filter
        mutableStateFlow.update {
            it.copy(
                vaultFilterData = VAULT_FILTER_DATA,
                viewState = DEFAULT_CONTENT_VIEW_STATE,
            )
        }

        composeTestRule.assertNoDialogExists()

        composeTestRule.onNodeWithContentDescription("Filter items by vault").performClick()

        composeTestRule
            .onAllNodesWithText("All vaults")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("My vault")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Test Organization")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `cancel click in the filter selection dialog should close the dialog`() {
        // Display the vault selection dialog
        mutableStateFlow.update {
            it.copy(
                vaultFilterData = VAULT_FILTER_DATA,
                viewState = DEFAULT_CONTENT_VIEW_STATE,
            )
        }
        composeTestRule.onNodeWithContentDescription("Filter items by vault").performClick()

        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `vault filter click in the filter selection dialog should send VaultFilterTypeSelect and close the dialog`() {
        // Display the vault selection dialog
        mutableStateFlow.update {
            it.copy(
                vaultFilterData = VAULT_FILTER_DATA,
                viewState = DEFAULT_CONTENT_VIEW_STATE,
            )
        }
        composeTestRule.onNodeWithContentDescription("Filter items by vault").performClick()

        composeTestRule
            .onAllNodesWithText("All vaults")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(VaultAction.VaultFilterTypeSelect(VaultFilterType.AllVaults))
        }
        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `account icon click should show the account switcher and trigger the nav bar dim request`() {
        val accountSummaries = DEFAULT_STATE.accountSummaries
        composeTestRule.assertSwitcherIsNotDisplayed(
            accountSummaries = accountSummaries,
        )
        assertFalse(onDimBottomNavBarRequestCalled)

        composeTestRule.performAccountIconClick()

        composeTestRule.assertSwitcherIsDisplayed(
            accountSummaries = accountSummaries,
        )
        assertTrue(onDimBottomNavBarRequestCalled)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `account click in the account switcher should send SwitchAccountClick and close switcher`() {
        // Open the Account Switcher
        val accountSummaries = DEFAULT_STATE.accountSummaries
        composeTestRule.performAccountIconClick()

        composeTestRule.performAccountClick(accountSummary = LOCKED_ACCOUNT_SUMMARY)

        verify { viewModel.trySendAction(VaultAction.SwitchAccountClick(LOCKED_ACCOUNT_SUMMARY)) }
        composeTestRule.assertSwitcherIsNotDisplayed(
            accountSummaries = accountSummaries,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `Add Account click in the account switcher should send AddAccountClick and close switcher`() {
        // Open the Account Switcher
        val accountSummaries = DEFAULT_STATE.accountSummaries
        composeTestRule.performAccountIconClick()

        composeTestRule.performAddAccountClick()

        verify { viewModel.trySendAction(VaultAction.AddAccountClick) }
        composeTestRule.assertSwitcherIsNotDisplayed(
            accountSummaries = accountSummaries,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `account long click in the account switcher should show the lock-or-logout dialog and close the switcher`() {
        // Show the account switcher
        composeTestRule.performAccountIconClick()
        composeTestRule.assertNoDialogExists()

        composeTestRule.performAccountLongClick(
            accountSummary = ACTIVE_ACCOUNT_SUMMARY,
        )

        composeTestRule.assertLockOrLogoutDialogIsDisplayed(
            accountSummary = ACTIVE_ACCOUNT_SUMMARY,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `lock button click in the lock-or-logout dialog should send LockAccountClick action and close the dialog`() {
        // Show the lock-or-logout dialog
        composeTestRule.performAccountIconClick()
        composeTestRule.performAccountLongClick(ACTIVE_ACCOUNT_SUMMARY)

        composeTestRule.performLockAccountClick()

        verify { viewModel.trySendAction(VaultAction.LockAccountClick(ACTIVE_ACCOUNT_SUMMARY)) }
        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `logout button click in the lock-or-logout dialog should show the logout confirmation dialog and hide the lock-or-logout dialog`() {
        // Show the lock-or-logout dialog
        composeTestRule.performAccountIconClick()
        composeTestRule.performAccountLongClick(ACTIVE_ACCOUNT_SUMMARY)

        composeTestRule.performLogoutAccountClick()

        composeTestRule.assertLogoutConfirmationDialogIsDisplayed(
            accountSummary = ACTIVE_ACCOUNT_SUMMARY,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `logout button click in the logout confirmation dialog should send LogoutAccountClick action and close the dialog`() {
        // Show the logout confirmation dialog
        composeTestRule.performAccountIconClick()
        composeTestRule.performAccountLongClick(ACTIVE_ACCOUNT_SUMMARY)
        composeTestRule.performLogoutAccountClick()

        composeTestRule.performLogoutAccountConfirmationClick()

        verify { viewModel.trySendAction(VaultAction.LogoutAccountClick(ACTIVE_ACCOUNT_SUMMARY)) }
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `overflow button click should show the overflow menu`() {
        composeTestRule.onNode(isPopup()).assertDoesNotExist()
        composeTestRule.onNodeWithText("Sync").assertDoesNotExist()
        composeTestRule.onNodeWithText("Lock").assertDoesNotExist()
        composeTestRule.onNodeWithText("Exit").assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription("More").performClick()

        composeTestRule.onNode(isPopup()).assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Sync")
            .filterToOne(hasAnyAncestor(isPopup()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Lock")
            .filterToOne(hasAnyAncestor(isPopup()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Exit")
            .filterToOne(hasAnyAncestor(isPopup()))
            .assertIsDisplayed()
    }

    @Test
    fun `sync click in the overflow menu should send SyncClick`() {
        // Expand the overflow menu
        composeTestRule.onNodeWithContentDescription("More").performClick()

        composeTestRule
            .onAllNodesWithText("Sync")
            .filterToOne(hasAnyAncestor(isPopup()))
            .performClick()

        verify { viewModel.trySendAction(VaultAction.SyncClick) }
    }

    @Test
    fun `lock click in the overflow menu should send LockClick`() {
        // Expand the overflow menu
        composeTestRule.onNodeWithContentDescription("More").performClick()

        composeTestRule
            .onAllNodesWithText("Lock")
            .filterToOne(hasAnyAncestor(isPopup()))
            .performClick()

        verify { viewModel.trySendAction(VaultAction.LockClick) }
    }

    @Test
    fun `exit click in the overflow menu should show a confirmation dialog`() {
        // Expand the overflow menu
        composeTestRule.onNodeWithContentDescription("More").performClick()

        composeTestRule
            .onAllNodesWithText("Exit")
            .filterToOne(hasAnyAncestor(isPopup()))
            .performClick()

        composeTestRule
            .onNode(isDialog())
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Exit")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Are you sure you want to exit Bitwarden?")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Yes")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `yes click in exit confirmation dialog should send ExitConfirmationClick`() {
        // Expand the overflow menu and show the exit confirmation dialog
        composeTestRule.onNodeWithContentDescription("More").performClick()
        composeTestRule
            .onAllNodesWithText("Exit")
            .filterToOne(hasAnyAncestor(isPopup()))
            .performClick()

        composeTestRule
            .onAllNodesWithText("Yes")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule.assertNoDialogExists()
        verify { viewModel.trySendAction(VaultAction.ExitConfirmationClick) }
    }

    @Test
    fun `floating action button should be shown or hidden according to the state`() {
        val fabDescription = "Add item"

        mutableStateFlow.update { it.copy(viewState = VaultState.ViewState.Loading) }
        composeTestRule.onNodeWithContentDescription(fabDescription).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(viewState = VaultState.ViewState.Error("Error".asText()))
        }
        composeTestRule.onNodeWithContentDescription(fabDescription).assertDoesNotExist()

        mutableStateFlow.update { it.copy(viewState = VaultState.ViewState.NoItems) }
        composeTestRule.onNodeWithContentDescription(fabDescription).assertIsDisplayed()

        mutableStateFlow.update { it.copy(viewState = DEFAULT_CONTENT_VIEW_STATE) }
        composeTestRule.onNodeWithContentDescription(fabDescription).assertIsDisplayed()
    }

    @Test
    fun `error dialog should be shown or hidden according to the state`() {
        val errorTitle = "Error title"
        val errorMessage = "Error message"
        composeTestRule.assertNoDialogExists()
        composeTestRule
            .onNodeWithText(errorTitle)
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(errorMessage)
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialog = VaultState.DialogState.Error(
                    title = errorTitle.asText(),
                    message = errorMessage.asText(),
                ),
            )
        }

        composeTestRule
            .onAllNodesWithText(errorTitle)
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(errorMessage)
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `OK button click in error dialog should send DialogDismiss`() {
        val errorTitle = "Error title"
        val errorMessage = "Error message"
        mutableStateFlow.update {
            it.copy(
                dialog = VaultState.DialogState.Error(
                    title = errorTitle.asText(),
                    message = errorMessage.asText(),
                ),
            )
        }

        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify { viewModel.trySendAction(VaultAction.DialogDismiss) }
    }

    @Test
    fun `Error screen should be shown according to the state`() {
        val errorMessage = "Error message"
        val tryAgainButtonText = "Try again"
        composeTestRule
            .onNodeWithText(errorMessage)
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(tryAgainButtonText)
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                viewState = VaultState.ViewState.Error(
                    message = errorMessage.asText(),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(errorMessage)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(tryAgainButtonText)
            .assertIsDisplayed()
    }

    @Test
    fun `try again button click on the Error screen should send TryAgainClick`() {
        val errorMessage = "Error message"
        val tryAgainButtonText = "Try again"
        mutableStateFlow.update {
            it.copy(
                viewState = VaultState.ViewState.Error(
                    message = errorMessage.asText(),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(tryAgainButtonText)
            .performClick()

        verify { viewModel.trySendAction(VaultAction.TryAgainClick) }
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
    @Suppress("MaxLineLength")
    fun `NavigateToItemListing event for Card type should call onNavigateToVaultItemListingType with Card type`() {
        mutableEventFlow.tryEmit(VaultEvent.NavigateToItemListing(VaultItemListingType.Card))
        assertEquals(VaultItemListingType.Card, onNavigateToVaultItemListingType)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `NavigateToItemListing event for Identity type should call onNavigateToVaultItemListingType with Identity type`() {
        mutableEventFlow.tryEmit(VaultEvent.NavigateToItemListing(VaultItemListingType.Identity))
        assertEquals(VaultItemListingType.Identity, onNavigateToVaultItemListingType)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `NavigateToItemListing event for Login type should call onNavigateToVaultItemListingType with Login type`() {
        mutableEventFlow.tryEmit(VaultEvent.NavigateToItemListing(VaultItemListingType.Login))
        assertEquals(VaultItemListingType.Login, onNavigateToVaultItemListingType)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `NavigateToItemListing event for SecureNote type should call onNavigateToVaultItemListingType with SecureNote type`() {
        mutableEventFlow.tryEmit(VaultEvent.NavigateToItemListing(VaultItemListingType.SecureNote))
        assertEquals(VaultItemListingType.SecureNote, onNavigateToVaultItemListingType)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `NavigateToItemListing event for Trash type should call onNavigateToVaultItemListingType with Trash type`() {
        mutableEventFlow.tryEmit(VaultEvent.NavigateToItemListing(VaultItemListingType.Trash))
        assertEquals(VaultItemListingType.Trash, onNavigateToVaultItemListingType)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `NavigateToItemListing event for Folder type should call onNavigateToVaultItemListingType with Folder type`() {
        val mockFolderId = "mockFolderId"
        mutableEventFlow.tryEmit(
            VaultEvent.NavigateToItemListing(VaultItemListingType.Folder(mockFolderId)),
        )
        assertEquals(VaultItemListingType.Folder(mockFolderId), onNavigateToVaultItemListingType)
    }

    @Test
    fun `NavigateOutOfApp event should call exitApplication on the IntentHandler`() {
        mutableEventFlow.tryEmit(VaultEvent.NavigateOutOfApp)
        verify { intentHandler.exitApplication() }
    }

    @Test
    fun `totp section should be visible based on state`() {
        mutableStateFlow.update { state ->
            state.copy(
                viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                    totpItemsCount = 2,
                ),
            )
        }

        composeTestRule
            .onNodeWithText("TOTP")
            .assertTextEquals("TOTP", "1")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Verification codes")
            .assertTextEquals("Verification codes", "2")
            .assertIsDisplayed()

        mutableStateFlow.update { state ->
            state.copy(
                viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                    totpItemsCount = 0,
                ),
            )
        }

        composeTestRule
            .onNodeWithText("TOTP")
            .assertIsNotDisplayed()

        composeTestRule
            .onNodeWithText("Verification codes")
            .assertIsNotDisplayed()
    }

    @Test
    fun `clicking totp section should emit VerificationCodesClick action`() {
        mutableStateFlow.update { state ->
            state.copy(
                isPremium = true,
                viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                    totpItemsCount = 2,
                ),
            )
        }

        composeTestRule
            .onNodeWithText("Verification codes")
            .performClick()

        verify { viewModel.trySendAction(VaultAction.VerificationCodesClick) }
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
    fun `collection data should update according to the state`() {
        val collectionsHeader = "Collections"
        val collectionsCount = 1
        val collectionName = "Test Collection"
        val collectionCount = 3
        val collectionItem = VaultState.ViewState.CollectionItem(
            id = "12345",
            name = collectionName,
            itemCount = collectionCount,
        )

        composeTestRule.assertScrollableNodeDoesNotExist(collectionsHeader, substring = true)
        composeTestRule.assertScrollableNodeDoesNotExist(collectionName, substring = true)

        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                    collectionItems = listOf(collectionItem),
                ),
            )
        }

        composeTestRule
            .onNodeWithTextAfterScroll(collectionsHeader, substring = true)
            .assertTextEquals(collectionsHeader, collectionsCount.toString())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(collectionName)
            .assertTextEquals(collectionName, collectionCount.toString())
    }

    @Test
    fun `clicking a collection item should send CollectionClick with the correct item`() {
        val collectionName = "Test Collection"
        val collectionCount = 3
        val collectionItem = VaultState.ViewState.CollectionItem(
            id = "12345",
            name = collectionName,
            itemCount = collectionCount,
        )

        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                    collectionItems = listOf(collectionItem),
                ),
            )
        }

        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(collectionName))
        composeTestRule
            .onNodeWithText(collectionName)
            .assertTextEquals(collectionName, collectionCount.toString())
            .performClick()
        verify {
            viewModel.trySendAction(VaultAction.CollectionClick(collectionItem))
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
    environmentLabel = "bitwarden.com",
    isActive = true,
    isLoggedIn = true,
    isVaultUnlocked = true,
)

private val LOCKED_ACCOUNT_SUMMARY = AccountSummary(
    userId = "lockedUserId",
    name = "Locked User",
    email = "locked@bitwarden.com",
    avatarColorHex = "#00aaaa",
    environmentLabel = "bitwarden.com",
    isActive = false,
    isLoggedIn = true,
    isVaultUnlocked = false,
)

private val ORGANIZATION_VAULT_FILTER = VaultFilterType.OrganizationVault(
    organizationId = "testOrganizationId",
    organizationName = "Test Organization",
)

private val VAULT_FILTER_DATA = VaultFilterData(
    selectedVaultFilterType = VaultFilterType.AllVaults,
    vaultFilterTypes = listOf(
        VaultFilterType.AllVaults,
        VaultFilterType.MyVault,
        ORGANIZATION_VAULT_FILTER,
    ),
)

private val DEFAULT_STATE: VaultState = VaultState(
    appBarTitle = R.string.my_vault.asText(),
    avatarColorString = "#aa00aa",
    initials = "AU",
    accountSummaries = persistentListOf(
        ACTIVE_ACCOUNT_SUMMARY,
        LOCKED_ACCOUNT_SUMMARY,
    ),
    viewState = VaultState.ViewState.Loading,
    isPremium = false,
)

private val DEFAULT_CONTENT_VIEW_STATE: VaultState.ViewState.Content = VaultState.ViewState.Content(
    loginItemsCount = 0,
    cardItemsCount = 0,
    identityItemsCount = 0,
    secureNoteItemsCount = 0,
    favoriteItems = emptyList(),
    folderItems = emptyList(),
    noFolderItems = emptyList(),
    collectionItems = emptyList(),
    trashItemsCount = 0,
    totpItemsCount = 0,
)
