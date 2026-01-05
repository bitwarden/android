package com.x8bit.bitwarden.ui.vault.feature.vault

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.core.net.toUri
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.core.data.util.advanceTimeByAndRunCurrent
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.data.repository.util.baseIconUrl
import com.bitwarden.ui.platform.components.account.model.AccountSummary
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.assertLockOrLogoutDialogIsDisplayed
import com.bitwarden.ui.util.assertLogoutConfirmationDialogIsDisplayed
import com.bitwarden.ui.util.assertNoDialogExists
import com.bitwarden.ui.util.assertRemovalConfirmationDialogIsDisplayed
import com.bitwarden.ui.util.assertScrollableNodeDoesNotExist
import com.bitwarden.ui.util.assertSwitcherIsDisplayed
import com.bitwarden.ui.util.assertSwitcherIsNotDisplayed
import com.bitwarden.ui.util.onNodeWithTextAfterScroll
import com.bitwarden.ui.util.performAccountClick
import com.bitwarden.ui.util.performAccountIconClick
import com.bitwarden.ui.util.performAccountLongClick
import com.bitwarden.ui.util.performAddAccountClick
import com.bitwarden.ui.util.performLockAccountClick
import com.bitwarden.ui.util.performLogoutAccountClick
import com.bitwarden.ui.util.performRemoveAccountClick
import com.bitwarden.ui.util.performYesDialogButtonClick
import com.bitwarden.vault.CipherType
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import com.x8bit.bitwarden.ui.platform.manager.review.AppReviewManager
import com.x8bit.bitwarden.ui.vault.components.model.CreateVaultItemType
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditArgs
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemArgs
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterData
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import com.x8bit.bitwarden.ui.vault.model.VaultAddEditType
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@Suppress("LargeClass")
class VaultScreenTest : BitwardenComposeTest() {
    private var onNavigateToAboutCalled = false
    private var onNavigateToAutofillCalled = false
    private var onNavigateToImportLoginsCalled = false
    private var onNavigateToVaultAddItemScreenCalled = false
    private var onNavigateToVaultItemArgs: VaultItemArgs? = null
    private var onNavigateToVaultEditItemArgs: VaultAddEditArgs? = null
    private var onNavigateToVaultItemListingType: VaultItemListingType? = null
    private var onDimBottomNavBarRequestCalled = false
    private var onNavigateToVerificationCodeScreen = false
    private var onNavigateToSearchScreen = false
    private var onNavigateToAddFolderCalled = false
    private var onNavigateToAddFolderParentFolderName: String? = null
    private val intentManager = mockk<IntentManager>(relaxed = true)
    private val appReviewManager: AppReviewManager = mockk {
        every { promptForReview() } just runs
    }
    private val mutableEventFlow = bufferedMutableSharedFlow<VaultEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<VaultViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        setContent(
            intentManager = intentManager,
            appReviewManager = appReviewManager,
        ) {
            VaultScreen(
                viewModel = viewModel,
                onNavigateToVaultAddItemScreen = { onNavigateToVaultAddItemScreenCalled = true },
                onNavigateToVaultItemScreen = { onNavigateToVaultItemArgs = it },
                onNavigateToVaultEditItemScreen = { onNavigateToVaultEditItemArgs = it },
                onNavigateToVaultItemListingScreen = { onNavigateToVaultItemListingType = it },
                onDimBottomNavBarRequest = { onDimBottomNavBarRequestCalled = true },
                onNavigateToVerificationCodeScreen = { onNavigateToVerificationCodeScreen = true },
                onNavigateToSearchVault = { onNavigateToSearchScreen = true },
                onNavigateToImportLogins = { onNavigateToImportLoginsCalled = true },
                onNavigateToAddFolderScreen = { folderName ->
                    onNavigateToAddFolderCalled = true
                    onNavigateToAddFolderParentFolderName = folderName
                },
                onNavigateToAboutScreen = { onNavigateToAboutCalled = true },
                onNavigateToAutofillScreen = { onNavigateToAutofillCalled = true },
            )
        }
    }

    @Test
    fun `app bar title should update according to state`() {
        composeTestRule.onNodeWithText("My vault").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vaults").assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(appBarTitle = BitwardenString.vaults.asText())
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
                    vaultFilterTypes = listOf(
                        VaultFilterType.AllVaults,
                        ORGANIZATION_VAULT_FILTER,
                    ),
                ),
            )
        }

        composeTestRule.onNodeWithText("Vault: All").assertDoesNotExist()
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

        composeTestRule.performYesDialogButtonClick()

        verify { viewModel.trySendAction(VaultAction.LogoutAccountClick(ACTIVE_ACCOUNT_SUMMARY)) }
        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `remove account button click in the lock-or-logout dialog should show the remove account confirmation dialog and hide the lock-or-logout dialog`() {
        // Show the lock-or-logout dialog
        val activeAccountSummary = ACTIVE_ACCOUNT_SUMMARY.copy(isLoggedIn = false)
        mutableStateFlow.update {
            it.copy(accountSummaries = listOf(activeAccountSummary))
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
            it.copy(accountSummaries = listOf(activeAccountSummary))
        }
        composeTestRule.performAccountIconClick()
        composeTestRule.performAccountLongClick(activeAccountSummary)
        composeTestRule.performRemoveAccountClick()

        composeTestRule.performYesDialogButtonClick()

        verify { viewModel.trySendAction(VaultAction.LogoutAccountClick(activeAccountSummary)) }
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `overflow button click should show the overflow menu`() {
        composeTestRule.onNode(isPopup()).assertDoesNotExist()
        composeTestRule.onNodeWithText("Sync").assertDoesNotExist()
        composeTestRule.onNodeWithText("Lock").assertDoesNotExist()

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
    fun `floating action button should be shown or hidden according to the state`() {
        val fabDescription = "Add Item"

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
            .onAllNodesWithText(text = "Okay")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify { viewModel.trySendAction(VaultAction.DialogDismiss) }
    }

    @Test
    fun `ThirdPartyBrowserAutofill should be displayed according to state`() {
        composeTestRule.assertNoDialogExists()
        mutableStateFlow.update {
            it.copy(dialog = VaultState.DialogState.ThirdPartyBrowserAutofill(browserCount = 1))
        }

        composeTestRule
            .onNodeWithText(text = "Enable browser Autofill to keep filling passwords")
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))

        mutableStateFlow.update { it.copy(dialog = null) }
        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ThirdPartyBrowserAutofill dialog Not now button should emit DismissThirdPartyAutofillDialogClick`() {
        mutableStateFlow.update {
            it.copy(dialog = VaultState.DialogState.ThirdPartyBrowserAutofill(browserCount = 2))
        }

        composeTestRule
            .onNodeWithText(text = "Not now")
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(VaultAction.DismissThirdPartyAutofillDialogClick)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `ThirdPartyBrowserAutofill dialog Go to settings now button should emit EnableThirdPartyAutofillClick`() {
        mutableStateFlow.update {
            it.copy(dialog = VaultState.DialogState.ThirdPartyBrowserAutofill(browserCount = 3))
        }

        composeTestRule
            .onNodeWithText(text = "Go to settings")
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        verify(exactly = 1) { viewModel.trySendAction(VaultAction.EnableThirdPartyAutofillClick) }
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
                dialog = VaultState.DialogState.CipherDecryptionError(
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
                dialog = VaultState.DialogState.CipherDecryptionError(
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
                VaultAction.ShareCipherDecryptionErrorClick("1"),
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
                dialog = VaultState.DialogState.CipherDecryptionError(
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
                VaultAction.DialogDismiss,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `vault load cipher decryption error dialog should be shown or hidden according to the state`() {
        val errorTitle = "Decryption error"
        val errorMessage =
            "Bitwarden could not decrypt 1 vault item. Copy and share this error report with customer success to avoid additional data loss."
        composeTestRule.assertNoDialogExists()
        composeTestRule
            .onNodeWithText(errorTitle)
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(errorMessage)
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialog = VaultState.DialogState.VaultLoadCipherDecryptionError(
                    title = errorTitle.asText(),
                    cipherCount = 1,
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
    fun `vault load cipher decryption error dialog should show plural when error is more than one`() {
        val errorTitle = "Decryption error"
        val errorMessage =
            "Bitwarden could not decrypt 3 vault items. Copy and share this error report with customer success to avoid additional data loss."
        composeTestRule.assertNoDialogExists()
        composeTestRule
            .onNodeWithText(errorTitle)
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(errorMessage)
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialog = VaultState.DialogState.VaultLoadCipherDecryptionError(
                    title = errorTitle.asText(),
                    cipherCount = 3,
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
    fun `share and copy button click on the VaultLoadCipherDecryptionError screen should send ShareAllCipherDecryptionErrorsClick`() {
        val errorTitle = "Decryption error"
        val shareAndCopyText = "Copy error report"
        mutableStateFlow.update {
            it.copy(
                dialog = VaultState.DialogState.VaultLoadCipherDecryptionError(
                    title = errorTitle.asText(),
                    cipherCount = 3,
                ),
            )
        }

        composeTestRule
            .onNodeWithText(shareAndCopyText)
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAction.ShareAllCipherDecryptionErrorsClick,
            )
        }
    }

    @Test
    fun `close button click on the VaultLoadCipherDecryptionError should send DialogDismiss`() {
        val errorTitle = "Decryption error"
        val closeText = "Close"
        mutableStateFlow.update {
            it.copy(
                dialog = VaultState.DialogState.VaultLoadCipherDecryptionError(
                    title = errorTitle.asText(),
                    cipherCount = 3,
                ),
            )
        }

        composeTestRule
            .onNodeWithText(closeText)
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAction.DialogDismiss,
            )
        }
    }

    @Test
    fun `vault load KDF update required dialog should be shown or hidden according to the state`() {
        val dialogTitle = "Master Password Update"
        val dialogMessage = "Your master password does not meet the current security requirements."
        composeTestRule.assertNoDialogExists()
        composeTestRule
            .onNodeWithText(dialogTitle)
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(dialogMessage)
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialog = VaultState.DialogState.VaultLoadKdfUpdateRequired(
                    title = dialogTitle.asText(),
                    message = dialogMessage.asText(),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(dialogTitle)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(dialogMessage)
            .assertIsDisplayed()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `confirm button click on the VaultLoadKdfUpdateRequired dialog should send KdfUpdatePasswordRepromptSubmit`() {
        val dialogTitle = "Master Password Update"
        val dialogMessage = "Your master password does not meet the current security requirements."
        val testPassword = "test_password"
        mutableStateFlow.update {
            it.copy(
                dialog = VaultState.DialogState.VaultLoadKdfUpdateRequired(
                    title = dialogTitle.asText(),
                    message = dialogMessage.asText(),
                ),
            )
        }

        // Enter password in the input field
        composeTestRule
            .onNodeWithText("Master password")
            .performTextInput(testPassword)

        // Click confirm button
        composeTestRule
            .onNodeWithText("Submit")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultAction.KdfUpdatePasswordRepromptSubmit(testPassword),
            )
        }
    }

    @Test
    fun `later button click on the VaultLoadKdfUpdateRequired dialog should send DialogDismiss`() {
        val dialogTitle = "Master Password Update"
        val dialogMessage = "Your master password does not meet the current security requirements."
        val laterText = "Later"
        mutableStateFlow.update {
            it.copy(
                dialog = VaultState.DialogState.VaultLoadKdfUpdateRequired(
                    title = dialogTitle.asText(),
                    message = dialogMessage.asText(),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(laterText)
            .performClick()

        verify {
            viewModel.trySendAction(VaultAction.DialogDismiss)
        }
    }

    @Test
    fun `syncing dialog should be displayed according to state`() {
        composeTestRule.assertNoDialogExists()
        composeTestRule.onNodeWithText("Loading").assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(dialog = VaultState.DialogState.Syncing)
        }

        composeTestRule
            .onNodeWithText("Syncing…")
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))
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
    fun `verification code click should call VerificationCodesClick `() {
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                    totpItemsCount = 3,
                ),
                isPremium = true,
            )
        }

        composeTestRule
            .onNodeWithText("Verification codes")
            .performClick()

        verify { viewModel.trySendAction(VaultAction.VerificationCodesClick) }
    }

    @Test
    fun `NavigateToVerificationCodeScreen event should call onNavigateToVerificationCodeScreen`() {
        mutableEventFlow.tryEmit(VaultEvent.NavigateToVerificationCodeScreen)
        assertTrue(onNavigateToVerificationCodeScreen)
    }

    @Test
    fun `search icon click should send SearchIconClick action`() {
        mutableStateFlow.update { it.copy(viewState = VaultState.ViewState.NoItems) }
        composeTestRule.onNodeWithContentDescription("Search vault").performClick()
        verify { viewModel.trySendAction(VaultAction.SearchIconClick) }
    }

    @Test
    fun `floating action button click should send SelectAddItemType action`() {
        mutableStateFlow.update { it.copy(viewState = VaultState.ViewState.NoItems) }
        composeTestRule.onNodeWithContentDescription("Add Item").performClick()
        verify { viewModel.trySendAction(VaultAction.SelectAddItemType) }
    }

    @Test
    fun `add an item button click should send AddItemClick action`() {
        mutableStateFlow.update { it.copy(viewState = VaultState.ViewState.NoItems) }
        composeTestRule
            .onNodeWithText("New login")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(VaultAction.AddItemClick(CreateVaultItemType.LOGIN)) }
    }

    @Test
    fun `NavigateToAddItemScreen event should call onNavigateToVaultAddItemScreen`() {
        mutableEventFlow.tryEmit(
            VaultEvent.NavigateToAddItemScreen(type = VaultItemCipherType.LOGIN),
        )
        assertTrue(onNavigateToVaultAddItemScreenCalled)
    }

    @Test
    fun `NavigateToVaultSearchScreen event should call onNavigateToSearchScreen`() {
        mutableEventFlow.tryEmit(VaultEvent.NavigateToVaultSearchScreen)
        assertTrue(onNavigateToSearchScreen)
    }

    @Test
    fun `NavigateToVaultItem event should call onNavigateToVaultItemScreen`() {
        val id = "id4321"
        val type = VaultItemCipherType.LOGIN
        mutableEventFlow.tryEmit(VaultEvent.NavigateToVaultItem(itemId = id, type = type))
        assertEquals(
            VaultItemArgs(vaultItemId = id, cipherType = type),
            onNavigateToVaultItemArgs,
        )
    }

    @Test
    fun `NavigateToEditVaultItem event should call onNavigateToVaultEditItemScreen`() {
        val id = "id1234"
        val type = VaultItemCipherType.CARD
        mutableEventFlow.tryEmit(
            VaultEvent.NavigateToEditVaultItem(itemId = id, type = type),
        )
        assertEquals(
            VaultAddEditArgs(
                vaultAddEditType = VaultAddEditType.EditItem(vaultItemId = id),
                vaultItemCipherType = type,
            ),
            onNavigateToVaultEditItemArgs,
        )
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
    fun `NavigateToItemListing event for SshKey type should call onNavigateToVaultItemListingType with SshKey type`() {
        mutableEventFlow.tryEmit(VaultEvent.NavigateToItemListing(VaultItemListingType.SshKey))
        assertEquals(VaultItemListingType.SshKey, onNavigateToVaultItemListingType)
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
    fun `NavigateToUrl event should call launchUri`() {
        val url = "www.test.com"
        mutableEventFlow.tryEmit(VaultEvent.NavigateToUrl(url))
        verify(exactly = 1) {
            intentManager.launchUri(url.toUri())
        }
    }

    @Test
    fun `ShowShareSheet event should call shareText`() {
        val text = "share this text"
        mutableEventFlow.tryEmit(VaultEvent.ShowShareSheet(text))
        verify(exactly = 1) {
            intentManager.shareText(text)
        }
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
            .onNodeWithText("TOTP (1)")
            .performScrollTo()
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
            .onNodeWithText("TOTP (1)")
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
            overflowOptions = emptyList(),
            shouldShowMasterPasswordReprompt = false,
            hasDecryptionError = false,
        )
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                    favoriteItems = listOf(vaultItem),
                ),
            )
        }

        // Header
        composeTestRule
            .onNodeWithText("FAVORITES (1)")
            .performScrollTo()
            .assertIsDisplayed()
        // Item
        composeTestRule
            .onNodeWithText(itemText)
            .performScrollTo()
            .assertTextEquals(itemText, username)
            .performClick()
        verify {
            viewModel.trySendAction(VaultAction.VaultItemClick(vaultItem))
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking a favorite item with password prompt should prompt for password before dismissing upon Cancel`() {
        val itemText = "Test Item"
        val userName = "Bitwarden"
        val vaultItem = VaultState.ViewState.VaultItem.Login(
            id = "12345",
            name = itemText.asText(),
            username = userName.asText(),
            overflowOptions = emptyList(),
            shouldShowMasterPasswordReprompt = true,
            hasDecryptionError = false,
        )
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                    favoriteItems = listOf(vaultItem),
                ),
            )
        }

        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(itemText))
        composeTestRule
            .onNodeWithText(text = itemText)
            .assertTextEquals(itemText, userName)
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
            .performClick()

        verify(exactly = 0) {
            viewModel.trySendAction(
                action = VaultAction.MasterPasswordRepromptSubmit(
                    item = vaultItem,
                    password = "",
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking a favorite item with password prompt should prompt for password before sending VaultItemClick upon Submit`() {
        val itemText = "Test Item"
        val userName = "Bitwarden"
        val password = "password1234"
        val vaultItem = VaultState.ViewState.VaultItem.Login(
            id = "12345",
            name = itemText.asText(),
            username = userName.asText(),
            overflowOptions = emptyList(),
            shouldShowMasterPasswordReprompt = true,
            hasDecryptionError = false,
        )
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                    favoriteItems = listOf(vaultItem),
                ),
            )
        }

        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(itemText))
        composeTestRule
            .onNodeWithText(text = itemText)
            .assertTextEquals(itemText, userName)
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
            .performTextInput(text = password)
        composeTestRule
            .onAllNodesWithText(text = "Submit")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(
                action = VaultAction.MasterPasswordRepromptSubmit(
                    item = vaultItem,
                    password = password,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking a favorite item overflow with password prompt should prompt for password before dismissing upon Cancel`() {
        val itemText = "Test Item"
        val userName = "Bitwarden"
        val cipherId = "12345"
        val vaultItem = VaultState.ViewState.VaultItem.Login(
            id = cipherId,
            name = itemText.asText(),
            username = userName.asText(),
            overflowOptions = persistentListOf(
                ListingItemOverflowAction.VaultAction.ViewClick(
                    cipherId = cipherId,
                    cipherType = CipherType.LOGIN,
                    requiresPasswordReprompt = true,
                ),
            ),
            shouldShowMasterPasswordReprompt = true,
            hasDecryptionError = false,
        )
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                    favoriteItems = listOf(vaultItem),
                ),
            )
        }

        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(itemText))
        composeTestRule
            .onNodeWithText(text = itemText)
            .onChildren()
            .filterToOne(hasContentDescription(value = "Options"))
            .performClick()

        composeTestRule
            .onNodeWithText(text = "View")
            .assert(hasAnyAncestor(isDialog()))
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
            .performClick()

        verify(exactly = 0) {
            viewModel.trySendAction(
                action = VaultAction.MasterPasswordRepromptSubmit(
                    item = vaultItem,
                    password = "",
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking a favorite item overflow with password prompt should prompt for password before sending VaultItemClick upon Submit`() {
        val itemText = "Test Item"
        val userName = "Bitwarden"
        val password = "password1234"
        val cipherId = "12345"
        val overflowAction = ListingItemOverflowAction.VaultAction.ViewClick(
            cipherId = cipherId,
            cipherType = CipherType.LOGIN,
            requiresPasswordReprompt = true,
        )
        val vaultItem = VaultState.ViewState.VaultItem.Login(
            id = cipherId,
            name = itemText.asText(),
            username = userName.asText(),
            overflowOptions = persistentListOf(overflowAction),
            shouldShowMasterPasswordReprompt = true,
            hasDecryptionError = false,
        )
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                    favoriteItems = persistentListOf(vaultItem),
                ),
            )
        }

        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(itemText))
        composeTestRule
            .onNodeWithText(text = itemText)
            .onChildren()
            .filterToOne(hasContentDescription(value = "Options"))
            .performClick()

        composeTestRule
            .onNodeWithText(text = "View")
            .assert(hasAnyAncestor(isDialog()))
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
            .performTextInput(text = password)
        composeTestRule
            .onAllNodesWithText(text = "Submit")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(
                action = VaultAction.OverflowMasterPasswordRepromptSubmit(
                    overflowAction = overflowAction,
                    password = password,
                ),
            )
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
        val collectionsHeader = "COLLECTIONS (1)"
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
            .assertTextEquals(collectionsHeader)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTextAfterScroll(collectionName)
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
    fun `clicking a no folder item without password prompt should send VaultItemClick`() {
        val itemText = "Test Item"
        val userName = "BitWarden"
        val vaultItem = VaultState.ViewState.VaultItem.Login(
            id = "12345",
            name = itemText.asText(),
            username = userName.asText(),
            overflowOptions = emptyList(),
            shouldShowMasterPasswordReprompt = false,
            hasDecryptionError = false,
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

    @Suppress("MaxLineLength")
    @Test
    fun `clicking a no folder overflow item with password prompt should prompt for password before dismissing upon Cancel`() {
        val itemText = "Test Item"
        val userName = "Bitwarden"
        val cipherId = "12345"
        val overflowAction = ListingItemOverflowAction.VaultAction.ViewClick(
            cipherId = cipherId,
            cipherType = CipherType.LOGIN,
            requiresPasswordReprompt = true,
        )
        val vaultItem = VaultState.ViewState.VaultItem.Login(
            id = cipherId,
            name = itemText.asText(),
            username = userName.asText(),
            overflowOptions = persistentListOf(overflowAction),
            shouldShowMasterPasswordReprompt = true,
            hasDecryptionError = false,
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
            .onNodeWithText(text = itemText)
            .onChildren()
            .filterToOne(hasContentDescription(value = "Options"))
            .performClick()

        composeTestRule
            .onNodeWithText(text = "View")
            .assert(hasAnyAncestor(isDialog()))
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
            .performClick()

        verify(exactly = 0) {
            viewModel.trySendAction(
                action = VaultAction.OverflowMasterPasswordRepromptSubmit(
                    overflowAction = overflowAction,
                    password = "",
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking a no folder overflow item with password prompt should prompt for password before sending VaultItemClick upon Submit`() {
        val itemText = "Test Item"
        val userName = "Bitwarden"
        val password = "password1234"
        val cipherId = "12345"
        val overflowAction = ListingItemOverflowAction.VaultAction.ViewClick(
            cipherId = cipherId,
            cipherType = CipherType.LOGIN,
            requiresPasswordReprompt = true,
        )
        val vaultItem = VaultState.ViewState.VaultItem.Login(
            id = cipherId,
            name = itemText.asText(),
            username = userName.asText(),
            overflowOptions = persistentListOf(overflowAction),
            shouldShowMasterPasswordReprompt = true,
            hasDecryptionError = false,
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
            .onNodeWithText(text = itemText)
            .onChildren()
            .filterToOne(hasContentDescription(value = "Options"))
            .performClick()

        composeTestRule
            .onNodeWithText(text = "View")
            .assert(hasAnyAncestor(isDialog()))
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
            .performTextInput(text = password)
        composeTestRule
            .onAllNodesWithText(text = "Submit")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(
                action = VaultAction.OverflowMasterPasswordRepromptSubmit(
                    overflowAction = overflowAction,
                    password = password,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking a no folder item with password prompt should prompt for password before dismissing upon Cancel`() {
        val itemText = "Test Item"
        val userName = "Bitwarden"
        val vaultItem = VaultState.ViewState.VaultItem.Login(
            id = "12345",
            name = itemText.asText(),
            username = userName.asText(),
            overflowOptions = emptyList(),
            shouldShowMasterPasswordReprompt = true,
            hasDecryptionError = false,
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
            .onNodeWithText(text = itemText)
            .assertTextEquals(itemText, userName)
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
            .performClick()

        verify(exactly = 0) {
            viewModel.trySendAction(
                action = VaultAction.MasterPasswordRepromptSubmit(
                    item = vaultItem,
                    password = "",
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking a no folder item with password prompt should prompt for password before sending VaultItemClick upon Submit`() {
        val itemText = "Test Item"
        val userName = "Bitwarden"
        val password = "password1234"
        val vaultItem = VaultState.ViewState.VaultItem.Login(
            id = "12345",
            name = itemText.asText(),
            username = userName.asText(),
            overflowOptions = emptyList(),
            shouldShowMasterPasswordReprompt = true,
            hasDecryptionError = false,
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
            .onNodeWithText(text = itemText)
            .assertTextEquals(itemText, userName)
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
            .performTextInput(text = password)
        composeTestRule
            .onAllNodesWithText(text = "Submit")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(
                action = VaultAction.MasterPasswordRepromptSubmit(
                    item = vaultItem,
                    password = password,
                ),
            )
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
    fun `card section should be visible based on state`() {
        mutableStateFlow.update { state ->
            state.copy(
                viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                    cardItemsCount = 1,
                    showCardGroup = true,
                ),
            )
        }

        composeTestRule
            .onNodeWithText("Card")
            .performScrollTo()
            .assertIsDisplayed()

        mutableStateFlow.update { state ->
            state.copy(
                viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                    cardItemsCount = 0,
                    showCardGroup = false,
                ),
            )
        }

        composeTestRule
            .onNodeWithText("Card")
            .assertIsNotDisplayed()
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
        // Header
        composeTestRule
            .onNodeWithTextAfterScroll(text = "TRASH (1)")
            .assertIsDisplayed()
        // Item
        composeTestRule
            .onNodeWithTextAfterScroll(rowText)
            .assertTextEquals(rowText, 0.toString())

        val trashCount = 5
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                    trashItemsCount = trashCount,
                ),
            )
        }

        // Header
        composeTestRule
            .onNodeWithTextAfterScroll(text = "TRASH (1)")
            .assertIsDisplayed()
        // Item
        composeTestRule
            .onNodeWithTextAfterScroll(rowText)
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

    @Test
    fun `action card for importing logins should show based on state`() {
        mutableStateFlow.update {
            it.copy(
                viewState = VaultState.ViewState.NoItems,
            )
        }
        val importSavedLogins = "Import saved logins"
        composeTestRule
            .onNodeWithText(importSavedLogins)
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                viewState = VaultState.ViewState.NoItems,
                showImportActionCard = true,
            )
        }
        composeTestRule
            .onNodeWithText(importSavedLogins)
            .assertIsDisplayed()
    }

    @Test
    fun `when import action card is showing, clicking it should send ImportLoginsClick action`() {
        mutableStateFlow.update {
            it.copy(
                viewState = VaultState.ViewState.NoItems,
                showImportActionCard = true,
            )
        }
        composeTestRule
            .onNodeWithText("Get started")
            .performClick()

        verify { viewModel.trySendAction(VaultAction.ImportActionCardClick) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when import action card is showing, dismissing it should send DismissImportActionCard action`() {
        mutableStateFlow.update {
            it.copy(
                viewState = VaultState.ViewState.NoItems,
                showImportActionCard = true,
            )
        }
        composeTestRule
            .onNodeWithContentDescription("Close")
            .performClick()
        verify { viewModel.trySendAction(VaultAction.DismissImportActionCard) }
    }

    @Test
    fun `when NavigateToImportLogins is sent, it should call onNavigateToImportLogins`() {
        mutableEventFlow.tryEmit(VaultEvent.NavigateToImportLogins)
        assertTrue(onNavigateToImportLoginsCalled)
    }

    @Test
    fun `when NavigateToAbout is sent, it should call onNavigateToAbout`() {
        mutableEventFlow.tryEmit(VaultEvent.NavigateToAbout)
        assertTrue(onNavigateToAboutCalled)
    }

    @Test
    fun `when NavigateToAutofillSettings is sent, it should call onNavigateToAutofillSettings`() {
        mutableEventFlow.tryEmit(VaultEvent.NavigateToAutofillSettings)
        assertTrue(onNavigateToAutofillCalled)
    }

    @Test
    fun `when ShowSnackbar is sent snackbar should be displayed`() {
        val data = BitwardenSnackbarData("message".asText())
        mutableEventFlow.tryEmit(VaultEvent.ShowSnackbar(data))
        composeTestRule.onNodeWithText("message").assertIsDisplayed()
    }

    @Test
    fun `when snackbar is displayed clicking on it should dismiss`() {
        val data = BitwardenSnackbarData("message".asText())
        mutableEventFlow.tryEmit(VaultEvent.ShowSnackbar(data))
        composeTestRule
            .onNodeWithText("message")
            .assertIsDisplayed()
            .performClick()
        composeTestRule
            .onNodeWithText("message")
            .assertIsNotDisplayed()
    }

    @Test
    fun `SSH key group header should display correctly based on state`() {
        val count = 1
        // Verify SSH key group is displayed when showSshKeys is true
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                    sshKeyItemsCount = count,
                ),
            )
        }
        composeTestRule
            .onNodeWithText("SSH key")
            .assertTextEquals("SSH key", count.toString())
            .assertIsDisplayed()
    }

    @Test
    fun `SSH key vault items should display correctly based on state`() {
        // Verify SSH key vault items are displayed when showSshKeys is true
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                    noFolderItems = listOf(
                        VaultState.ViewState.VaultItem.SshKey(
                            id = "mockId",
                            name = "mockSshKey".asText(),
                            overflowOptions = emptyList(),
                            shouldShowMasterPasswordReprompt = false,
                            hasDecryptionError = false,
                        ),
                    ),
                ),
            )
        }
        composeTestRule
            .onNodeWithTextAfterScroll("mockSshKey")
            .assertIsDisplayed()
    }

    @Test
    fun `LifecycleResumed action is sent when the screen is resumed`() {
        verify { viewModel.trySendAction(VaultAction.LifecycleResumed) }
    }

    @Test
    fun `PromptForAppReview triggers app review manager`() {
        mutableEventFlow.tryEmit(VaultEvent.PromptForAppReview)
        dispatcher.advanceTimeByAndRunCurrent(4000L)
        verify(exactly = 1) { appReviewManager.promptForReview() }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `NavigateToAddItemScreen event calls onNavigateToAddFolder callback when cipher item type is FOLDER`() {
        mutableEventFlow.tryEmit(VaultEvent.NavigateToAddFolder)
        assertTrue(onNavigateToAddFolderCalled)
        assertNull(onNavigateToAddFolderParentFolderName)
    }

    @Test
    fun `SelectVaultAddItemType dialog state show vault item type selection dialog`() {
        mutableStateFlow.update {
            it.copy(
                dialog = VaultState.DialogState.SelectVaultAddItemType(
                    persistentListOf(CreateVaultItemType.SSH_KEY),
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
    fun `SelectVaultAddItemType dialog state hide vault item type selection if excluded`() {
        mutableStateFlow.update {
            it.copy(
                dialog = VaultState.DialogState.SelectVaultAddItemType(
                    persistentListOf(
                        CreateVaultItemType.SSH_KEY,
                        CreateVaultItemType.CARD,
                    ),
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

        composeTestRule
            .onAllNodesWithText("Card")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertDoesNotExist()

        composeTestRule
            .onAllNodesWithText("SSH key")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertDoesNotExist()

        composeTestRule
            .onAllNodesWithText("Card")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertDoesNotExist()
    }

    @Test
    fun `when option is selected in SelectVaultAddItemType dialog add item action is sent`() {
        mutableStateFlow.update {
            it.copy(
                dialog = VaultState.DialogState.SelectVaultAddItemType(
                    persistentListOf(CreateVaultItemType.SSH_KEY),
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
            viewModel.trySendAction(VaultAction.DialogDismiss)
            viewModel.trySendAction(
                VaultAction.AddItemClick(
                    CreateVaultItemType.CARD,
                ),
            )
        }
    }

    @Test
    fun `on FlightRecorder Snackbar close click sends the DismissFlightRecorderSnackbar`() =
        runTest {
            mutableStateFlow.update {
                it.copy(
                    flightRecorderSnackBar = BitwardenSnackbarData(
                        message = BitwardenString.flight_recorder_banner_message.asText(
                            "4/12/25",
                            "9:15 AM",
                        ),
                        messageHeader = BitwardenString.flight_recorder_banner_title.asText(),
                        actionLabel = BitwardenString.go_to_settings.asText(),
                        withDismissAction = true,
                    ),
                )
            }

            composeTestRule.onNodeWithText(text = "Flight recorder on").assertIsDisplayed()
            composeTestRule.onNodeWithContentDescription(label = "Close").performClick()
            verify(exactly = 1) {
                viewModel.trySendAction(VaultAction.DismissFlightRecorderSnackbar)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `on FlightRecorder Snackbar go to setting click sends the FlightRecorderGoToSettingsClick`() =
        runTest {
            mutableStateFlow.update {
                it.copy(
                    flightRecorderSnackBar = BitwardenSnackbarData(
                        message = BitwardenString.flight_recorder_banner_message.asText(
                            "4/12/25",
                            "9:15 AM",
                        ),
                        messageHeader = BitwardenString.flight_recorder_banner_title.asText(),
                        actionLabel = BitwardenString.go_to_settings.asText(),
                        withDismissAction = true,
                    ),
                )
            }

            composeTestRule.onNodeWithText(text = "Flight recorder on").assertIsDisplayed()
            composeTestRule.onNodeWithText(text = "Go to settings").performClick()
            verify(exactly = 1) {
                viewModel.trySendAction(VaultAction.FlightRecorderGoToSettingsClick)
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
    appBarTitle = BitwardenString.my_vault.asText(),
    avatarColorString = "#aa00aa",
    initials = "AU",
    accountSummaries = persistentListOf(
        ACTIVE_ACCOUNT_SUMMARY,
        LOCKED_ACCOUNT_SUMMARY,
    ),
    viewState = VaultState.ViewState.Loading,
    isPremium = false,
    isPullToRefreshSettingEnabled = false,
    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
    isIconLoadingDisabled = false,
    hasMasterPassword = true,
    isRefreshing = false,
    showImportActionCard = false,
    flightRecorderSnackBar = null,
    cipherDecryptionFailureIds = persistentListOf(),
    hasShownDecryptionFailureAlert = false,
    restrictItemTypesPolicyOrgIds = emptyList(),
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
    itemTypesCount = 4,
    sshKeyItemsCount = 0,
    showCardGroup = true,
)
