package com.x8bit.bitwarden.ui.vault.feature.item

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onSiblings
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.core.net.toUri
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.util.assertNoDialogExists
import com.x8bit.bitwarden.ui.util.assertScrollableNodeDoesNotExist
import com.x8bit.bitwarden.ui.util.isProgressBar
import com.x8bit.bitwarden.ui.util.onFirstNodeWithTextAfterScroll
import com.x8bit.bitwarden.ui.util.onNodeWithContentDescriptionAfterScroll
import com.x8bit.bitwarden.ui.util.onNodeWithTextAfterScroll
import com.x8bit.bitwarden.ui.vault.feature.item.model.TotpCodeItemData
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
import com.x8bit.bitwarden.ui.vault.model.VaultLinkedFieldType
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
import java.time.Instant

@Suppress("LargeClass")
class VaultItemScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled = false
    private var onNavigateToVaultEditItemId: String? = null
    private var onNavigateToMoveToOrganizationItemId: String? = null
    private var onNavigateToAttachmentsId: String? = null
    private var onNavigateToPasswordHistoryId: String? = null

    private val intentManager = mockk<IntentManager>(relaxed = true)

    private val mutableEventFlow = bufferedMutableSharedFlow<VaultItemEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<VaultItemViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            VaultItemScreen(
                viewModel = viewModel,
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToVaultAddEditItem = { id, _ -> onNavigateToVaultEditItemId = id },
                onNavigateToMoveToOrganization = { id, _ ->
                    onNavigateToMoveToOrganizationItemId = id
                },
                onNavigateToAttachments = { onNavigateToAttachmentsId = it },
                onNavigateToPasswordHistory = { onNavigateToPasswordHistoryId = it },
                intentManager = intentManager,
            )
        }
    }

    //region common
    @Test
    fun `NavigateToEdit event should invoke onNavigateToVaultEditItem`() {
        val id = "id1234"
        mutableEventFlow.tryEmit(VaultItemEvent.NavigateToAddEdit(itemId = id, isClone = false))
        assertEquals(id, onNavigateToVaultEditItemId)
    }

    @Test
    fun `NavigateToMoveToOrganization event should invoke onNavigateToMoveToOrganization`() {
        val id = "id1234"
        mutableEventFlow.tryEmit(VaultItemEvent.NavigateToMoveToOrganization(itemId = id))
        assertEquals(id, onNavigateToMoveToOrganizationItemId)
    }

    @Test
    fun `NavigateToMoveToOrganization event should invoke onNavigateToAttachments`() {
        val id = "id1234"
        mutableEventFlow.tryEmit(VaultItemEvent.NavigateToAttachments(itemId = id))
        assertEquals(id, onNavigateToAttachmentsId)
    }

    @Test
    fun `NavigateToPasswordHistory event should invoke onNavigateToPasswordHistory`() {
        val id = "id1234"
        mutableEventFlow.tryEmit(VaultItemEvent.NavigateToPasswordHistory(itemId = id))
        assertEquals(id, onNavigateToPasswordHistoryId)
    }

    @Test
    fun `on close click should send CloseClick`() {
        composeTestRule.onNodeWithContentDescription(label = "Close").performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.Common.CloseClick)
        }
    }

    @Test
    fun `NavigateBack event should invoke onNavigateBack`() {
        mutableEventFlow.tryEmit(VaultItemEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `NavigateToUri event should invoke launchUri`() {
        val uriString = "http://www.example.com"
        val uri = uriString.toUri()
        every { intentManager.launchUri(uri) } just runs

        mutableEventFlow.tryEmit(VaultItemEvent.NavigateToUri(uriString))

        verify(exactly = 1) {
            intentManager.launchUri(uri)
        }
    }

    @Test
    fun `NavigateToSelectAttachmentSaveLocation should invoke createDocumentIntent`() {
        mutableEventFlow.tryEmit(VaultItemEvent.NavigateToSelectAttachmentSaveLocation("test.mp4"))

        verify(exactly = 1) {
            intentManager.createDocumentIntent("test.mp4")
        }
    }

    @Test
    fun `basic dialog should be displayed according to state`() {
        val message = "message"
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText(message).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(dialog = VaultItemState.DialogState.Generic("message".asText()))
        }

        composeTestRule
            .onNodeWithText(message)
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))
    }

    @Test
    fun `Ok click on generic dialog should emit DismissDialogClick`() {
        mutableStateFlow.update {
            it.copy(dialog = VaultItemState.DialogState.Generic("message".asText()))
        }

        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.Common.DismissDialogClick)
        }
    }

    @Test
    fun `loading dialog should be displayed according to state`() {
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText("Loading").assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(dialog = VaultItemState.DialogState.Loading(R.string.loading.asText()))
        }

        composeTestRule
            .onNodeWithText("Loading")
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))
    }

    @Test
    fun `MasterPassword dialog should be displayed according to state`() {
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText("Master password confirmation").assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialog = VaultItemState.DialogState.MasterPasswordDialog(
                    action = PasswordRepromptAction.DeleteClick,
                ),
            )
        }

        composeTestRule
            .onNodeWithText("Master password confirmation")
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))
    }

    @Test
    fun `Ok click on master password dialog should emit DismissDialogClick`() {
        val enteredPassword = "pass1234"
        val passwordRepromptAction = PasswordRepromptAction.EditClick
        mutableStateFlow.update {
            it.copy(
                dialog = VaultItemState.DialogState.MasterPasswordDialog(
                    action = passwordRepromptAction,
                ),
            )
        }

        composeTestRule.onNodeWithText("Master password").performTextInput(enteredPassword)
        composeTestRule
            .onAllNodesWithText("Submit")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultItemAction.Common.MasterPasswordSubmit(
                    masterPassword = enteredPassword,
                    action = passwordRepromptAction,
                ),
            )
        }
    }

    @Test
    fun `name should be displayed according to state`() {
        EMPTY_VIEW_STATES
            .forEach { typeState ->
                mutableStateFlow.update { it.copy(viewState = typeState) }

                composeTestRule
                    .onNodeWithTextAfterScroll("Name")
                    .assertTextContains("cipher")

                mutableStateFlow.update { currentState ->
                    updateCommonContent(currentState) { copy(name = "Test Name") }
                }

                composeTestRule
                    .onNodeWithTextAfterScroll("Name")
                    .assertTextContains("Test Name")
            }
    }

    @Test
    fun `lastUpdated should be displayed according to state`() {
        EMPTY_VIEW_STATES
            .forEach { typeState ->
                mutableStateFlow.update { it.copy(viewState = typeState) }

                composeTestRule
                    .onNodeWithTextAfterScroll("Updated: ")
                    .assertTextContains("12/31/69 06:16 PM")

                mutableStateFlow.update { currentState ->
                    updateCommonContent(currentState) { copy(lastUpdated = "12/31/69 06:20 PM") }
                }

                composeTestRule
                    .onNodeWithTextAfterScroll("Updated: ")
                    .assertTextContains("12/31/69 06:20 PM")
            }
    }

    @Test
    fun `notes should be displayed according to state`() {
        DEFAULT_VIEW_STATES
            .forEach { typeState ->

                mutableStateFlow.update { it.copy(viewState = typeState) }

                composeTestRule.onFirstNodeWithTextAfterScroll("Notes").assertIsDisplayed()
                composeTestRule.onNodeWithTextAfterScroll("Lots of notes").assertIsDisplayed()

                mutableStateFlow.update { currentState ->
                    updateCommonContent(currentState) { copy(notes = null) }
                }

                composeTestRule.assertScrollableNodeDoesNotExist("Notes")
                composeTestRule.assertScrollableNodeDoesNotExist("Lots of notes")
            }
    }

    @Test
    fun `custom views should be displayed according to state`() {
        DEFAULT_VIEW_STATES
            .forEach { typeState ->
                mutableStateFlow.update { it.copy(viewState = typeState) }
                composeTestRule.onNodeWithTextAfterScroll("CUSTOM FIELDS").assertIsDisplayed()
                composeTestRule.onNodeWithTextAfterScroll("text").assertIsDisplayed()
                composeTestRule.onNodeWithTextAfterScroll("value").assertIsDisplayed()
                composeTestRule.onNodeWithTextAfterScroll("hidden").assertIsDisplayed()
                composeTestRule.onNodeWithTextAfterScroll("boolean").assertIsDisplayed()

                mutableStateFlow.update { currentState ->
                    updateCommonContent(currentState) { copy(customFields = emptyList()) }
                }

                composeTestRule.assertScrollableNodeDoesNotExist("CUSTOM FIELDS")
                composeTestRule.assertScrollableNodeDoesNotExist("text")
                composeTestRule.assertScrollableNodeDoesNotExist("value")
                composeTestRule.assertScrollableNodeDoesNotExist("hidden")
                composeTestRule.assertScrollableNodeDoesNotExist("boolean")
            }
    }

    @Test
    fun `attachments should be displayed according to state`() {
        DEFAULT_VIEW_STATES
            .forEach { typeState ->
                mutableStateFlow.update { it.copy(viewState = typeState) }
                composeTestRule.onNodeWithTextAfterScroll("ATTACHMENTS").assertIsDisplayed()
                composeTestRule.onNodeWithTextAfterScroll("test.mp4").assertIsDisplayed()
                composeTestRule.onNodeWithTextAfterScroll("11 MB").assertIsDisplayed()

                mutableStateFlow.update { currentState ->
                    updateCommonContent(currentState) { copy(attachments = emptyList()) }
                }

                composeTestRule.assertScrollableNodeDoesNotExist("ATTACHMENTS")
                composeTestRule.assertScrollableNodeDoesNotExist("test.mp4")
                composeTestRule.assertScrollableNodeDoesNotExist("11 MB")
            }
    }

    @Test
    fun `attachment download click for non-premium users should show an error dialog`() {
        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = EMPTY_LOGIN_VIEW_STATE.copy(
                    common = EMPTY_COMMON.copy(
                        attachments = listOf(
                            VaultItemState.ViewState.Content.Common.AttachmentItem(
                                id = "attachment-id",
                                displaySize = "11 MB",
                                isLargeFile = true,
                                isDownloadAllowed = false,
                                url = "https://example.com",
                                title = "test.mp4",
                            ),
                        ),
                    ),
                ),
            )
        }

        composeTestRule.assertNoDialogExists()
        composeTestRule.onNodeWithContentDescriptionAfterScroll("Download").performClick()

        composeTestRule
            .onAllNodesWithText(
                "A premium membership is required to use this feature.",
            )
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `attachment download click for large downloads should show a prompt and dismiss when clicking No`() {
        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = EMPTY_LOGIN_VIEW_STATE.copy(
                    common = EMPTY_COMMON.copy(
                        attachments = listOf(
                            VaultItemState.ViewState.Content.Common.AttachmentItem(
                                id = "attachment-id",
                                displaySize = "11 MB",
                                isLargeFile = true,
                                isDownloadAllowed = true,
                                url = "https://example.com",
                                title = "test.mp4",
                            ),
                        ),
                    ),
                ),
            )
        }

        composeTestRule.assertNoDialogExists()
        composeTestRule.onNodeWithContentDescriptionAfterScroll("Download").performClick()

        composeTestRule
            .onAllNodesWithText(
                "This attachment is 11 MB in size. Are you sure you want to download it onto " +
                    "your device?",
            )
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("No")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `attachment download click for large downloads should show a prompt and emit AttachmentDownloadClick`() {
        val attachment = VaultItemState.ViewState.Content.Common.AttachmentItem(
            id = "attachment-id",
            displaySize = "11 MB",
            isLargeFile = true,
            isDownloadAllowed = true,
            url = "https://example.com",
            title = "test.mp4",
        )
        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = EMPTY_LOGIN_VIEW_STATE.copy(
                    common = EMPTY_COMMON.copy(
                        attachments = listOf(attachment),
                    ),
                ),
            )
        }

        composeTestRule.assertNoDialogExists()
        composeTestRule.onNodeWithContentDescriptionAfterScroll("Download").performClick()

        composeTestRule
            .onAllNodesWithText(
                "This attachment is 11 MB in size. Are you sure you want to download it onto " +
                    "your device?",
            )
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Yes")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule.assertNoDialogExists()

        verify {
            viewModel.trySendAction(VaultItemAction.Common.AttachmentDownloadClick(attachment))
        }
    }

    @Test
    fun `attachment download click for smaller downloads should emit AttachmentDownloadClick`() {
        val attachment = VaultItemState.ViewState.Content.Common.AttachmentItem(
            id = "attachment-id",
            displaySize = "9 MB",
            isLargeFile = false,
            isDownloadAllowed = true,
            url = "https://example.com",
            title = "test.mp4",
        )
        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = EMPTY_LOGIN_VIEW_STATE.copy(
                    common = EMPTY_COMMON.copy(
                        attachments = listOf(attachment),
                    ),
                ),
            )
        }

        composeTestRule.assertNoDialogExists()
        composeTestRule.onNodeWithContentDescriptionAfterScroll("Download").performClick()

        composeTestRule.assertNoDialogExists()

        verify {
            viewModel.trySendAction(VaultItemAction.Common.AttachmentDownloadClick(attachment))
        }
    }

    @Test
    fun `on show hidden field click should send HiddenFieldVisibilityClicked`() {
        val textField = VaultItemState.ViewState.Content.Common.Custom.HiddenField(
            name = "hidden",
            value = "hidden password",
            isCopyable = true,
            isVisible = false,
        )

        EMPTY_VIEW_STATES
            .forEach { typeState ->
                mutableStateFlow.update { currentState ->
                    currentState.copy(
                        viewState = typeState.copy(
                            common = EMPTY_COMMON.copy(
                                customFields = listOf(textField),
                            ),
                        ),
                    )
                }

                composeTestRule
                    .onNodeWithTextAfterScroll(textField.name)
                    .onChildren()
                    .filterToOne(hasContentDescription("Show"))
                    .performClick()

                verify {
                    viewModel.trySendAction(
                        VaultItemAction.Common.HiddenFieldVisibilityClicked(
                            field = textField,
                            isVisible = true,
                        ),
                    )
                }
            }
    }

    @Test
    fun `copy hidden field button should be displayed according to state`() {
        val hiddenField = VaultItemState.ViewState.Content.Common.Custom.HiddenField(
            name = "hidden",
            value = "hidden password",
            isCopyable = true,
            isVisible = false,
        )

        EMPTY_VIEW_STATES
            .forEach { typeState ->
                mutableStateFlow.update { currentState ->
                    currentState.copy(
                        viewState = typeState.copy(
                            common = EMPTY_COMMON.copy(
                                customFields = listOf(hiddenField),
                            ),
                        ),
                    )
                }

                composeTestRule
                    .onNodeWithTextAfterScroll(hiddenField.name)
                    .onSiblings()
                    .filterToOne(hasContentDescription("Copy"))
                    .assertIsDisplayed()

                mutableStateFlow.update { currentState ->
                    updateCommonContent(currentState) {
                        copy(customFields = listOf(hiddenField.copy(isCopyable = false)))
                    }
                }

                composeTestRule
                    .onNodeWithTextAfterScroll(hiddenField.name)
                    .onSiblings()
                    .filterToOne(hasContentDescription("Copy"))
                    .assertDoesNotExist()
            }
    }

    @Test
    fun `on copy hidden field click should send CopyCustomHiddenFieldClick`() {
        val hiddenField = VaultItemState.ViewState.Content.Common.Custom.HiddenField(
            name = "hidden",
            value = "hidden password",
            isCopyable = true,
            isVisible = false,
        )

        EMPTY_VIEW_STATES
            .forEach { typeState ->
                mutableStateFlow.update { currentState ->
                    currentState.copy(
                        viewState = typeState.copy(
                            common = EMPTY_COMMON.copy(
                                customFields = listOf(hiddenField),
                            ),
                        ),
                    )
                }

                composeTestRule
                    .onNodeWithTextAfterScroll(hiddenField.name)
                    .onSiblings()
                    .filterToOne(hasContentDescription("Copy"))
                    .performClick()

                verify {
                    viewModel.trySendAction(
                        VaultItemAction.Common.CopyCustomHiddenFieldClick(hiddenField.value),
                    )
                }
            }
    }

    @Test
    fun `on copy text field click should send CopyCustomTextFieldClick`() {
        val textField = VaultItemState.ViewState.Content.Common.Custom.TextField(
            name = "text",
            value = "value",
            isCopyable = true,
        )

        EMPTY_VIEW_STATES
            .forEach { typeState ->
                mutableStateFlow.update { currentState ->
                    currentState.copy(
                        viewState = typeState.copy(
                            common = EMPTY_COMMON.copy(
                                customFields = listOf(textField),
                            ),
                        ),
                    )
                }

                composeTestRule
                    .onNodeWithTextAfterScroll(textField.name)
                    .onSiblings()
                    .filterToOne(hasContentDescription("Copy"))
                    .performClick()

                verify {
                    viewModel.trySendAction(
                        VaultItemAction.Common.CopyCustomTextFieldClick(textField.value),
                    )
                }
            }
    }

    @Test
    fun `text field copy button should be displayed according to state`() {
        val textField = VaultItemState.ViewState.Content.Common.Custom.TextField(
            name = "text",
            value = "value",
            isCopyable = true,
        )

        EMPTY_VIEW_STATES
            .forEach { typeState ->
                mutableStateFlow.update { currentState ->
                    currentState.copy(
                        viewState = typeState.copy(
                            common = EMPTY_COMMON.copy(
                                customFields = listOf(textField),
                            ),
                        ),
                    )
                }

                composeTestRule
                    .onNodeWithTextAfterScroll(textField.name)
                    .onSiblings()
                    .filterToOne(hasContentDescription("Copy"))
                    .assertIsDisplayed()

                mutableStateFlow.update { currentState ->
                    updateCommonContent(currentState) {
                        copy(customFields = listOf(textField.copy(isCopyable = false)))
                    }
                }

                composeTestRule
                    .onNodeWithTextAfterScroll(textField.name)
                    .onSiblings()
                    .filterToOne(hasContentDescription("Copy"))
                    .assertDoesNotExist()
            }
    }

    @Test
    fun `in login state the password should change according to state`() {
        composeTestRule.assertScrollableNodeDoesNotExist("Password")

        mutableStateFlow.update {
            it.copy(
                viewState = EMPTY_LOGIN_VIEW_STATE.copy(
                    type = EMPTY_LOGIN_TYPE.copy(
                        passwordData = VaultItemState.ViewState.Content.ItemType.Login.PasswordData(
                            password = "p@ssw0rd",
                            isVisible = false,
                            canViewPassword = true,
                        ),
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithTextAfterScroll("Password")
            .assertTextEquals("Password", "••••••••")
            .assertIsEnabled()
        composeTestRule
            .onNodeWithContentDescription("Check known data breaches for this password")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Copy password")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Show")
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                viewState = EMPTY_LOGIN_VIEW_STATE.copy(
                    type = EMPTY_LOGIN_TYPE.copy(
                        passwordData = VaultItemState.ViewState.Content.ItemType.Login.PasswordData(
                            password = "p@ssw0rd",
                            isVisible = true,
                            canViewPassword = true,
                        ),
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithText("Password")
            .assertTextEquals("Password", "p@ssw0rd")
            .assertIsEnabled()
        composeTestRule
            .onNodeWithContentDescription("Check known data breaches for this password")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Copy password")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Hide")
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                viewState = EMPTY_LOGIN_VIEW_STATE.copy(
                    type = EMPTY_LOGIN_TYPE.copy(
                        passwordData = VaultItemState.ViewState.Content.ItemType.Login.PasswordData(
                            password = "p@ssw0rd",
                            isVisible = true,
                            canViewPassword = false,
                        ),
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithText("Password")
            .assertTextEquals("Password", "••••••••")
            .assertIsNotEnabled()
        composeTestRule
            .onNodeWithContentDescription("Check known data breaches for this password")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithContentDescription("Copy password")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithContentDescription("Hide")
            .assertDoesNotExist()
    }

    @Test
    fun `menu Delete option should be displayed based on state`() {
        // Confirm overflow is closed on initial load
        composeTestRule
            .onAllNodesWithText("Delete")
            .filter(hasAnyAncestor(isPopup()))
            .assertCountEquals(0)

        // Open the overflow menu
        composeTestRule
            .onNodeWithContentDescription("More")
            .performClick()

        // Confirm Delete option is present
        mutableStateFlow.update { it.copy(viewState = DEFAULT_LOGIN_VIEW_STATE) }
        composeTestRule
            .onAllNodesWithText("Delete")
            .filterToOne(hasAnyAncestor(isPopup()))
            .assertIsDisplayed()

        // Confirm Delete option is not present when canDelete is false
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_LOGIN_VIEW_STATE
                    .copy(
                        common = DEFAULT_COMMON
                            .copy(canDelete = false),
                    ),
            )
        }
        composeTestRule
            .onAllNodesWithText("Delete")
            .filter(hasAnyAncestor(isPopup()))
            .assertCountEquals(0)
    }

    @Test
    fun `Delete dialog ok click should send ConfirmDeleteClick`() {
        mutableStateFlow.update {
            it.copy(
                dialog = VaultItemState
                    .DialogState
                    .DeleteConfirmationPrompt("TestText".asText()),
            )
        }

        composeTestRule
            .onAllNodesWithText("Delete")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Ok")
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.Common.ConfirmDeleteClick)
        }
    }

    @Test
    fun `Delete Confirmation dialog text should display according to state`() {
        mutableStateFlow.update {
            it.copy(
                dialog = VaultItemState
                    .DialogState
                    .DeleteConfirmationPrompt("TestText".asText()),
            )
        }

        composeTestRule
            .onAllNodesWithText("Delete")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(dialog = null)
        }

        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `Clicking Restore should send RestoreVaultItemClick ViewModel action`() {
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_IDENTITY_VIEW_STATE
                    .copy(
                        common = DEFAULT_COMMON
                            .copy(
                                currentCipher = createMockCipherView(1).copy(
                                    deletedDate = Instant.MIN,
                                ),
                            ),
                    ),
            )
        }

        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onNodeWithText("Restore")
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultItemAction.Common.RestoreVaultItemClick,
            )
        }
    }

    @Test
    fun `Restore dialog should display correctly when dialog state changes`() {
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_IDENTITY_VIEW_STATE
                    .copy(
                        common = DEFAULT_COMMON
                            .copy(
                                currentCipher = createMockCipherView(1).copy(
                                    deletedDate = Instant.MIN,
                                ),
                            ),
                    ),
            )
        }

        composeTestRule.assertNoDialogExists()

        mutableStateFlow.update {
            it.copy(dialog = VaultItemState.DialogState.RestoreItemDialog)
        }

        composeTestRule
            .onAllNodesWithText("Do you really want to restore this item?")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Restore")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `Restore dialog should hide restore confirmation menu if dialog state changes`() {
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_IDENTITY_VIEW_STATE
                    .copy(
                        common = DEFAULT_COMMON
                            .copy(
                                currentCipher = createMockCipherView(1).copy(
                                    deletedDate = Instant.MIN,
                                ),
                            ),
                    ),
            )
        }

        composeTestRule.assertNoDialogExists()

        mutableStateFlow.update {
            it.copy(dialog = VaultItemState.DialogState.RestoreItemDialog)
        }

        composeTestRule
            .onAllNodesWithText("Do you really want to restore this item?")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Restore")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(dialog = null)
        }

        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `Restore dialog ok click should send ConfirmRestoreClick`() {
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_IDENTITY_VIEW_STATE
                    .copy(
                        common = DEFAULT_COMMON
                            .copy(
                                currentCipher = createMockCipherView(1).copy(
                                    deletedDate = Instant.MIN,
                                ),
                            ),
                    ),
            )
        }

        composeTestRule.assertNoDialogExists()

        mutableStateFlow.update {
            it.copy(dialog = VaultItemState.DialogState.RestoreItemDialog)
        }

        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.Common.ConfirmRestoreClick)
        }
    }

    @Test
    fun `Restore dialog cancel click should send DismissDialogClick`() {
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_IDENTITY_VIEW_STATE
                    .copy(
                        common = DEFAULT_COMMON
                            .copy(
                                currentCipher = createMockCipherView(1).copy(
                                    deletedDate = Instant.MIN,
                                ),
                            ),
                    ),
            )
        }

        composeTestRule.assertNoDialogExists()

        mutableStateFlow.update {
            it.copy(dialog = VaultItemState.DialogState.RestoreItemDialog)
        }

        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.Common.DismissDialogClick)
        }
    }

    @Test
    fun `Attachments option menu click should send AttachmentsClick action`() {
        // Confirm dropdown version of item is absent
        composeTestRule
            .onAllNodesWithText("Attachments")
            .filter(hasAnyAncestor(isPopup()))
            .assertCountEquals(0)
        // Open the overflow menu
        composeTestRule.onNodeWithContentDescription("More").performClick()
        // Click on the attachments hint item in the dropdown
        composeTestRule
            .onAllNodesWithText("Attachments")
            .filterToOne(hasAnyAncestor(isPopup()))
            .performClick()
        verify {
            viewModel.trySendAction(VaultItemAction.Common.AttachmentsClick)
        }
    }

    @Test
    fun `Clone option menu click should send CloneClick action`() {
        // Confirm dropdown version of item is absent
        composeTestRule
            .onAllNodesWithText("Clone")
            .filter(hasAnyAncestor(isPopup()))
            .assertCountEquals(0)
        // Open the overflow menu
        composeTestRule.onNodeWithContentDescription("More").performClick()
        // Click on the clone item in the dropdown
        composeTestRule
            .onAllNodesWithText("Clone")
            .filterToOne(hasAnyAncestor(isPopup()))
            .performClick()
        verify {
            viewModel.trySendAction(VaultItemAction.Common.CloneClick)
        }
    }

    @Test
    fun `Move to organization option menu click should send MoveToOrganizationClick action`() {
        // Confirm dropdown version of item is absent
        composeTestRule
            .onAllNodesWithText("Move to Organization")
            .filter(hasAnyAncestor(isPopup()))
            .assertCountEquals(0)
        // Open the overflow menu
        composeTestRule.onNodeWithContentDescription("More").performClick()
        // Click on the move to organization hint item in the dropdown
        composeTestRule
            .onAllNodesWithText("Move to Organization")
            .filterToOne(hasAnyAncestor(isPopup()))
            .performClick()
        verify {
            viewModel.trySendAction(VaultItemAction.Common.MoveToOrganizationClick)
        }
    }

    @Test
    fun `menu Collection option should be displayed based on state`() {
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_IDENTITY_VIEW_STATE
                    .copy(
                        common = DEFAULT_COMMON
                            .copy(currentCipher = createMockCipherView(1)),
                    ),
            )
        }
        // Confirm overflow is closed on initial load
        composeTestRule
            .onAllNodesWithText("Collections")
            .filter(hasAnyAncestor(isPopup()))
            .assertCountEquals(0)

        // Open the overflow menu
        composeTestRule
            .onNodeWithContentDescription("More")
            .performClick()

        // Confirm Collections option is present
        composeTestRule
            .onAllNodesWithText("Collections")
            .filterToOne(hasAnyAncestor(isPopup()))
            .assertIsDisplayed()

        // Confirm Collections option is not present when canDelete is false
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_IDENTITY_VIEW_STATE
                    .copy(
                        common = DEFAULT_COMMON
                            .copy(canAssignToCollections = false),
                    ),
            )
        }
        composeTestRule
            .onAllNodesWithText("Collections")
            .filter(hasAnyAncestor(isPopup()))
            .assertCountEquals(0)
    }

    @Test
    fun `Collections menu click should send CollectionsClick action`() {
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_IDENTITY_VIEW_STATE
                    .copy(
                        common = DEFAULT_COMMON
                            .copy(currentCipher = createMockCipherView(1)),
                    ),
            )
        }
        // Confirm dropdown version of item is absent
        composeTestRule
            .onAllNodesWithText("Collections")
            .filter(hasAnyAncestor(isPopup()))
            .assertCountEquals(0)
        // Open the overflow menu
        composeTestRule
            .onNodeWithContentDescription("More")
            .performClick()
        // Click on the move to organization hint item in the dropdown
        composeTestRule
            .onAllNodesWithText("Collections")
            .filterToOne(hasAnyAncestor(isPopup()))
            .performClick()

        composeTestRule
            .onNode(isPopup())
            .assertDoesNotExist()

        verify {
            viewModel.trySendAction(VaultItemAction.Common.CollectionsClick)
        }
    }

    @Test
    fun `Menu should display correct items when cipher is in a collection`() {
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_IDENTITY_VIEW_STATE
                    .copy(
                        common = DEFAULT_COMMON
                            .copy(currentCipher = createMockCipherView(1)),
                    ),
            )
        }
        composeTestRule
            .onNodeWithContentDescription("More")
            .performClick()

        composeTestRule
            .onAllNodesWithText("Attachments")
            .filterToOne(hasAnyAncestor(isPopup()))
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Collections")
            .filterToOne(hasAnyAncestor(isPopup()))
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Delete")
            .filterToOne(hasAnyAncestor(isPopup()))
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Move to Organization")
            .filterToOne(hasAnyAncestor(isPopup()))
            .assertDoesNotExist()

        composeTestRule
            .onAllNodesWithText("Clone")
            .filterToOne(hasAnyAncestor(isPopup()))
            .assertDoesNotExist()
    }

    @Test
    fun `Menu should display correct items when cipher is not in a collection`() {
        mutableStateFlow.update { it.copy(viewState = DEFAULT_LOGIN_VIEW_STATE) }
        composeTestRule
            .onNodeWithContentDescription("More")
            .performClick()

        composeTestRule
            .onAllNodesWithText("Attachments")
            .filterToOne(hasAnyAncestor(isPopup()))
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Clone")
            .filterToOne(hasAnyAncestor(isPopup()))
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Move to Organization")
            .filterToOne(hasAnyAncestor(isPopup()))
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Delete")
            .filterToOne(hasAnyAncestor(isPopup()))
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Collections")
            .filterToOne(hasAnyAncestor(isPopup()))
            .assertDoesNotExist()
    }

    @Test
    fun `on login copy notes field click should send CopyNotesClick`() {

        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = DEFAULT_LOGIN_VIEW_STATE,
            )
        }
        composeTestRule.onNodeWithTextAfterScroll("Lots of notes")
        composeTestRule
            .onNodeWithTag("CipherNotesCopyButton")
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.Common.CopyNotesClick)
        }
    }

    @Test
    fun `on identity copy notes field click should send CopyNotesClick`() {
        // Adding a custom field so that we can scroll to it
        // So we can see the Copy notes button but not have it covered by the FAB
        val textField = VaultItemState.ViewState.Content.Common.Custom.TextField(
            name = "text",
            value = "value",
            isCopyable = true,
        )

        EMPTY_VIEW_STATES
            .forEach { typeState ->
                mutableStateFlow.update { currentState ->
                    currentState.copy(
                        viewState = typeState.copy(
                            type = DEFAULT_IDENTITY,
                            common = EMPTY_COMMON.copy(
                                notes = "this is a note",
                                customFields = listOf(textField),
                            ),
                        ),
                    )
                }

                composeTestRule.onNodeWithTextAfterScroll(textField.name)
                composeTestRule
                    .onNodeWithTag("CipherNotesCopyButton")
                    .performClick()

                verify {
                    viewModel.trySendAction(VaultItemAction.Common.CopyNotesClick)
                }
            }
    }

    @Test
    fun `on card copy notes field click should send CopyNotesClick`() {
        // Adding a custom field so that we can scroll to it
        // So we can see the Copy notes button but not have it covered by the FAB
        val textField = VaultItemState.ViewState.Content.Common.Custom.TextField(
            name = "text",
            value = "value",
            isCopyable = true,
        )

        EMPTY_VIEW_STATES
            .forEach { typeState ->
                mutableStateFlow.update { currentState ->
                    currentState.copy(
                        viewState = typeState.copy(
                            type = DEFAULT_IDENTITY,
                            common = EMPTY_COMMON.copy(
                                notes = "this is a note",
                                customFields = listOf(textField),
                            ),
                        ),
                    )
                }
            }

        composeTestRule.onNodeWithTextAfterScroll(textField.name)

        composeTestRule
            .onNodeWithTag("CipherNotesCopyButton")
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.Common.CopyNotesClick)
        }
    }

    @Test
    fun `on secure note copy notes field click should send CopyNotesClick`() {

        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = DEFAULT_SECURE_NOTE_VIEW_STATE,
            )
        }
        composeTestRule.onNodeWithTextAfterScroll("Lots of notes")

        composeTestRule
            .onNodeWithTag("CipherNotesCopyButton")
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.Common.CopyNotesClick)
        }
    }

    @Test
    fun `on ssh key copy notes field click should send CopyNotesClick`() {
        // Adding a custom field so that we can scroll to it
        // So we can see the Copy notes button but not have it covered by the FAB
        val textField = VaultItemState.ViewState.Content.Common.Custom.TextField(
            name = "text",
            value = "value",
            isCopyable = true,
        )

        EMPTY_VIEW_STATES
            .forEach { typeState ->
                mutableStateFlow.update { currentState ->
                    currentState.copy(
                        viewState = typeState.copy(
                            type = DEFAULT_SSH_KEY,
                            common = EMPTY_COMMON.copy(
                                notes = "this is a note",
                                customFields = listOf(textField),
                            ),
                        ),
                    )
                }
            }

        composeTestRule.onNodeWithTextAfterScroll(textField.name)

        composeTestRule
            .onNodeWithTag("CipherNotesCopyButton")
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.Common.CopyNotesClick)
        }
    }
    //endregion common

    //region login
    @Test
    fun `in login state, linked custom fields should be displayed according to state`() {
        val linkedFieldUserName =
            VaultItemState.ViewState.Content.Common.Custom.LinkedField(
                name = "linked username",
                vaultLinkedFieldType = VaultLinkedFieldType.USERNAME,
            )

        val linkedFieldsPassword = VaultItemState.ViewState.Content.Common.Custom.LinkedField(
            name = "linked password",
            vaultLinkedFieldType = VaultLinkedFieldType.PASSWORD,
        )

        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = EMPTY_LOGIN_VIEW_STATE.copy(
                    common = EMPTY_COMMON.copy(
                        customFields = listOf(linkedFieldUserName, linkedFieldsPassword),
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithTextAfterScroll(linkedFieldsPassword.name)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTextAfterScroll(linkedFieldUserName.name)
            .assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = EMPTY_LOGIN_VIEW_STATE.copy(
                    common = EMPTY_COMMON.copy(
                        customFields = listOf(),
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(linkedFieldsPassword.name)
            .assertDoesNotExist()

        composeTestRule
            .onNodeWithText(linkedFieldUserName.name)
            .assertDoesNotExist()
    }

    @Test
    fun `in login state, on username copy click should send CopyUsernameClick`() {
        val username = "username1234"
        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = EMPTY_LOGIN_VIEW_STATE.copy(
                    type = VaultItemState.ViewState.Content.ItemType.Login(
                        username = username,
                        passwordData = null,
                        passwordHistoryCount = null,
                        uris = emptyList(),
                        passwordRevisionDate = null,
                        isPremiumUser = true,
                        canViewTotpCode = true,
                        totpCodeItemData = null,
                        fido2CredentialCreationDateText = null,
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithTextAfterScroll(username)
            .onSiblings()
            .filterToOne(hasContentDescription("Copy username"))
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.ItemType.Login.CopyUsernameClick)
        }
    }

    @Test
    fun `in login state, on breach check click should send CheckForBreachClick`() {
        val passwordData = VaultItemState.ViewState.Content.ItemType.Login.PasswordData(
            password = "12345",
            isVisible = true,
            canViewPassword = true,
        )
        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = EMPTY_LOGIN_VIEW_STATE.copy(
                    type = EMPTY_LOGIN_TYPE.copy(
                        passwordData = passwordData,
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithTextAfterScroll(passwordData.password)
            .onSiblings()
            .filterToOne(hasContentDescription("Check known data breaches for this password"))
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.ItemType.Login.CheckForBreachClick)
        }
    }

    @Test
    fun `in login state, on show password click should send PasswordVisibilityClicked`() {
        mutableStateFlow.update { it.copy(viewState = DEFAULT_LOGIN_VIEW_STATE) }

        composeTestRule
            .onNodeWithTextAfterScroll("Password")
            .onChildren()
            .filterToOne(hasContentDescription("Show"))
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(VaultItemAction.ItemType.Login.PasswordVisibilityClicked(true))
        }
    }

    @Test
    fun `in login state, on copy password click should send CopyPasswordClick`() {
        val passwordData = VaultItemState.ViewState.Content.ItemType.Login.PasswordData(
            password = "12345",
            isVisible = true,
            canViewPassword = true,
        )
        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = EMPTY_LOGIN_VIEW_STATE.copy(
                    type = EMPTY_LOGIN_TYPE.copy(
                        passwordData = passwordData,
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithTextAfterScroll(passwordData.password)
            .onSiblings()
            .filterToOne(hasContentDescription("Copy password"))
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.ItemType.Login.CopyPasswordClick)
        }
    }

    @Test
    fun `in login state, the Passkey field should exist based on the state`() {
        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = EMPTY_LOGIN_VIEW_STATE.copy(
                    type = EMPTY_LOGIN_TYPE.copy(
                        fido2CredentialCreationDateText = DEFAULT_PASSKEY,
                    ),
                ),
            )
        }

        // Only pull-to-refresh remains
        composeTestRule.onAllNodes(isProgressBar).assertCountEquals(1)

        composeTestRule
            .onNodeWithText("Passkey")
            .assertIsDisplayed()
    }

    @Test
    fun `in login state, the Passkey field should not exist based on state`() {
        mutableStateFlow.update { it }

        composeTestRule
            .onNodeWithText("Passkey")
            .assertDoesNotExist()
    }

    @Test
    fun `in login state, the Passkey field text should display creation date`() {
        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = EMPTY_LOGIN_VIEW_STATE.copy(
                    type = EMPTY_LOGIN_TYPE.copy(
                        fido2CredentialCreationDateText = DEFAULT_PASSKEY,
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(text = "Created 3/13/24, 3:56 PM")
            .assertIsDisplayed()
    }

    @Test
    fun `in login state, the TOTP field should exist based on the state`() {
        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = DEFAULT_LOGIN_VIEW_STATE.copy(
                    type = DEFAULT_LOGIN.copy(
                        totpCodeItemData = null,
                    ),
                ),
            )
        }

        // Only pull-to-refresh remains
        composeTestRule.onAllNodes(isProgressBar).assertCountEquals(1)

        composeTestRule
            .onNodeWithContentDescription("Copy TOTP")
            .assertDoesNotExist()

        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = DEFAULT_LOGIN_VIEW_STATE.copy(
                    type = DEFAULT_LOGIN.copy(
                        totpCodeItemData = TotpCodeItemData(
                            periodSeconds = 30,
                            timeLeftSeconds = 15,
                            verificationCode = "123456",
                            totpCode = "testCode",
                        ),
                    ),
                ),
            )
        }

        // There are 2 because of the pull-to-refresh
        composeTestRule.onAllNodes(isProgressBar).assertCountEquals(2)

        composeTestRule
            .onNodeWithContentDescription("Copy TOTP")
            .assertIsDisplayed()
    }

    @Test
    fun `in login state, TOTP item should be displayed according to state`() {
        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = DEFAULT_LOGIN_VIEW_STATE,
            )
        }

        // There are 2 because of the pull-to-refresh
        composeTestRule.onAllNodes(isProgressBar).assertCountEquals(2)

        composeTestRule
            .onNodeWithContentDescription("Copy TOTP")
            .assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = DEFAULT_LOGIN_VIEW_STATE.copy(
                    type = DEFAULT_LOGIN.copy(
                        canViewTotpCode = false,
                    ),
                ),
            )
        }

        // Only pull-to-refresh remains
        composeTestRule.onAllNodes(isProgressBar).assertCountEquals(1)

        composeTestRule
            .onNodeWithContentDescription("Copy TOTP")
            .assertIsNotDisplayed()
    }

    @Test
    fun `in login state, on copy totp click should send CopyTotpClick`() {
        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = DEFAULT_LOGIN_VIEW_STATE,
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Copy TOTP")
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.ItemType.Login.CopyTotpClick)
        }
    }

    @Test
    fun `in login state, launch uri button should be displayed according to state`() {
        val uriData = VaultItemState.ViewState.Content.ItemType.Login.UriData(
            uri = "www.example.com",
            isCopyable = true,
            isLaunchable = true,
        )
        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = EMPTY_LOGIN_VIEW_STATE.copy(
                    type = EMPTY_LOGIN_TYPE.copy(
                        uris = listOf(uriData),
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithTextAfterScroll(uriData.uri)
            .onSiblings()
            .filterToOne(hasContentDescription("Launch"))
            .assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) {
                copy(uris = listOf(uriData.copy(isLaunchable = false)))
            }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(uriData.uri)
            .onSiblings()
            .filterToOne(hasContentDescription("Launch"))
            .assertDoesNotExist()
    }

    @Test
    fun `in login state, copy uri button should be displayed according to state`() {
        val uriData = VaultItemState.ViewState.Content.ItemType.Login.UriData(
            uri = "www.example.com",
            isCopyable = true,
            isLaunchable = true,
        )
        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = EMPTY_LOGIN_VIEW_STATE.copy(
                    type = EMPTY_LOGIN_TYPE.copy(
                        uris = listOf(uriData),
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithTextAfterScroll(uriData.uri)
            .onSiblings()
            .filterToOne(hasContentDescription("Copy"))
            .assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(uris = listOf(uriData.copy(isCopyable = false))) }
        }

        composeTestRule
            .onNodeWithTextAfterScroll(uriData.uri)
            .onSiblings()
            .filterToOne(hasContentDescription("Copy"))
            .assertDoesNotExist()
    }

    @Test
    fun `in login state, on launch URI click should send LaunchClick`() {
        val uriData = VaultItemState.ViewState.Content.ItemType.Login.UriData(
            uri = "www.example.com",
            isCopyable = true,
            isLaunchable = true,
        )
        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = EMPTY_LOGIN_VIEW_STATE.copy(
                    type = EMPTY_LOGIN_TYPE.copy(
                        uris = listOf(uriData),
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithTextAfterScroll(uriData.uri)
            .onSiblings()
            .filterToOne(hasContentDescription("Launch"))
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.ItemType.Login.LaunchClick(uriData.uri))
        }
    }

    @Test
    fun `in login state, on copy URI click should send CopyUriClick`() {
        val uriData = VaultItemState.ViewState.Content.ItemType.Login.UriData(
            uri = "www.example.com",
            isCopyable = true,
            isLaunchable = true,
        )
        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = EMPTY_LOGIN_VIEW_STATE.copy(
                    type = EMPTY_LOGIN_TYPE.copy(
                        uris = listOf(uriData),
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithTextAfterScroll(uriData.uri)
            .onSiblings()
            .filterToOne(hasContentDescription("Copy"))
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.ItemType.Login.CopyUriClick(uriData.uri))
        }
    }

    @Test
    fun `in login state, on password history click should send PasswordHistoryClick`() {
        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = EMPTY_LOGIN_VIEW_STATE.copy(
                    type = EMPTY_LOGIN_TYPE.copy(
                        passwordHistoryCount = 5,
                    ),
                ),
            )
        }

        composeTestRule.onNodeWithTextAfterScroll("5")
        composeTestRule.onNodeWithText("5").performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.ItemType.Login.PasswordHistoryClick)
        }
    }

    @Test
    fun `fab should be displayed according state`() {
        mutableStateFlow.update {
            it.copy(viewState = VaultItemState.ViewState.Loading)
        }
        composeTestRule.onNodeWithContentDescription("Edit item").assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(viewState = VaultItemState.ViewState.Error("Fail".asText()))
        }
        composeTestRule.onNodeWithContentDescription("Edit item").assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_LOGIN_VIEW_STATE)
        }
        composeTestRule.onNodeWithContentDescription("Edit item").performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(VaultItemAction.Common.EditClick)
        }
    }

    @Test
    fun `error text and retry should be displayed according to state`() {
        val message = "message"
        mutableStateFlow.update {
            it.copy(viewState = VaultItemState.ViewState.Error(message.asText()))
        }

        composeTestRule.onNodeWithText(message).assertIsDisplayed()
        composeTestRule.onNodeWithText("Try again").assertIsDisplayed()
    }

    @Test
    fun `progressbar should be displayed according to state`() {
        mutableStateFlow.update {
            it.copy(viewState = VaultItemState.ViewState.Loading)
        }
        // There are 2 because of the pull-to-refresh
        composeTestRule.onAllNodes(isProgressBar).assertCountEquals(2)

        mutableStateFlow.update {
            it.copy(viewState = VaultItemState.ViewState.Error("Fail".asText()))
        }
        // Only pull-to-refresh remains
        composeTestRule.onAllNodes(isProgressBar).assertCountEquals(1)

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) {
                copy(totpCodeItemData = null)
            }
        }
        // Only pull-to-refresh remains
        composeTestRule.onAllNodes(isProgressBar).assertCountEquals(1)
    }

    @Test
    fun `in login state, username should be displayed according to state`() {
        val username = "the username"
        mutableStateFlow.update { it.copy(viewState = DEFAULT_LOGIN_VIEW_STATE) }
        composeTestRule.onNodeWithTextAfterScroll(username).assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(username = null) }
        }

        composeTestRule.assertScrollableNodeDoesNotExist(username)
    }

    @Test
    fun `in login state, uris should be displayed according to state`() {
        mutableStateFlow.update { it.copy(viewState = DEFAULT_LOGIN_VIEW_STATE) }
        composeTestRule.onNodeWithTextAfterScroll("URIS").assertIsDisplayed()
        composeTestRule.onNodeWithTextAfterScroll("URI").assertIsDisplayed()
        composeTestRule.onNodeWithTextAfterScroll("www.example.com").assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(uris = emptyList()) }
        }

        composeTestRule.assertScrollableNodeDoesNotExist("URIS")
        composeTestRule.assertScrollableNodeDoesNotExist("URI")
        composeTestRule.assertScrollableNodeDoesNotExist("www.example.com")
    }

    @Test
    fun `in login state, password updated should be displayed according to state`() {
        mutableStateFlow.update { it.copy(viewState = DEFAULT_LOGIN_VIEW_STATE) }
        composeTestRule.onNodeWithTextAfterScroll("Password updated: ").assertIsDisplayed()
        composeTestRule.onNodeWithTextAfterScroll("4/14/83 3:56 PM").assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(passwordRevisionDate = null) }
        }

        composeTestRule.assertScrollableNodeDoesNotExist("Password updated: ")
        composeTestRule.assertScrollableNodeDoesNotExist("4/14/83 3:56 PM")
    }

    @Test
    fun `in login state, password history should be displayed according to state`() {
        mutableStateFlow.update { it.copy(viewState = DEFAULT_LOGIN_VIEW_STATE) }
        composeTestRule.onNodeWithTextAfterScroll("Password history: ").assertIsDisplayed()
        composeTestRule.onNodeWithTextAfterScroll("1").assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(passwordHistoryCount = null) }
        }

        composeTestRule.assertScrollableNodeDoesNotExist("Password history: ")
        composeTestRule.assertScrollableNodeDoesNotExist("1")
    }
    //endregion login

    //region identity
    @Test
    fun `in identity state, identityName should be displayed according to state`() {
        val identityName = "the identity name"
        mutableStateFlow.update { it.copy(viewState = DEFAULT_IDENTITY_VIEW_STATE) }
        composeTestRule.onNodeWithTextAfterScroll(identityName).assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateIdentityType(currentState) { copy(identityName = null) }
        }

        composeTestRule.assertScrollableNodeDoesNotExist(identityName)
    }

    @Test
    fun `in identity state, username should be displayed according to state`() {
        val identityName = "the username"
        mutableStateFlow.update { it.copy(viewState = DEFAULT_IDENTITY_VIEW_STATE) }
        composeTestRule.onNodeWithTextAfterScroll(identityName).assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateIdentityType(currentState) { copy(username = null) }
        }

        composeTestRule.assertScrollableNodeDoesNotExist(identityName)
    }

    @Test
    fun `in identity state, company should be displayed according to state`() {
        val identityName = "the company name"
        mutableStateFlow.update { it.copy(viewState = DEFAULT_IDENTITY_VIEW_STATE) }
        composeTestRule.onNodeWithTextAfterScroll(identityName).assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateIdentityType(currentState) { copy(company = null) }
        }

        composeTestRule.assertScrollableNodeDoesNotExist(identityName)
    }

    @Test
    fun `in identity state, ssn should be displayed according to state`() {
        val identityName = "the SSN"
        mutableStateFlow.update { it.copy(viewState = DEFAULT_IDENTITY_VIEW_STATE) }
        composeTestRule.onNodeWithTextAfterScroll(identityName).assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateIdentityType(currentState) { copy(ssn = null) }
        }

        composeTestRule.assertScrollableNodeDoesNotExist(identityName)
    }

    @Test
    fun `in identity state, passportNumber should be displayed according to state`() {
        val identityName = "the passport number"
        mutableStateFlow.update { it.copy(viewState = DEFAULT_IDENTITY_VIEW_STATE) }
        composeTestRule.onNodeWithTextAfterScroll(identityName).assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateIdentityType(currentState) { copy(passportNumber = null) }
        }

        composeTestRule.assertScrollableNodeDoesNotExist(identityName)
    }

    @Test
    fun `in identity state, licenseNumber should be displayed according to state`() {
        val identityName = "the license number"
        mutableStateFlow.update { it.copy(viewState = DEFAULT_IDENTITY_VIEW_STATE) }
        composeTestRule.onNodeWithTextAfterScroll(identityName).assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateIdentityType(currentState) { copy(licenseNumber = null) }
        }

        composeTestRule.assertScrollableNodeDoesNotExist(identityName)
    }

    @Test
    fun `in identity state, email should be displayed according to state`() {
        val identityName = "the email address"
        mutableStateFlow.update { it.copy(viewState = DEFAULT_IDENTITY_VIEW_STATE) }
        composeTestRule.onNodeWithTextAfterScroll(identityName).assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateIdentityType(currentState) { copy(email = null) }
        }

        composeTestRule.assertScrollableNodeDoesNotExist(identityName)
    }

    @Test
    fun `in identity state, phone should be displayed according to state`() {
        val identityName = "the phone number"
        mutableStateFlow.update { it.copy(viewState = DEFAULT_IDENTITY_VIEW_STATE) }
        composeTestRule.onNodeWithTextAfterScroll(identityName).assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateIdentityType(currentState) { copy(phone = null) }
        }

        composeTestRule.assertScrollableNodeDoesNotExist(identityName)
    }

    @Test
    fun `in identity state, address should be displayed according to state`() {
        val identityName = "the address"
        mutableStateFlow.update { it.copy(viewState = DEFAULT_IDENTITY_VIEW_STATE) }
        composeTestRule.onNodeWithTextAfterScroll(identityName).assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateIdentityType(currentState) { copy(address = null) }
        }

        composeTestRule.assertScrollableNodeDoesNotExist(identityName)
    }

    @Test
    fun `in identity state, on copy identity name field click should send CopyIdentityNameClick`() {

        val identityName = "the identity name"
        mutableStateFlow.update { it.copy(viewState = DEFAULT_IDENTITY_VIEW_STATE) }
        composeTestRule.onNodeWithTextAfterScroll(identityName)

        composeTestRule
            .onNodeWithTag("IdentityCopyNameButton")
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopyIdentityNameClick)
        }
    }

    @Test
    fun `in identity state, on copy username field click should send CopyUsernameClick`() {
        val username = "the username"
        mutableStateFlow.update { it.copy(viewState = DEFAULT_IDENTITY_VIEW_STATE) }
        composeTestRule.onNodeWithTextAfterScroll(username)

        composeTestRule
            .onNodeWithTag("IdentityCopyUsernameButton")
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopyUsernameClick)
        }
    }

    @Test
    fun `in identity state, on copy company field click should send CopyCompanyClick`() {
        mutableStateFlow.update { it.copy(viewState = DEFAULT_IDENTITY_VIEW_STATE) }

        // Scroll to ssn so we can see the Copy company button but not have it covered by the FAB
        composeTestRule.onNodeWithTextAfterScroll("the SSN")

        composeTestRule
            .onNodeWithTag("IdentityCopyCompanyButton")
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopyCompanyClick)
        }
    }

    @Test
    fun `in identity state, on copy SSN field click should send CopySsnClick`() {
        val ssn = "the SSN"
        mutableStateFlow.update { it.copy(viewState = DEFAULT_IDENTITY_VIEW_STATE) }
        composeTestRule.onNodeWithTextAfterScroll(ssn)

        composeTestRule
            .onNodeWithTag("IdentityCopySsnButton")
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopySsnClick)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in identity state, on copy passport number field click should send CopyPassportNumberClick`() {
        val passportNumber = "the passport number"
        mutableStateFlow.update { it.copy(viewState = DEFAULT_IDENTITY_VIEW_STATE) }
        composeTestRule.onNodeWithTextAfterScroll(passportNumber)

        composeTestRule
            .onNodeWithTag("IdentityCopyPassportNumberButton")
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopyPassportNumberClick)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `in identity state, on copy license number field click should send CopyLicenseNumberClick`() {
        val licenseNumber = "the license number"
        mutableStateFlow.update { it.copy(viewState = DEFAULT_IDENTITY_VIEW_STATE) }
        composeTestRule.onNodeWithTextAfterScroll(licenseNumber)

        composeTestRule
            .onNodeWithTag("IdentityCopyLicenseNumberButton")
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopyLicenseNumberClick)
        }
    }

    @Test
    fun `in identity state, on copy email field click should send CopyEmailClick`() {
        mutableStateFlow.update { it.copy(viewState = DEFAULT_IDENTITY_VIEW_STATE) }
        composeTestRule.onFirstNodeWithTextAfterScroll("the address")

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll("Copy email")
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopyEmailClick)
        }
    }

    @Test
    fun `in identity state, on copy phone field click should send CopyPhoneClick`() {
        val phone = "the phone number"
        mutableStateFlow.update { it.copy(viewState = DEFAULT_IDENTITY_VIEW_STATE) }
        composeTestRule.onNodeWithTextAfterScroll(phone)

        composeTestRule
            .onNodeWithTag("IdentityCopyPhoneButton")
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopyPhoneClick)
        }
    }

    @Test
    fun `in identity state, on copy address field click should send CopyAddressClick`() {
        val address = "the address"
        mutableStateFlow.update { it.copy(viewState = DEFAULT_IDENTITY_VIEW_STATE) }
        composeTestRule.onNodeWithTextAfterScroll(address)

        composeTestRule
            .onNodeWithTag("IdentityCopyAddressButton")
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.ItemType.Identity.CopyAddressClick)
        }
    }
    //endregion identity

    //region card

    @Test
    fun `in card state, cardholderName should be displayed according to state`() {
        val cardholderName = "the cardholder name"
        mutableStateFlow.update { it.copy(viewState = DEFAULT_CARD_VIEW_STATE) }
        composeTestRule.onNodeWithTextAfterScroll(cardholderName).assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateCardType(currentState) { copy(cardholderName = null) }
        }

        composeTestRule.assertScrollableNodeDoesNotExist(cardholderName)
    }

    @Test
    fun `in card state, on show number click should send NumberVisibilityClick`() {
        composeTestRule.assertScrollableNodeDoesNotExist("Number")

        mutableStateFlow.update {
            it.copy(
                viewState = EMPTY_CARD_VIEW_STATE.copy(
                    type = EMPTY_CARD_TYPE.copy(
                        number = VaultItemState.ViewState.Content.ItemType.Card.NumberData(
                            number = "number",
                            isVisible = false,
                        ),
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithTextAfterScroll("Number")
            .assertTextEquals("Number", "••••••")
            .assertIsEnabled()
        composeTestRule
            .onNodeWithContentDescription("Copy number")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Show")
            .assertIsDisplayed()
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(
                VaultItemAction.ItemType.Card.NumberVisibilityClick(isVisible = true),
            )
        }
    }

    @Test
    fun `in card state the number should be displayed according to state`() {
        composeTestRule.assertScrollableNodeDoesNotExist("Number")

        mutableStateFlow.update {
            it.copy(
                viewState = EMPTY_CARD_VIEW_STATE.copy(
                    type = EMPTY_CARD_TYPE.copy(
                        number = VaultItemState.ViewState.Content.ItemType.Card.NumberData(
                            number = "number",
                            isVisible = false,
                        ),
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithTextAfterScroll("Number")
            .assertTextEquals("Number", "••••••")
            .assertIsEnabled()
        composeTestRule
            .onNodeWithContentDescription("Copy number")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Show")
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                viewState = EMPTY_CARD_VIEW_STATE.copy(
                    type = EMPTY_CARD_TYPE.copy(
                        number = VaultItemState.ViewState.Content.ItemType.Card.NumberData(
                            number = "number",
                            isVisible = true,
                        ),
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithText("Number")
            .assertTextEquals("Number", "number")
            .assertIsEnabled()
        composeTestRule
            .onNodeWithContentDescription("Copy number")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Hide")
            .assertIsDisplayed()
    }

    @Test
    fun `in card state, on copy number click should send CopyNumberClick`() {
        val number = "123456789"
        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = EMPTY_CARD_VIEW_STATE.copy(
                    type = EMPTY_CARD_TYPE.copy(
                        number = VaultItemState.ViewState.Content.ItemType.Card.NumberData(
                            number = number,
                            isVisible = false,
                        ),
                        expiration = "test",
                    ),
                ),
            )
        }

        // Scroll so we can see the Copy number button but not have it covered by the FAB
        composeTestRule
            .onNodeWithTextAfterScroll("Expiration")
        composeTestRule
            .onNodeWithContentDescription("Copy number")
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.ItemType.Card.CopyNumberClick)
        }
    }

    @Test
    fun `in card state, brand should be displayed according to state`() {
        val visa = "Visa"
        mutableStateFlow.update { it.copy(viewState = DEFAULT_CARD_VIEW_STATE) }
        composeTestRule.onNodeWithTextAfterScroll(visa).assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateCardType(currentState) { copy(brand = null) }
        }

        composeTestRule.assertScrollableNodeDoesNotExist(visa)
    }

    @Test
    fun `in card state, expiration should be displayed according to state`() {
        val expiration = "the expiration"
        mutableStateFlow.update { it.copy(viewState = DEFAULT_CARD_VIEW_STATE) }
        composeTestRule.onNodeWithTextAfterScroll(expiration).assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateCardType(currentState) { copy(expiration = null) }
        }

        composeTestRule.assertScrollableNodeDoesNotExist(expiration)
    }

    @Test
    fun `in card state, on show code click should send CodeVisibilityClick`() {
        composeTestRule.assertScrollableNodeDoesNotExist("Security code")

        mutableStateFlow.update {
            it.copy(
                viewState = EMPTY_CARD_VIEW_STATE.copy(
                    type = EMPTY_CARD_TYPE.copy(
                        securityCode = VaultItemState.ViewState.Content.ItemType.Card.CodeData(
                            code = "123",
                            isVisible = false,
                        ),
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithTextAfterScroll("Security code")
            .assertTextEquals("Security code", "•••")
            .assertIsEnabled()
        composeTestRule
            .onNodeWithContentDescription("Copy security code")
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithContentDescription("Show")
            .onLast()
            .assertIsDisplayed()
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(
                VaultItemAction.ItemType.Card.CodeVisibilityClick(isVisible = true),
            )
        }
    }

    @Test
    fun `in card state the security code should be displayed according to state`() {
        composeTestRule.assertScrollableNodeDoesNotExist("Security code")

        mutableStateFlow.update {
            it.copy(
                viewState = EMPTY_CARD_VIEW_STATE.copy(
                    type = EMPTY_CARD_TYPE.copy(
                        securityCode = VaultItemState.ViewState.Content.ItemType.Card.CodeData(
                            code = "123",
                            isVisible = false,
                        ),
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithTextAfterScroll("Security code")
            .assertTextEquals("Security code", "•••")
            .assertIsEnabled()
        composeTestRule
            .onNodeWithContentDescription("Copy security code")
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithContentDescription("Show")
            .onLast()
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                viewState = EMPTY_CARD_VIEW_STATE.copy(
                    type = EMPTY_CARD_TYPE.copy(
                        securityCode = VaultItemState.ViewState.Content.ItemType.Card.CodeData(
                            code = "123",
                            isVisible = true,
                        ),
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithText("Security code")
            .assertTextEquals("Security code", "123")
            .assertIsEnabled()
        composeTestRule
            .onNodeWithContentDescription("Copy security code")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Hide")
            .assertIsDisplayed()
    }

    @Test
    fun `in card state, on copy security code click should send CopySecurityCodeClick`() {
        val code = "1234"
        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = EMPTY_CARD_VIEW_STATE.copy(
                    type = EMPTY_CARD_TYPE.copy(
                        securityCode = VaultItemState.ViewState.Content.ItemType.Card.CodeData(
                            code = code,
                            isVisible = false,
                        ),
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll("Copy security code")
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.ItemType.Card.CopySecurityCodeClick)
        }
    }

    //endregion card

    //region ssh key

    @Test
    fun `in ssh key state, public key should be displayed according to state`() {
        val publicKey = "the public key"
        mutableStateFlow.update { it.copy(viewState = DEFAULT_SSH_KEY_VIEW_STATE) }
        composeTestRule.onNodeWithTextAfterScroll(publicKey).assertIsDisplayed()
    }

    @Test
    fun `in ssh key state, on copy public key click should send CopyPublicKeyClick`() {
        mutableStateFlow.update { it.copy(viewState = DEFAULT_SSH_KEY_VIEW_STATE) }
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll("Copy public key")
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(VaultItemAction.ItemType.SshKey.CopyPublicKeyClick)
        }
    }

    @Test
    fun `in ssh key state, private key should be displayed according to state`() {
        val privateKey = "the private key"
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_SSH_KEY_VIEW_STATE
                    .copy(
                        type = DEFAULT_SSH_KEY.copy(showPrivateKey = true),
                    ),
            )
        }
        composeTestRule
            .onNodeWithText(privateKey)
            .assertIsDisplayed()
    }

    @Test
    fun `in ssh key state, on show private key click should send ShowPrivateKeyClick`() {
        mutableStateFlow.update { it.copy(viewState = DEFAULT_SSH_KEY_VIEW_STATE) }

        composeTestRule
            .onNodeWithTextAfterScroll("Private key")
            .onChildren()
            .filterToOne(hasContentDescription("Show"))
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(
                VaultItemAction.ItemType.SshKey.PrivateKeyVisibilityClicked(
                    isVisible = true,
                ),
            )
        }
    }

    @Test
    fun `in ssh key state, on copy private key click should send CopyPrivateKeyClick`() {
        mutableStateFlow.update { it.copy(viewState = DEFAULT_SSH_KEY_VIEW_STATE) }
        composeTestRule
            .onNodeWithContentDescriptionAfterScroll("Copy private key")
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(VaultItemAction.ItemType.SshKey.CopyPrivateKeyClick)
        }
    }

    @Test
    fun `in ssh key state, fingerprint should be displayed according to state`() {
        val fingerprint = "the fingerprint"
        mutableStateFlow.update { it.copy(viewState = DEFAULT_SSH_KEY_VIEW_STATE) }
        composeTestRule.onNodeWithTextAfterScroll(fingerprint).assertIsDisplayed()
    }

    @Test
    fun `in ssh key state, on copy fingerprint click should send CopyFingerprintClick`() {
        mutableStateFlow.update { it.copy(viewState = DEFAULT_SSH_KEY_VIEW_STATE) }
        composeTestRule
            .onNodeWithContentDescription("Copy fingerprint")
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(VaultItemAction.ItemType.SshKey.CopyFingerprintClick)
        }
    }

    //endregion ssh key
}

//region Helper functions

private fun updateLoginType(
    currentState: VaultItemState,
    transform: VaultItemState.ViewState.Content.ItemType.Login.() ->
    VaultItemState.ViewState.Content.ItemType.Login,
): VaultItemState {
    val updatedType = when (val viewState = currentState.viewState) {
        is VaultItemState.ViewState.Content -> {
            when (val type = viewState.type) {
                is VaultItemState.ViewState.Content.ItemType.Login -> {
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

private fun updateIdentityType(
    currentState: VaultItemState,
    transform: VaultItemState.ViewState.Content.ItemType.Identity.() ->
    VaultItemState.ViewState.Content.ItemType.Identity,
): VaultItemState {
    val updatedType = when (val viewState = currentState.viewState) {
        is VaultItemState.ViewState.Content -> {
            when (val type = viewState.type) {
                is VaultItemState.ViewState.Content.ItemType.Identity -> {
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

private fun updateCardType(
    currentState: VaultItemState,
    transform: VaultItemState.ViewState.Content.ItemType.Card.() ->
    VaultItemState.ViewState.Content.ItemType.Card,
): VaultItemState {
    val updatedType = when (val viewState = currentState.viewState) {
        is VaultItemState.ViewState.Content -> {
            when (val type = viewState.type) {
                is VaultItemState.ViewState.Content.ItemType.Card -> {
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

private fun updateCommonContent(
    currentState: VaultItemState,
    transform: VaultItemState.ViewState.Content.Common.()
    -> VaultItemState.ViewState.Content.Common,
): VaultItemState {
    val updatedType = when (val viewState = currentState.viewState) {
        is VaultItemState.ViewState.Content ->
            viewState.copy(common = viewState.common.transform())

        else -> viewState
    }
    return currentState.copy(viewState = updatedType)
}

//endregion Helper functions

private const val VAULT_ITEM_ID = "vault_item_id"

private val DEFAULT_STATE: VaultItemState = VaultItemState(
    vaultItemId = VAULT_ITEM_ID,
    viewState = VaultItemState.ViewState.Loading,
    dialog = null,
)

private val DEFAULT_COMMON: VaultItemState.ViewState.Content.Common =
    VaultItemState.ViewState.Content.Common(
        name = "cipher",
        lastUpdated = "12/31/69 06:16 PM",
        notes = "Lots of notes",
        customFields = listOf(
            VaultItemState.ViewState.Content.Common.Custom.TextField(
                name = "text",
                value = "value",
                isCopyable = true,
            ),
            VaultItemState.ViewState.Content.Common.Custom.HiddenField(
                name = "hidden",
                value = "hidden password",
                isCopyable = true,
                isVisible = false,
            ),
            VaultItemState.ViewState.Content.Common.Custom.BooleanField(
                name = "boolean",
                value = true,
            ),
        ),
        requiresReprompt = true,
        requiresCloneConfirmation = false,
        attachments = listOf(
            VaultItemState.ViewState.Content.Common.AttachmentItem(
                id = "attachment-id",
                displaySize = "11 MB",
                isLargeFile = true,
                isDownloadAllowed = true,
                url = "https://example.com",
                title = "test.mp4",
            ),
        ),
        canDelete = true,
        canAssignToCollections = true,
    )

private val DEFAULT_PASSKEY = R.string.created_xy.asText(
    "3/13/24",
    "3:56 PM",
)

private val DEFAULT_LOGIN: VaultItemState.ViewState.Content.ItemType.Login =
    VaultItemState.ViewState.Content.ItemType.Login(
        passwordHistoryCount = 1,
        username = "the username",
        passwordData = VaultItemState.ViewState.Content.ItemType.Login.PasswordData(
            password = "the password",
            isVisible = false,
            canViewPassword = true,
        ),
        uris = listOf(
            VaultItemState.ViewState.Content.ItemType.Login.UriData(
                uri = "www.example.com",
                isCopyable = true,
                isLaunchable = true,
            ),
        ),
        passwordRevisionDate = "4/14/83 3:56 PM",
        isPremiumUser = true,
        totpCodeItemData = TotpCodeItemData(
            periodSeconds = 30,
            timeLeftSeconds = 15,
            verificationCode = "123456",
            totpCode = "testCode",
        ),
        fido2CredentialCreationDateText = null,
        canViewTotpCode = true,
    )

private val DEFAULT_IDENTITY: VaultItemState.ViewState.Content.ItemType.Identity =
    VaultItemState.ViewState.Content.ItemType.Identity(
        username = "the username",
        identityName = "the identity name",
        company = "the company name",
        ssn = "the SSN",
        passportNumber = "the passport number",
        licenseNumber = "the license number",
        email = "the email address",
        phone = "the phone number",
        address = "the address",
    )

private val DEFAULT_CARD: VaultItemState.ViewState.Content.ItemType.Card =
    VaultItemState.ViewState.Content.ItemType.Card(
        cardholderName = "the cardholder name",
        number = VaultItemState.ViewState.Content.ItemType.Card.NumberData(
            number = "the number",
            isVisible = false,
        ),
        brand = VaultCardBrand.VISA,
        expiration = "the expiration",
        securityCode = VaultItemState.ViewState.Content.ItemType.Card.CodeData(
            code = "the security code",
            isVisible = false,
        ),
    )

private val DEFAULT_SSH_KEY: VaultItemState.ViewState.Content.ItemType.SshKey =
    VaultItemState.ViewState.Content.ItemType.SshKey(
        name = "the ssh key name",
        publicKey = "the public key",
        privateKey = "the private key",
        fingerprint = "the fingerprint",
        showPrivateKey = false,
    )

private val EMPTY_COMMON: VaultItemState.ViewState.Content.Common =
    VaultItemState.ViewState.Content.Common(
        name = "cipher",
        lastUpdated = "12/31/69 06:16 PM",
        notes = null,
        customFields = emptyList(),
        requiresReprompt = true,
        requiresCloneConfirmation = false,
        attachments = emptyList(),
        canDelete = true,
        canAssignToCollections = true,
    )

private val EMPTY_LOGIN_TYPE: VaultItemState.ViewState.Content.ItemType.Login =
    VaultItemState.ViewState.Content.ItemType.Login(
        username = null,
        passwordData = null,
        passwordHistoryCount = null,
        uris = emptyList(),
        passwordRevisionDate = null,
        totpCodeItemData = null,
        isPremiumUser = true,
        canViewTotpCode = true,
        fido2CredentialCreationDateText = null,
    )

private val EMPTY_IDENTITY_TYPE: VaultItemState.ViewState.Content.ItemType.Identity =
    VaultItemState.ViewState.Content.ItemType.Identity(
        username = "",
        identityName = "",
        company = "",
        ssn = "",
        passportNumber = "",
        licenseNumber = "",
        email = "",
        phone = "",
        address = "",
    )

private val EMPTY_CARD_TYPE: VaultItemState.ViewState.Content.ItemType.Card =
    VaultItemState.ViewState.Content.ItemType.Card(
        cardholderName = "",
        number = VaultItemState.ViewState.Content.ItemType.Card.NumberData(
            number = "",
            isVisible = false,
        ),
        brand = VaultCardBrand.SELECT,
        expiration = "",
        securityCode = VaultItemState.ViewState.Content.ItemType.Card.CodeData(
            code = "",
            isVisible = false,
        ),
    )

private val EMPTY_SSH_KEY_TYPE: VaultItemState.ViewState.Content.ItemType.SshKey =
    VaultItemState.ViewState.Content.ItemType.SshKey(
        name = "",
        publicKey = "",
        privateKey = "",
        fingerprint = "",
        showPrivateKey = false,
    )

private val EMPTY_LOGIN_VIEW_STATE: VaultItemState.ViewState.Content =
    VaultItemState.ViewState.Content(
        common = EMPTY_COMMON,
        type = EMPTY_LOGIN_TYPE,
    )

private val EMPTY_IDENTITY_VIEW_STATE: VaultItemState.ViewState.Content =
    VaultItemState.ViewState.Content(
        common = EMPTY_COMMON,
        type = EMPTY_IDENTITY_TYPE,
    )

private val EMPTY_CARD_VIEW_STATE: VaultItemState.ViewState.Content =
    VaultItemState.ViewState.Content(
        common = EMPTY_COMMON,
        type = EMPTY_CARD_TYPE,
    )

private val EMPTY_SECURE_NOTE_VIEW_STATE =
    VaultItemState.ViewState.Content(
        common = EMPTY_COMMON,
        type = VaultItemState.ViewState.Content.ItemType.SecureNote,
    )

private val EMPTY_SSH_KEY_VIEW_STATE =
    VaultItemState.ViewState.Content(
        common = EMPTY_COMMON,
        type = EMPTY_SSH_KEY_TYPE,
    )

private val DEFAULT_LOGIN_VIEW_STATE: VaultItemState.ViewState.Content =
    VaultItemState.ViewState.Content(
        type = DEFAULT_LOGIN,
        common = DEFAULT_COMMON,
    )

private val DEFAULT_IDENTITY_VIEW_STATE: VaultItemState.ViewState.Content =
    VaultItemState.ViewState.Content(
        type = DEFAULT_IDENTITY,
        common = DEFAULT_COMMON,
    )

private val DEFAULT_CARD_VIEW_STATE: VaultItemState.ViewState.Content =
    VaultItemState.ViewState.Content(
        type = DEFAULT_CARD,
        common = DEFAULT_COMMON,
    )

private val DEFAULT_SECURE_NOTE_VIEW_STATE: VaultItemState.ViewState.Content =
    VaultItemState.ViewState.Content(
        common = DEFAULT_COMMON,
        type = VaultItemState.ViewState.Content.ItemType.SecureNote,
    )

private val DEFAULT_SSH_KEY_VIEW_STATE: VaultItemState.ViewState.Content =
    VaultItemState.ViewState.Content(
        common = DEFAULT_COMMON,
        type = DEFAULT_SSH_KEY,
    )

private val EMPTY_VIEW_STATES = listOf(
    EMPTY_LOGIN_VIEW_STATE,
    EMPTY_IDENTITY_VIEW_STATE,
    EMPTY_SECURE_NOTE_VIEW_STATE,
    EMPTY_SSH_KEY_VIEW_STATE,
)

private val DEFAULT_VIEW_STATES = listOf(
    DEFAULT_LOGIN_VIEW_STATE,
    DEFAULT_IDENTITY_VIEW_STATE,
    DEFAULT_SECURE_NOTE_VIEW_STATE,
    DEFAULT_SSH_KEY_VIEW_STATE,
)
