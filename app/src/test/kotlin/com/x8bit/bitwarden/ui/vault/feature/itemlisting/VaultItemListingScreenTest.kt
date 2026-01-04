package com.x8bit.bitwarden.ui.vault.feature.itemlisting

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.core.net.toUri
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.data.repository.util.baseIconUrl
import com.bitwarden.data.repository.util.baseWebSendUrl
import com.bitwarden.send.SendType
import com.bitwarden.ui.platform.base.util.toHostOrPathOrNull
import com.bitwarden.ui.platform.components.account.model.AccountSummary
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.manager.exit.ExitManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.assertLockOrLogoutDialogIsDisplayed
import com.bitwarden.ui.util.assertLogoutConfirmationDialogIsDisplayed
import com.bitwarden.ui.util.assertMasterPasswordDialogDisplayed
import com.bitwarden.ui.util.assertNoDialogExists
import com.bitwarden.ui.util.assertRemovalConfirmationDialogIsDisplayed
import com.bitwarden.ui.util.assertSwitcherIsDisplayed
import com.bitwarden.ui.util.assertSwitcherIsNotDisplayed
import com.bitwarden.ui.util.isProgressBar
import com.bitwarden.ui.util.onNodeWithTextAfterScroll
import com.bitwarden.ui.util.performAccountClick
import com.bitwarden.ui.util.performAccountIconClick
import com.bitwarden.ui.util.performAccountLongClick
import com.bitwarden.ui.util.performLockAccountClick
import com.bitwarden.ui.util.performLogoutAccountClick
import com.bitwarden.ui.util.performRemoveAccountClick
import com.bitwarden.ui.util.performYesDialogButtonClick
import com.bitwarden.vault.CipherType
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockLoginView
import com.x8bit.bitwarden.ui.credentials.manager.CredentialProviderCompletionManager
import com.x8bit.bitwarden.ui.credentials.manager.model.AssertFido2CredentialResult
import com.x8bit.bitwarden.ui.credentials.manager.model.CreateCredentialResult
import com.x8bit.bitwarden.ui.credentials.manager.model.GetCredentialsResult
import com.x8bit.bitwarden.ui.credentials.manager.model.GetPasswordCredentialResult
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.platform.manager.biometrics.BiometricsManager
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.AddEditSendRoute
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.ModeType
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendItemType
import com.x8bit.bitwarden.ui.tools.feature.send.viewsend.ViewSendRoute
import com.x8bit.bitwarden.ui.vault.components.model.CreateVaultItemType
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditArgs
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemArgs
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import com.x8bit.bitwarden.ui.vault.model.VaultAddEditType
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType
import io.mockk.every
import io.mockk.invoke
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@Suppress("LargeClass")
class VaultItemListingScreenTest : BitwardenComposeTest() {

    private var onNavigateBackCalled = false
    private var onNavigateToVaultAddItemScreenCalled = false
    private var onNavigateToAddEditSendScreenRoute: AddEditSendRoute? = null
    private var onNavigateToViewSendScreenRoute: ViewSendRoute? = null
    private var onNavigateToVaultItemArgs: VaultItemArgs? = null
    private var onNavigateToVaultEditItemScreenArgs: VaultAddEditArgs? = null
    private var onNavigateToSearchType: SearchType? = null
    private var onNavigateToVaultItemListingScreenType: VaultItemListingType? = null
    private var onNavigateToAddFolderCalled = false
    private var onNavigateToAddFolderParentFolderName: String? = null

    private val exitManager: ExitManager = mockk {
        every { exitApplication() } just runs
    }
    private val intentManager: IntentManager = mockk {
        every { shareText(any()) } just runs
        every { launchUri(any()) } just runs
    }
    private val credentialProviderCompletionManager: CredentialProviderCompletionManager = mockk {
        every { completeCredentialRegistration(any()) } just runs
        every { completeFido2Assertion(any()) } just runs
        every { completePasswordGet(any()) } just runs
        every { completeProviderGetCredentialsRequest(any()) } just runs
    }
    private val biometricsManager: BiometricsManager = mockk()
    private val mutableEventFlow = bufferedMutableSharedFlow<VaultItemListingEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<VaultItemListingViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        mockkStatic(String::toHostOrPathOrNull)
        every { AUTOFILL_SELECTION_DATA.uri?.toHostOrPathOrNull() } returns "www.test.com"
        setContent(
            exitManager = exitManager,
            intentManager = intentManager,
            credentialProviderCompletionManager = credentialProviderCompletionManager,
            biometricsManager = biometricsManager,
        ) {
            VaultItemListingScreen(
                viewModel = viewModel,
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToVaultItemScreen = { onNavigateToVaultItemArgs = it },
                onNavigateToVaultAddItemScreen = { onNavigateToVaultAddItemScreenCalled = true },
                onNavigateToAddEditSendItem = { onNavigateToAddEditSendScreenRoute = it },
                onNavigateToViewSendItem = { onNavigateToViewSendScreenRoute = it },
                onNavigateToSearch = { onNavigateToSearchType = it },
                onNavigateToVaultEditItemScreen = { onNavigateToVaultEditItemScreenArgs = it },
                onNavigateToVaultItemListing = { this.onNavigateToVaultItemListingScreenType = it },
                onNavigateToAddFolder = { folderName ->
                    onNavigateToAddFolderCalled = true
                    onNavigateToAddFolderParentFolderName = folderName
                },
            )
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(String::toHostOrPathOrNull)
    }

    @Test
    fun `the app bar title should update according to state`() {
        composeTestRule
            .onNodeWithText("Logins")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Items for www.test.com")
            .assertDoesNotExist()

        mutableStateFlow.value = STATE_FOR_AUTOFILL

        composeTestRule
            .onNodeWithText("Logins")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText("Items for www.test.com")
            .assertIsDisplayed()
    }

    @Test
    fun `back button should update according to state`() {
        composeTestRule
            .onNodeWithContentDescription("Back")
            .assertIsDisplayed()

        mutableStateFlow.value = STATE_FOR_AUTOFILL

        composeTestRule
            .onNodeWithContentDescription("Back")
            .assertDoesNotExist()
    }

    @Test
    fun `overflow menu should update according to state`() {
        composeTestRule
            .onNodeWithContentDescription("More")
            .assertIsDisplayed()

        mutableStateFlow.value = STATE_FOR_AUTOFILL

        composeTestRule
            .onNodeWithContentDescription("More")
            .assertDoesNotExist()
    }

    @Test
    fun `account icon should update according to state`() {
        composeTestRule
            .onNodeWithText("AU")
            .assertDoesNotExist()

        mutableStateFlow.value = STATE_FOR_AUTOFILL

        composeTestRule
            .onNodeWithText("AU")
            .assertIsDisplayed()
    }

    @Test
    fun `account icon click should show the account switcher`() {
        mutableStateFlow.value = STATE_FOR_AUTOFILL
        composeTestRule.assertSwitcherIsNotDisplayed(
            accountSummaries = ACCOUNT_SUMMARIES,
        )

        composeTestRule.performAccountIconClick()

        composeTestRule.assertSwitcherIsDisplayed(
            accountSummaries = ACCOUNT_SUMMARIES,
            isAddAccountButtonVisible = false,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `account click in the account switcher should send AccountSwitchClick and close switcher`() {
        // Open the Account Switcher
        mutableStateFlow.value = STATE_FOR_AUTOFILL
        composeTestRule.performAccountIconClick()

        composeTestRule.performAccountClick(accountSummary = LOCKED_ACCOUNT_SUMMARY)

        verify {
            viewModel.trySendAction(
                VaultItemListingsAction.SwitchAccountClick(LOCKED_ACCOUNT_SUMMARY),
            )
        }
        composeTestRule.assertSwitcherIsNotDisplayed(
            accountSummaries = ACCOUNT_SUMMARIES,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `account long click in the account switcher should show the lock-or-logout dialog and close the switcher`() {
        // Show the account switcher
        mutableStateFlow.value = STATE_FOR_AUTOFILL
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
        mutableStateFlow.value = STATE_FOR_AUTOFILL
        composeTestRule.performAccountIconClick()
        composeTestRule.performAccountLongClick(ACTIVE_ACCOUNT_SUMMARY)

        composeTestRule.performLockAccountClick()

        verify {
            viewModel.trySendAction(
                VaultItemListingsAction.LockAccountClick(ACTIVE_ACCOUNT_SUMMARY),
            )
        }
        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `logout button click in the lock-or-logout dialog should show the logout confirmation dialog and hide the lock-or-logout dialog`() {
        // Show the lock-or-logout dialog
        mutableStateFlow.value = STATE_FOR_AUTOFILL
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
        mutableStateFlow.value = STATE_FOR_AUTOFILL
        composeTestRule.performAccountIconClick()
        composeTestRule.performAccountLongClick(ACTIVE_ACCOUNT_SUMMARY)
        composeTestRule.performLogoutAccountClick()

        composeTestRule.performYesDialogButtonClick()

        verify {
            viewModel.trySendAction(
                VaultItemListingsAction.LogoutAccountClick(ACTIVE_ACCOUNT_SUMMARY),
            )
        }
        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `remove account button click in the lock-or-logout dialog should show the remove account confirmation dialog and hide the lock-or-logout dialog`() {
        // Show the lock-or-logout dialog
        val activeAccountSummary = ACTIVE_ACCOUNT_SUMMARY.copy(isLoggedIn = false)
        mutableStateFlow.update {
            it.copy(
                accountSummaries = listOf(activeAccountSummary),
                autofillSelectionData = AUTOFILL_SELECTION_DATA,
            )
        }
        composeTestRule.performAccountIconClick()
        composeTestRule.performAccountLongClick(activeAccountSummary)

        composeTestRule.performRemoveAccountClick()

        composeTestRule.assertRemovalConfirmationDialogIsDisplayed(activeAccountSummary)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `remove account button click in the remove account confirmation dialog should send LogoutAccountClick action and close the dialog`() {
        // Show the remove account confirmation dialog
        val activeAccountSummary = ACTIVE_ACCOUNT_SUMMARY.copy(isLoggedIn = false)
        mutableStateFlow.update {
            it.copy(
                accountSummaries = listOf(activeAccountSummary),
                autofillSelectionData = AUTOFILL_SELECTION_DATA,
            )
        }
        composeTestRule.performAccountIconClick()
        composeTestRule.performAccountLongClick(activeAccountSummary)
        composeTestRule.performRemoveAccountClick()

        composeTestRule.performYesDialogButtonClick()

        verify {
            viewModel.trySendAction(
                VaultItemListingsAction.LogoutAccountClick(activeAccountSummary),
            )
        }
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `NavigateBack event should invoke NavigateBack`() {
        mutableEventFlow.tryEmit(VaultItemListingEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `ExitApp event should invoke exitApplication`() {
        mutableEventFlow.tryEmit(VaultItemListingEvent.ExitApp)
        verify(exactly = 1) {
            exitManager.exitApplication()
        }
    }

    @Test
    fun `on ShowSnackbar event should display the snackbar`() {
        val message = "message"
        val data = BitwardenSnackbarData(message = message.asText())
        mutableEventFlow.tryEmit(VaultItemListingEvent.ShowSnackbar(data = data))
        composeTestRule
            .onNodeWithText(text = message)
            .assertIsDisplayed()
    }

    @Test
    fun `back gesture should send BackClick action`() {
        backDispatcher?.onBackPressed()

        verify(exactly = 1) {
            viewModel.trySendAction(VaultItemListingsAction.BackClick)
        }
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
    fun `policy warning should update according to state`() {
        mutableStateFlow.update {
            it.copy(
                itemListingType = VaultItemListingState.ItemListingType.Send.SendFile,
                viewState = VaultItemListingState.ViewState.NoItems(
                    header = "Save and protect your data".asText(),
                    message = "There are no Sends in your account.".asText(),
                    shouldShowAddButton = true,
                    buttonText = "Add an Item".asText(),
                ),
            )
        }
        val policyText = "Due to an enterprise policy, you are only " +
            "able to delete an existing Send."
        composeTestRule
            .onNodeWithText(policyText)
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                policyDisablesSend = true,
            )
        }

        composeTestRule
            .onNodeWithText(policyText)
            .assertIsDisplayed()
    }

    @Test
    fun `floating action button click should send AddItemClick action`() {
        composeTestRule
            .onNodeWithContentDescription("Add Item")
            .performClick()
        verify { viewModel.trySendAction(VaultItemListingsAction.AddVaultItemClick) }
    }

    @Test
    fun `Add an Item button click should send AddItemClick action`() {
        mutableStateFlow.update {
            it.copy(
                viewState = VaultItemListingState.ViewState.NoItems(
                    header = "Save and protect your data".asText(),
                    message = "There are no items in your vault.".asText(),
                    shouldShowAddButton = true,
                    buttonText = "Add an Item".asText(),
                ),
            )
        }
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
    fun `ShowShareSheet event should call shareText in intentManager`() {
        val content = "content"
        mutableEventFlow.tryEmit(VaultItemListingEvent.ShowShareSheet(content = content))
        verify {
            intentManager.shareText(content)
        }
    }

    @Test
    fun `NavigateToAdd VaultItem event should call NavigateToVaultAddItemScreen`() {
        mutableEventFlow.tryEmit(
            VaultItemListingEvent.NavigateToAddVaultItem(VaultItemCipherType.LOGIN),
        )
        assertTrue(onNavigateToVaultAddItemScreenCalled)
    }

    @Test
    fun `NavigateToAddSendItem should call onNavigateToAddEditSendScreen`() {
        val sendType = SendItemType.TEXT
        mutableEventFlow.tryEmit(VaultItemListingEvent.NavigateToAddSendItem(sendType = sendType))
        assertEquals(
            AddEditSendRoute(sendType = sendType, modeType = ModeType.ADD),
            onNavigateToAddEditSendScreenRoute,
        )
    }

    @Test
    fun `NavigateToViewSendItem should call onNavigateToViewSendScreen`() {
        val sendId = "id"
        val sendType = SendItemType.TEXT
        mutableEventFlow.tryEmit(
            VaultItemListingEvent.NavigateToViewSendItem(id = sendId, sendType = sendType),
        )
        assertEquals(
            ViewSendRoute(sendId = sendId, sendType = sendType),
            onNavigateToViewSendScreenRoute,
        )
    }

    @Test
    fun `NavigateToVaultSearchScreen should call onNavigateToSearch`() {
        val searchType = SearchType.Vault.SecureNotes
        mutableEventFlow.tryEmit(VaultItemListingEvent.NavigateToSearchScreen(searchType))
        assertEquals(searchType, onNavigateToSearchType)
    }

    @Test
    fun `NavigateToEditCipher should call onNavigateToVaultEditItemScreen`() {
        val cipherId = "cipherId"
        val type = VaultItemCipherType.LOGIN
        mutableEventFlow.tryEmit(
            VaultItemListingEvent.NavigateToEditCipher(cipherId = cipherId, cipherType = type),
        )
        assertEquals(
            VaultAddEditArgs(
                vaultAddEditType = VaultAddEditType.EditItem(vaultItemId = cipherId),
                vaultItemCipherType = type,
            ),
            onNavigateToVaultEditItemScreenArgs,
        )
    }

    @Test
    fun `NavigateToEditSendItem event should call onNavigateToAddEditSendScreen`() {
        val sendId = "sendId"
        val sendType = SendItemType.FILE
        mutableEventFlow.tryEmit(
            VaultItemListingEvent.NavigateToEditSendItem(id = sendId, sendType = sendType),
        )
        assertEquals(
            AddEditSendRoute(sendType = sendType, modeType = ModeType.EDIT, sendId = sendId),
            onNavigateToAddEditSendScreenRoute,
        )
    }

    @Test
    fun `NavigateToVaultItem event should call NavigateToVaultItemScreen`() {
        val id = "id4321"
        val type = VaultItemCipherType.LOGIN
        mutableEventFlow.tryEmit(VaultItemListingEvent.NavigateToVaultItem(id = id, type = type))
        assertEquals(
            VaultItemArgs(vaultItemId = id, cipherType = type),
            onNavigateToVaultItemArgs,
        )
    }

    @Test
    fun `NavigateToFolderItem should call onNavigateToVaultItemListing`() {
        val itemListingType = VaultItemListingType.Folder("testId")
        mutableEventFlow.tryEmit(VaultItemListingEvent.NavigateToFolderItem("testId"))
        assertEquals(itemListingType, onNavigateToVaultItemListingScreenType)
    }

    @Test
    fun `NavigateToUrl should call launchUri on the IntentManager`() {
        val url = "www.test.com"
        mutableEventFlow.tryEmit(VaultItemListingEvent.NavigateToUrl(url))
        verify(exactly = 1) {
            intentManager.launchUri(url.toUri())
        }
    }

    @Test
    fun `progressbar should be displayed according to state`() {
        mutableStateFlow.update { DEFAULT_STATE }
        composeTestRule.onNode(isProgressBar).assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                viewState = VaultItemListingState.ViewState.NoItems(
                    header = "Save and protect your data".asText(),
                    message = "There are no items in your vault.".asText(),
                    shouldShowAddButton = true,
                    buttonText = "Add an Item".asText(),
                ),
            )
        }
        composeTestRule.onNode(isProgressBar).assertDoesNotExist()
    }

    @Test
    fun `error text and retry should be displayed according to state`() {
        val message = "error_message"
        mutableStateFlow.update { DEFAULT_STATE }
        composeTestRule
            .onNodeWithText(message)
            .assertIsNotDisplayed()

        mutableStateFlow.update {
            it.copy(
                viewState = VaultItemListingState.ViewState.NoItems(
                    header = "Save and protect your data".asText(),
                    message = "There are no items in your vault.".asText(),
                    shouldShowAddButton = true,
                    buttonText = "Add an Item".asText(),
                ),
            )
        }
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
    fun `Add an Item button should be displayed according to state`() {
        mutableStateFlow.update { DEFAULT_STATE }
        composeTestRule
            .onNodeWithText(text = "Add an Item")
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                viewState = VaultItemListingState.ViewState.NoItems(
                    header = "Save and protect your data".asText(),
                    message = "There are no items in your vault.".asText(),
                    shouldShowAddButton = true,
                    buttonText = "Add an Item".asText(),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(text = "Add an Item")
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                viewState = VaultItemListingState.ViewState.NoItems(
                    header = "Save and protect your data".asText(),
                    message = "There are no items in your vault.".asText(),
                    shouldShowAddButton = false,
                    buttonText = "Add an Item".asText(),
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
            it.copy(
                viewState = VaultItemListingState.ViewState.NoItems(
                    header = "Save and protect your data".asText(),
                    message = "There are no items in your vault.".asText(),
                    shouldShowAddButton = true,
                    buttonText = "Save passkey as new login".asText(),
                ),
            )
        }
        composeTestRule
            .onNodeWithText(text = "Save and protect your data")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = "There are no items in your vault.")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = "Save passkey as new login")
            .assertIsDisplayed()
    }

    @Test
    fun `floating action button should be displayed according to state`() {
        mutableStateFlow.update { DEFAULT_STATE }

        composeTestRule
            .onNodeWithContentDescription("Add Item")
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

        mutableStateFlow.update {
            it.copy(
                itemListingType = VaultItemListingState.ItemListingType.Vault.SshKey,
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Add item")
            .assertDoesNotExist()
    }

    @Test
    fun `Folders text should be displayed according to state`() {
        val folders = "FOLDERS (1)"
        mutableStateFlow.update { DEFAULT_STATE }
        composeTestRule
            .onNodeWithText(text = folders)
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                viewState = VaultItemListingState.ViewState.Content(
                    displayItemList = emptyList(),
                    displayFolderList = listOf(
                        VaultItemListingState.FolderDisplayItem(
                            name = "test",
                            id = "1",
                            count = 0,
                        ),
                    ),
                    displayCollectionList = emptyList(),
                ),
            )
        }
        composeTestRule
            .onNodeWithTextAfterScroll(text = folders)
            .assertIsDisplayed()
    }

    @Test
    fun `Folders text count should be displayed according to state`() {
        mutableStateFlow.update { DEFAULT_STATE }
        composeTestRule
            .onNodeWithText(text = "FOLDERS (1)")
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                viewState = VaultItemListingState.ViewState.Content(
                    displayItemList = emptyList(),
                    displayFolderList = listOf(
                        VaultItemListingState.FolderDisplayItem(name = "test", id = "1", count = 0),
                    ),
                    displayCollectionList = emptyList(),
                ),
            )
        }
        composeTestRule
            .onNodeWithTextAfterScroll(text = "FOLDERS (1)")
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                viewState = VaultItemListingState.ViewState.Content(
                    displayItemList = emptyList(),
                    displayFolderList = listOf(
                        VaultItemListingState.FolderDisplayItem(
                            name = "test",
                            id = "1",
                            count = 0,
                        ),
                        VaultItemListingState.FolderDisplayItem(
                            name = "test1",
                            id = "2",
                            count = 0,
                        ),
                        VaultItemListingState.FolderDisplayItem(
                            name = "test2",
                            id = "3",
                            count = 0,
                        ),
                    ),
                    displayCollectionList = emptyList(),
                ),
            )
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "FOLDERS (3)")
            .assertIsDisplayed()
    }

    @Test
    fun `folderDisplayItems should be displayed according to state`() {
        val folderName = "TestFolder"
        mutableStateFlow.update { DEFAULT_STATE }
        composeTestRule
            .onNodeWithText(text = folderName)
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                viewState = VaultItemListingState.ViewState.Content(
                    displayCollectionList = emptyList(),
                    displayItemList = emptyList(),
                    displayFolderList = listOf(
                        VaultItemListingState.FolderDisplayItem(
                            name = folderName,
                            id = "1",
                            count = 0,
                        ),
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(text = folderName)
            .assertIsDisplayed()
    }

    @Test
    fun `Collections text should be displayed according to state`() {
        val collectionName = "Collections"
        mutableStateFlow.update { DEFAULT_STATE }
        composeTestRule
            .onNodeWithText(text = collectionName)
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                viewState = VaultItemListingState.ViewState.Content(
                    displayItemList = emptyList(),
                    displayFolderList = emptyList(),
                    displayCollectionList = listOf(
                        VaultItemListingState.CollectionDisplayItem(
                            name = collectionName,
                            id = "1",
                            count = 0,
                        ),
                    ),
                ),
            )
        }
        composeTestRule
            .onNodeWithTextAfterScroll(collectionName)
            .assertIsDisplayed()
    }

    @Test
    fun `Collection text count should be displayed according to state`() {
        mutableStateFlow.update { DEFAULT_STATE }
        composeTestRule
            .onNodeWithText(text = "COLLECTIONS (3)")
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                viewState = VaultItemListingState.ViewState.Content(
                    displayItemList = emptyList(),
                    displayFolderList = emptyList(),
                    displayCollectionList = listOf(
                        VaultItemListingState.CollectionDisplayItem(
                            name = "Collection",
                            id = "1",
                            count = 0,
                        ),
                        VaultItemListingState.CollectionDisplayItem(
                            name = "Collection2",
                            id = "2",
                            count = 0,
                        ),
                        VaultItemListingState.CollectionDisplayItem(
                            name = "Collection3",
                            id = "3",
                            count = 0,
                        ),
                    ),
                ),
            )
        }
        composeTestRule
            .onNodeWithTextAfterScroll(text = "COLLECTIONS (3)")
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                viewState = VaultItemListingState.ViewState.Content(
                    displayItemList = emptyList(),
                    displayFolderList = emptyList(),
                    displayCollectionList = listOf(
                        VaultItemListingState.CollectionDisplayItem(
                            name = "Collection",
                            id = "1",
                            count = 0,
                        ),
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "COLLECTIONS (1)")
            .assertIsDisplayed()
    }

    @Test
    fun `collectionDisplayItems should be displayed according to state`() {
        val collectionName = "TestCollection"
        mutableStateFlow.update { DEFAULT_STATE }
        composeTestRule
            .onNodeWithText(text = collectionName)
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                viewState = VaultItemListingState.ViewState.Content(
                    displayItemList = emptyList(),
                    displayFolderList = emptyList(),
                    displayCollectionList = listOf(
                        VaultItemListingState.CollectionDisplayItem(
                            name = collectionName,
                            id = "1",
                            count = 0,
                        ),
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(text = collectionName)
            .assertIsDisplayed()
    }

    @Test
    fun `Items text should be displayed according to state`() {
        val items = "ITEMS (1)"
        mutableStateFlow.update { DEFAULT_STATE }
        composeTestRule
            .onNodeWithText(text = items)
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                viewState = VaultItemListingState.ViewState.Content(
                    displayCollectionList = emptyList(),
                    displayItemList = listOf(
                        createDisplayItem(number = 1),
                    ),
                    displayFolderList = emptyList(),
                ),
            )
        }
        composeTestRule
            .onNodeWithTextAfterScroll(text = items)
            .assertIsDisplayed()
    }

    @Test
    fun `Items text count should be displayed according to state`() {
        mutableStateFlow.update { DEFAULT_STATE }
        composeTestRule
            .onNodeWithText(text = "ITEMS (1)")
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                viewState = VaultItemListingState.ViewState.Content(
                    displayCollectionList = emptyList(),
                    displayItemList = listOf(
                        createDisplayItem(number = 1),
                    ),
                    displayFolderList = emptyList(),
                ),
            )
        }
        composeTestRule
            .onNodeWithTextAfterScroll(text = "ITEMS (1)")
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                viewState = VaultItemListingState.ViewState.Content(
                    displayCollectionList = emptyList(),
                    displayItemList = listOf(
                        createDisplayItem(number = 1),
                        createDisplayItem(number = 2),
                        createDisplayItem(number = 3),
                        createDisplayItem(number = 4),
                    ),
                    displayFolderList = emptyList(),
                ),
            )
        }

        composeTestRule
            .onNodeWithTextAfterScroll(text = "ITEMS (4)")
            .assertIsDisplayed()
    }

    @Test
    fun `displayItems should be displayed according to state`() {
        mutableStateFlow.update {
            it.copy(
                viewState = VaultItemListingState.ViewState.Content(
                    displayCollectionList = emptyList(),
                    displayItemList = listOf(
                        createDisplayItem(number = 1),
                    ),
                    displayFolderList = emptyList(),
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

    @Suppress("MaxLineLength")
    @Test
    fun `clicking on a display item when master password reprompt is not required should send ItemClick action`() {
        mutableStateFlow.update {
            it.copy(
                viewState = VaultItemListingState.ViewState.Content(
                    displayCollectionList = emptyList(),
                    displayItemList = listOf(
                        createDisplayItem(number = 1)
                            .copy(
                                isAutofill = false,
                                shouldShowMasterPasswordReprompt = false,
                            ),
                    ),
                    displayFolderList = emptyList(),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(text = "mockTitle-1")
            .assertIsDisplayed()
            .performClick()
        verify {
            viewModel.trySendAction(
                VaultItemListingsAction.ItemClick(
                    id = "mockId-1",
                    type = VaultItemListingState.DisplayItem.ItemType.Sends(type = SendType.TEXT),
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking on a display item when master password reprompt is required for autofill should show the master password dialog`() {
        mutableStateFlow.update {
            it.copy(
                viewState = VaultItemListingState.ViewState.Content(
                    displayCollectionList = emptyList(),
                    displayItemList = listOf(
                        createDisplayItem(number = 1)
                            .copy(
                                isAutofill = true,
                                shouldShowMasterPasswordReprompt = true,
                            ),
                    ),
                    displayFolderList = emptyList(),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(text = "mockTitle-1")
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Master password confirmation")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(
                text = "This action is protected, to continue please re-enter your master " +
                    "password to verify your identity.",
            )
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(text = "Master password")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(text = "Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(text = "Submit")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        verify(exactly = 0) {
            viewModel.trySendAction(any())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking on a display item when master password reprompt is required for totp flow should show the master password dialog`() {
        mutableStateFlow.update {
            it.copy(
                viewState = VaultItemListingState.ViewState.Content(
                    displayCollectionList = emptyList(),
                    displayItemList = listOf(
                        createDisplayItem(number = 1).copy(
                            shouldShowMasterPasswordReprompt = true,
                        ),
                    ),
                    displayFolderList = emptyList(),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(text = "mockTitle-1")
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Master password confirmation")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(
                text = "This action is protected, to continue please re-enter your master " +
                    "password to verify your identity.",
            )
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(text = "Master password")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(text = "Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(text = "Submit")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        verify(exactly = 0) {
            viewModel.trySendAction(any())
        }
    }

    @Test
    fun `clicking cancel on the master password dialog should close the dialog`() {
        mutableStateFlow.update {
            it.copy(
                viewState = VaultItemListingState.ViewState.Content(
                    displayCollectionList = emptyList(),
                    displayItemList = listOf(
                        createDisplayItem(number = 1)
                            .copy(
                                isAutofill = true,
                                shouldShowMasterPasswordReprompt = true,
                            ),
                    ),
                    displayFolderList = emptyList(),
                ),
            )
        }
        composeTestRule
            .onNodeWithText(text = "mockTitle-1")
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking submit on the master password dialog should close the dialog and send MasterPasswordRepromptSubmit`() {
        mutableStateFlow.update {
            it.copy(
                viewState = VaultItemListingState.ViewState.Content(
                    displayCollectionList = emptyList(),
                    displayItemList = listOf(
                        createDisplayItem(number = 1)
                            .copy(
                                isAutofill = true,
                                shouldShowMasterPasswordReprompt = true,
                            ),
                    ),
                    displayFolderList = emptyList(),
                ),
            )
        }
        composeTestRule
            .onNodeWithText(text = "mockTitle-1")
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
                VaultItemListingsAction.MasterPasswordRepromptSubmit(
                    password = "password",
                    masterPasswordRepromptData = MasterPasswordRepromptData.Autofill(
                        cipherId = "mockId-1",
                    ),
                ),
            )
        }
        composeTestRule.assertNoDialogExists()
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
            it.copy(itemListingType = VaultItemListingState.ItemListingType.Vault.SshKey)
        }
        composeTestRule
            .onNodeWithText(text = "SSH keys")
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

    @Test
    fun `on overflow item click should display menu`() {
        composeTestRule
            .onNodeWithContentDescription(label = "More")
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Sync")
            .filterToOne(hasAnyAncestor(isPopup()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(text = "Lock")
            .filterToOne(hasAnyAncestor(isPopup()))
            .assertIsDisplayed()
    }

    @Test
    fun `on sync click should send SyncClick`() {
        composeTestRule
            .onNodeWithContentDescription(label = "More")
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Sync")
            .filterToOne(hasAnyAncestor(isPopup()))
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemListingsAction.SyncClick)
        }
    }

    @Test
    fun `on lock click should send LockClick`() {
        composeTestRule
            .onNodeWithContentDescription(label = "More")
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Lock")
            .filterToOne(hasAnyAncestor(isPopup()))
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemListingsAction.LockClick)
        }
    }

    @Test
    fun `on cipher item overflow option click should emit the appropriate action`() {
        mutableStateFlow.update {
            it.copy(
                itemListingType = VaultItemListingState.ItemListingType.Vault.Login,
                viewState = VaultItemListingState.ViewState.Content(
                    displayCollectionList = emptyList(),
                    displayItemList = listOf(createCipherDisplayItem(number = 1)),
                    displayFolderList = emptyList(),
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
            .assertIsDisplayed()
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(
                VaultItemListingsAction.OverflowOptionClick(
                    action = ListingItemOverflowAction.VaultAction.EditClick(
                        cipherId = "mockId-1",
                        cipherType = CipherType.LOGIN,
                        requiresPasswordReprompt = true,
                    ),
                ),
            )
        }

        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on cipher item overflow option click when reprompt is required should show the master password dialog`() {
        mutableStateFlow.update {
            it.copy(
                itemListingType = VaultItemListingState.ItemListingType.Vault.Login,
                viewState = VaultItemListingState.ViewState.Content(
                    displayCollectionList = emptyList(),
                    displayItemList = listOf(
                        createCipherDisplayItem(number = 1)
                            .copy(shouldShowMasterPasswordReprompt = true),
                    ),
                    displayFolderList = emptyList(),
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
            .assertIsDisplayed()
            .performClick()
        verify(exactly = 0) { viewModel.trySendAction(any()) }

        composeTestRule.assertMasterPasswordDialogDisplayed()
    }

    @Test
    fun `send item overflow item button should update according to state`() {
        mutableStateFlow.update {
            it.copy(
                itemListingType = VaultItemListingState.ItemListingType.Send.SendFile,
                viewState = VaultItemListingState.ViewState.Content(
                    displayCollectionList = emptyList(),
                    displayItemList = listOf(createDisplayItem(number = 1)),
                    displayFolderList = emptyList(),
                ),
            )
        }
        composeTestRule
            .onNodeWithContentDescription("Options")
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                policyDisablesSend = true,
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Options")
            .assertDoesNotExist()
    }

    @Test
    fun `on send item overflow click should display dialog`() {
        val number = 1
        mutableStateFlow.update {
            it.copy(
                viewState = VaultItemListingState.ViewState.Content(
                    displayCollectionList = emptyList(),
                    displayItemList = listOf(createDisplayItem(number = number)),
                    displayFolderList = emptyList(),
                ),
            )
        }
        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onNodeWithContentDescription("Options")
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onAllNodesWithText("mockTitle-$number")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `on send item overflow option click should emit the appropriate action`() {
        val number = 1
        mutableStateFlow.update {
            it.copy(
                viewState = VaultItemListingState.ViewState.Content(
                    displayCollectionList = emptyList(),
                    displayItemList = listOf(createDisplayItem(number = number)),
                    displayFolderList = emptyList(),
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
                VaultItemListingsAction.OverflowOptionClick(
                    action = ListingItemOverflowAction.SendAction.ViewClick(
                        sendId = "mockId-$number",
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
                VaultItemListingsAction.OverflowOptionClick(
                    action = ListingItemOverflowAction.SendAction.EditClick(
                        sendId = "mockId-$number",
                        sendType = SendType.TEXT,
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
                VaultItemListingsAction.OverflowOptionClick(
                    action = ListingItemOverflowAction.SendAction.CopyUrlClick(
                        sendUrl = "www.test.com",
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
                VaultItemListingsAction.OverflowOptionClick(
                    action = ListingItemOverflowAction.SendAction.ShareUrlClick(
                        sendUrl = "www.test.com",
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
                VaultItemListingsAction.OverflowOptionClick(
                    action = ListingItemOverflowAction.SendAction.RemovePasswordClick(
                        sendId = "mockId-$number",
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
                viewState = VaultItemListingState.ViewState.Content(
                    displayCollectionList = emptyList(),
                    displayItemList = listOf(createDisplayItem(number = 1)),
                    displayFolderList = emptyList(),
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
                VaultItemListingsAction.OverflowOptionClick(
                    action = ListingItemOverflowAction.SendAction.DeleteClick(sendId = sendId),
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
                dialogState = VaultItemListingState.DialogState.Error(
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
            it.copy(
                dialogState = VaultItemListingState.DialogState.Loading(
                    message = loadingMessage.asText(),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(loadingMessage)
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `user verification master password prompt dialog should display and function according to state`() {
        val selectedCipherId = "selectedCipherId"
        val dialogTitle = "Master password confirmation"
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText(dialogTitle).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState
                    .DialogState
                    .UserVerificationMasterPasswordPrompt(
                        selectedCipherId = selectedCipherId,
                    ),
            )
        }

        composeTestRule
            .onNodeWithText(dialogTitle)
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))

        composeTestRule
            .onAllNodesWithText(text = "Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify {
            viewModel.trySendAction(
                VaultItemListingsAction.DismissUserVerificationDialogClick,
            )
        }

        composeTestRule
            .onAllNodesWithText(text = "Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify {
            viewModel.trySendAction(
                VaultItemListingsAction.DismissUserVerificationDialogClick,
            )
        }

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
                VaultItemListingsAction.MasterPasswordUserVerificationSubmit(
                    password = "password",
                    selectedCipherId = selectedCipherId,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `user verification master password error dialog should display and function according to state`() {
        val selectedCipherId = "selectedCipherId"
        val dialogMessage = "Invalid master password. Try again."
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText(dialogMessage).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState.DialogState.UserVerificationMasterPasswordError(
                    title = null,
                    message = dialogMessage.asText(),
                    selectedCipherId = selectedCipherId,
                ),
            )
        }

        composeTestRule
            .onNodeWithText(dialogMessage)
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))

        composeTestRule
            .onAllNodesWithText(text = "Okay")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify {
            viewModel.trySendAction(
                VaultItemListingsAction.RetryUserVerificationPasswordVerificationClick(
                    selectedCipherId = selectedCipherId,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `user verification pin prompt dialog should display and function according to state`() {
        val selectedCipherId = "selectedCipherId"
        val dialogTitle = "Verify PIN"
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText(dialogTitle).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState.DialogState.UserVerificationPinPrompt(
                    selectedCipherId = selectedCipherId,
                ),
            )
        }

        composeTestRule
            .onNodeWithText(dialogTitle)
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))

        composeTestRule
            .onAllNodesWithText(text = "Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify {
            viewModel.trySendAction(
                VaultItemListingsAction.DismissUserVerificationDialogClick,
            )
        }

        composeTestRule
            .onAllNodesWithText(text = "Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify {
            viewModel.trySendAction(
                VaultItemListingsAction.DismissUserVerificationDialogClick,
            )
        }

        composeTestRule
            .onAllNodesWithText(text = "PIN")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performTextInput("PIN")
        composeTestRule
            .onAllNodesWithText(text = "Submit")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultItemListingsAction.PinUserVerificationSubmit(
                    pin = "PIN",
                    selectedCipherId = selectedCipherId,
                ),
            )
        }
    }

    @Test
    fun `user verification pin error dialog should display and function according to state`() {
        val selectedCipherId = "selectedCipherId"
        val dialogMessage = "Invalid PIN. Try again."
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText(dialogMessage).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState.DialogState.UserVerificationPinError(
                    title = null,
                    message = dialogMessage.asText(),
                    selectedCipherId = selectedCipherId,
                ),
            )
        }

        composeTestRule
            .onNodeWithText(dialogMessage)
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))

        composeTestRule
            .onAllNodesWithText(text = "Okay")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify {
            viewModel.trySendAction(
                VaultItemListingsAction.RetryUserVerificationPinVerificationClick(
                    selectedCipherId = selectedCipherId,
                ),
            )
        }
    }

    @Test
    fun `user verification pin set up dialog should display and function according to state`() {
        val selectedCipherId = "selectedCipherId"
        val dialogMessage = "Enter your PIN code"
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText(dialogMessage).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState.DialogState.UserVerificationPinSetUpPrompt(
                    selectedCipherId = selectedCipherId,
                ),
            )
        }

        composeTestRule
            .onAllNodesWithText(text = "Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify {
            viewModel.trySendAction(
                VaultItemListingsAction.DismissUserVerificationDialogClick,
            )
        }

        composeTestRule
            .onAllNodesWithText(text = "PIN")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performTextInput("1234")
        composeTestRule
            .onAllNodesWithText(text = "Submit")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultItemListingsAction.UserVerificationPinSetUpSubmit(
                    pin = "1234",
                    selectedCipherId = selectedCipherId,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `user verification pin set up error dialog should display and function according to state`() {
        val selectedCipherId = "selectedCipherId"
        val dialogMessage = "The PIN field is required."
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText(dialogMessage).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState.DialogState.UserVerificationPinSetUpError(
                    title = null,
                    message = dialogMessage.asText(),
                    selectedCipherId = selectedCipherId,
                ),
            )
        }

        composeTestRule
            .onAllNodesWithText(text = "Okay")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify {
            viewModel.trySendAction(
                VaultItemListingsAction.UserVerificationPinSetUpRetryClick(
                    selectedCipherId = selectedCipherId,
                ),
            )
        }
    }

    @Test
    fun `fido2 error dialog should display and function according to state`() {
        val dialogMessage = "Passkey error message"
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText(dialogMessage).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState.DialogState.CredentialManagerOperationFail(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = dialogMessage.asText(),
                ),
            )
        }

        composeTestRule
            .onAllNodesWithText(text = "Okay")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultItemListingsAction.DismissCredentialManagerErrorDialogClick(
                    message = dialogMessage.asText(),
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `CompleteCredentialRegistration event should call CredentialProviderCompletionManager with result`() {
        val result = CreateCredentialResult.Success.Fido2CredentialRegistered("mockResponse")
        mutableEventFlow.tryEmit(VaultItemListingEvent.CompleteCredentialRegistration(result))
        verify {
            credentialProviderCompletionManager.completeCredentialRegistration(result)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `CompleteFido2Assertion event should call CredentialProviderCompletionManager with result`() {
        val result = AssertFido2CredentialResult.Success("mockResponse")
        mutableEventFlow.tryEmit(VaultItemListingEvent.CompleteFido2Assertion(result))
        verify {
            credentialProviderCompletionManager.completeFido2Assertion(result)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `GetPasswordCredentialResult event should call CredentialProviderCompletionManager with result`() {
        val result = GetPasswordCredentialResult.Success(createMockLoginView(1))
        mutableEventFlow.tryEmit(
            VaultItemListingEvent.CompleteProviderGetPasswordCredentialRequest(result),
        )
        verify {
            credentialProviderCompletionManager.completePasswordGet(result)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `CompleteProviderGetCredentialsRequest event should call CredentialProviderCompletionManager with result`() {
        val result = GetCredentialsResult.Success(
            credentialEntries = listOf(mockk()),
            userId = "mockUserId",
        )
        mutableEventFlow.tryEmit(
            VaultItemListingEvent.CompleteProviderGetCredentialsRequest(result),
        )
        verify {
            credentialProviderCompletionManager.completeProviderGetCredentialsRequest(result)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `CredentialManagerUserVerification event should perform user verification when it is supported`() {
        every {
            biometricsManager.promptUserVerification(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } just runs
        mutableEventFlow.tryEmit(
            VaultItemListingEvent.CredentialManagerUserVerification(
                isRequired = true,
                selectedCipherView = createMockCipherView(number = 1),
            ),
        )
        verify {
            biometricsManager.promptUserVerification(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `promptForUserVerification onSuccess should send UserVerificationSuccess action`() {
        val selectedCipherView = createMockCipherView(number = 1)
        every {
            biometricsManager.promptUserVerification(
                onSuccess = captureLambda(),
                any(),
                any(),
                any(),
                any(),
            )
        } answers {
            lambda<() -> Unit>().invoke()
        }

        mutableEventFlow.tryEmit(
            VaultItemListingEvent.CredentialManagerUserVerification(
                isRequired = true,
                selectedCipherView = selectedCipherView,
            ),
        )

        verify {
            viewModel.trySendAction(
                VaultItemListingsAction.UserVerificationSuccess(selectedCipherView),
            )
        }
    }

    @Test
    fun `promptForUserVerification onCancel should send UserVerificationCancelled action`() {
        val selectedCipherView = createMockCipherView(number = 1)
        every {
            biometricsManager.promptUserVerification(
                any(),
                onCancel = captureLambda(),
                any(),
                any(),
                any(),
            )
        } answers {
            lambda<() -> Unit>().invoke()
        }

        mutableEventFlow.tryEmit(
            VaultItemListingEvent.CredentialManagerUserVerification(
                isRequired = true,
                selectedCipherView = selectedCipherView,
            ),
        )

        verify {
            viewModel.trySendAction(
                VaultItemListingsAction.UserVerificationCancelled,
            )
        }
    }

    @Test
    fun `promptForUserVerification onLockOut should send UserVerificationLockOut action`() {
        val selectedCipherView = createMockCipherView(number = 1)
        every {
            biometricsManager.promptUserVerification(
                any(),
                any(),
                onLockOut = captureLambda(),
                any(),
                any(),
            )
        } answers {
            lambda<() -> Unit>().invoke()
        }

        mutableEventFlow.tryEmit(
            VaultItemListingEvent.CredentialManagerUserVerification(
                isRequired = true,
                selectedCipherView = selectedCipherView,
            ),
        )

        verify {
            viewModel.trySendAction(
                VaultItemListingsAction.UserVerificationLockOut,
            )
        }
    }

    @Test
    fun `promptForUserVerification onError should send UserVerificationFail action`() {
        val selectedCipherView = createMockCipherView(number = 1)
        every {
            biometricsManager.promptUserVerification(
                any(),
                any(),
                any(),
                onError = captureLambda(),
                any(),
            )
        } answers {
            lambda<() -> Unit>().invoke()
        }

        mutableEventFlow.tryEmit(
            VaultItemListingEvent.CredentialManagerUserVerification(
                isRequired = true,
                selectedCipherView = selectedCipherView,
            ),
        )

        verify {
            viewModel.trySendAction(
                VaultItemListingsAction.UserVerificationFail,
            )
        }
    }

    @Test
    fun `promptForUserVerification onNotSupported should send UserVerificationNotFailed action`() {
        val selectedCipherView = createMockCipherView(number = 1)
        every {
            biometricsManager.promptUserVerification(
                any(),
                any(),
                any(),
                any(),
                onNotSupported = captureLambda(),
            )
        } answers {
            lambda<() -> Unit>().invoke()
        }

        mutableEventFlow.tryEmit(
            VaultItemListingEvent.CredentialManagerUserVerification(
                isRequired = true,
                selectedCipherView = selectedCipherView,
            ),
        )

        verify {
            viewModel.trySendAction(
                VaultItemListingsAction.UserVerificationNotSupported(
                    selectedCipherId = selectedCipherView.id,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `OverwritePasskeyConfirmationPrompt should display based on dialog state and send ConfirmOverwriteExistingPasskeyClick on Ok click`() {
        val stateWithDialog = DEFAULT_STATE
            .copy(
                dialogState = VaultItemListingState.DialogState.OverwritePasskeyConfirmationPrompt(
                    cipherViewId = "mockCipherViewId",
                ),
            )

        mutableStateFlow.value = stateWithDialog

        composeTestRule
            .onNodeWithText("Overwrite passkey?")
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))
        composeTestRule
            .onNodeWithText("This item already contains a passkey. Are you sure you want to overwrite the current passkey?")
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))
        composeTestRule
            .onAllNodesWithText(text = "Okay")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultItemListingsAction.ConfirmOverwriteExistingPasskeyClick(
                    cipherViewId = "mockCipherViewId",
                ),
            )
        }
    }

    @Test
    fun `NavigateToAddFolder event calls onNavigateToAddFolder callback with parent name`() {
        val parentFolder = "momNpop"
        mutableEventFlow.tryEmit(
            VaultItemListingEvent.NavigateToAddFolder(parentFolderName = parentFolder),
        )
        assertTrue(onNavigateToAddFolderCalled)
        assertEquals(
            parentFolder,
            onNavigateToAddFolderParentFolderName,
        )
    }

    @Test
    fun `VaultItemTypeSelection dialog state show vault item type selection dialog`() {
        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState.DialogState.VaultItemTypeSelection(
                    excludedOptions = persistentListOf(CreateVaultItemType.SSH_KEY),
                ),
            )
        }

        composeTestRule
            .onNode(isDialog())
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Type")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `when option is selected in VaultItemTypeSelection dialog add item action is sent`() {
        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState.DialogState.VaultItemTypeSelection(
                    excludedOptions = persistentListOf(
                        CreateVaultItemType.SSH_KEY,
                        CreateVaultItemType.FOLDER,
                    ),
                ),
            )
        }

        composeTestRule
            .onNode(isDialog())
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Card")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(VaultItemListingsAction.DismissDialogClick)
            viewModel.trySendAction(
                VaultItemListingsAction.ItemTypeToAddSelected(
                    CreateVaultItemType.CARD,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when Trust is selected in TrustPrivilegedAppPrompt dialog TrustPrivilegedApp action is sent`() {
        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState.DialogState.TrustPrivilegedAddPrompt(
                    message = "message".asText(),
                    selectedCipherId = null,
                ),
            )
        }

        composeTestRule
            .onNode(isDialog())
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Trust")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(VaultItemListingsAction.TrustPrivilegedAppClick(null))
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when Cancel is selected in TrustPrivilegedAppPrompt dialog DismissCredentialManagerErrorDialogClick action is sent`() {
        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState.DialogState.TrustPrivilegedAddPrompt(
                    message = "message".asText(),
                    selectedCipherId = null,
                ),
            )
        }

        composeTestRule
            .onNode(isDialog())
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(
                VaultItemListingsAction.DismissCredentialManagerErrorDialogClick(
                    message = BitwardenString.passkey_operation_failed_because_the_browser_is_not_trusted
                        .asText(),
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `cipher decryption error dialog should be shown or hidden according to the state`() {
        val errorTitle = "Decryption error"
        val errorMessage =
            "Bitwarden could not decrypt this vault item. Copy and share this error report with customer success to avoid additional data loss."
        composeTestRule.assertNoDialogExists()
        composeTestRule
            .onNodeWithText(errorTitle)
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(errorMessage)
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState.DialogState.CipherDecryptionError(
                    title = errorTitle.asText(),
                    message = errorMessage.asText(),
                    selectedCipherId = "1",
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

    @Suppress("MaxLineLength")
    @Test
    fun `share and copy button click on the CipherDecryptionError screen should send ShareCipherDecryptionErrorClick`() {
        val errorTitle = "Decryption error"
        val errorMessage =
            "Bitwarden could not decrypt this vault item. Copy and share this error report with customer success to avoid additional data loss."
        val shareAndCopyText = "Copy error report"
        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState.DialogState.CipherDecryptionError(
                    title = errorTitle.asText(),
                    message = errorMessage.asText(),
                    selectedCipherId = "1",
                ),
            )
        }

        composeTestRule
            .onNodeWithText(shareAndCopyText)
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultItemListingsAction.ShareCipherDecryptionErrorClick("1"),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `close button click on the CipherDecryptionError screen should send DialogDismiss`() {
        val errorTitle = "Decryption error"
        val errorMessage =
            "Bitwarden could not decrypt this vault item. Copy and share this error report with customer success to avoid additional data loss."
        val closeText = "Close"
        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState.DialogState.CipherDecryptionError(
                    title = errorTitle.asText(),
                    message = errorMessage.asText(),
                    selectedCipherId = "1",
                ),
            )
        }

        composeTestRule
            .onNodeWithText(closeText)
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultItemListingsAction.DismissDialogClick,
            )
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

private val ACCOUNT_SUMMARIES = listOf(
    ACTIVE_ACCOUNT_SUMMARY,
    LOCKED_ACCOUNT_SUMMARY,
)

private val AUTOFILL_SELECTION_DATA =
    AutofillSelectionData(
        type = AutofillSelectionData.Type.LOGIN,
        framework = AutofillSelectionData.Framework.AUTOFILL,
        uri = "https:://www.test.com",
    )

private val DEFAULT_STATE = VaultItemListingState(
    itemListingType = VaultItemListingState.ItemListingType.Vault.Login,
    activeAccountSummary = ACTIVE_ACCOUNT_SUMMARY,
    accountSummaries = ACCOUNT_SUMMARIES,
    viewState = VaultItemListingState.ViewState.Loading,
    vaultFilterType = VaultFilterType.AllVaults,
    baseWebSendUrl = Environment.Us.environmentUrlData.baseWebSendUrl,
    isIconLoadingDisabled = false,
    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
    isPullToRefreshSettingEnabled = false,
    dialogState = null,
    policyDisablesSend = false,
    hasMasterPassword = true,
    isPremium = false,
    isRefreshing = false,
    restrictItemTypesPolicyOrgIds = persistentListOf(),
)

private val STATE_FOR_AUTOFILL = DEFAULT_STATE.copy(
    autofillSelectionData = AUTOFILL_SELECTION_DATA,
)

private fun createDisplayItem(number: Int): VaultItemListingState.DisplayItem =
    VaultItemListingState.DisplayItem(
        id = "mockId-$number",
        title = "mockTitle-$number".asText(),
        titleTestTag = "SendNameLabel",
        secondSubtitle = null,
        secondSubtitleTestTag = null,
        subtitle = "mockSubtitle-$number",
        subtitleTestTag = "SendDateLabel",
        iconData = IconData.Local(BitwardenDrawable.ic_payment_card),
        extraIconList = persistentListOf(
            IconData.Local(
                iconRes = BitwardenDrawable.ic_send_disabled,
                contentDescription = BitwardenString.disabled.asText(),
            ),
            IconData.Local(
                iconRes = BitwardenDrawable.ic_key,
                contentDescription = BitwardenString.password.asText(),
            ),
            IconData.Local(
                iconRes = BitwardenDrawable.ic_send_max_access_count_reached,
                contentDescription = BitwardenString.maximum_access_count_reached.asText(),
            ),
            IconData.Local(
                iconRes = BitwardenDrawable.ic_send_expired,
                contentDescription = BitwardenString.expired.asText(),
            ),
            IconData.Local(
                iconRes = BitwardenDrawable.ic_send_pending_delete,
                contentDescription = BitwardenString.pending_delete.asText(),
            ),
        ),
        overflowOptions = listOf(
            ListingItemOverflowAction.SendAction.CopyUrlClick(sendUrl = "www.test.com"),
            ListingItemOverflowAction.SendAction.ShareUrlClick(sendUrl = "www.test.com"),
            ListingItemOverflowAction.SendAction.ViewClick(
                sendId = "mockId-$number",
                sendType = SendType.FILE,
            ),
            ListingItemOverflowAction.SendAction.EditClick(
                sendId = "mockId-$number",
                sendType = SendType.TEXT,
            ),
            ListingItemOverflowAction.SendAction.RemovePasswordClick(sendId = "mockId-$number"),
            ListingItemOverflowAction.SendAction.DeleteClick(sendId = "mockId-$number"),
        ),
        optionsTestTag = "SendOptionsButton",
        isAutofill = false,
        isCredentialCreation = false,
        shouldShowMasterPasswordReprompt = false,
        iconTestTag = null,
        itemType = VaultItemListingState.DisplayItem.ItemType.Sends(type = SendType.TEXT),
    )

private fun createCipherDisplayItem(
    @Suppress("SameParameterValue") number: Int,
): VaultItemListingState.DisplayItem =
    VaultItemListingState.DisplayItem(
        id = "mockId-$number",
        title = "mockTitle-$number".asText(),
        titleTestTag = "CipherNameLabel",
        secondSubtitle = null,
        secondSubtitleTestTag = null,
        subtitle = "mockSubtitle-$number",
        subtitleTestTag = "CipherSubTitleLabel",
        iconData = IconData.Local(BitwardenDrawable.ic_vault),
        extraIconList = persistentListOf(),
        overflowOptions = listOf(
            ListingItemOverflowAction.VaultAction.EditClick(
                cipherId = "mockId-$number",
                cipherType = CipherType.LOGIN,
                requiresPasswordReprompt = true,
            ),
        ),
        optionsTestTag = "CipherOptionsButton",
        isAutofill = false,
        isCredentialCreation = false,
        shouldShowMasterPasswordReprompt = false,
        iconTestTag = null,
        itemType = VaultItemListingState.DisplayItem.ItemType.Vault(
            type = CipherType.LOGIN,
        ),
    )
