package com.x8bit.bitwarden.ui.vault.feature.item

import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onSiblings
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.core.net.toUri
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.IntentHandler
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.toAnnotatedString
import com.x8bit.bitwarden.ui.util.assertScrollableNodeDoesNotExist
import com.x8bit.bitwarden.ui.util.isProgressBar
import com.x8bit.bitwarden.ui.util.onFirstNodeWithTextAfterScroll
import com.x8bit.bitwarden.ui.util.onNodeWithTextAfterScroll
import com.x8bit.bitwarden.ui.vault.model.VaultLinkedFieldType
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VaultItemScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled = false
    private var onNavigateToVaultEditItemId: String? = null

    private val clipboardManager = mockk<ClipboardManager>()
    private val intentHandler = mockk<IntentHandler>()

    private val mutableEventFlow = MutableSharedFlow<VaultItemEvent>(
        extraBufferCapacity = Int.MAX_VALUE,
    )
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
                onNavigateToVaultEditItem = { onNavigateToVaultEditItemId = it },
                clipboardManager = clipboardManager,
                intentHandler = intentHandler,
            )
        }
    }

    @Test
    fun `NavigateToEdit event should invoke onNavigateToVaultEditItem`() {
        val id = "id1234"
        mutableEventFlow.tryEmit(VaultItemEvent.NavigateToEdit(id))
        assertEquals(id, onNavigateToVaultEditItemId)
    }

    @Test
    fun `on close click should send CloseClick`() {
        composeTestRule.onNodeWithContentDescription(label = "Close").performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.CloseClick)
        }
    }

    @Test
    fun `CopyToClipboard event should invoke setText`() {
        val textString = "text"
        val text = textString.asText()
        every { clipboardManager.setText(textString.toAnnotatedString()) } just runs

        mutableEventFlow.tryEmit(VaultItemEvent.CopyToClipboard(text))

        verify(exactly = 1) {
            clipboardManager.setText(textString.toAnnotatedString())
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
        every { intentHandler.launchUri(uri) } just runs

        mutableEventFlow.tryEmit(VaultItemEvent.NavigateToUri(uriString))

        verify(exactly = 1) {
            intentHandler.launchUri(uri)
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
            viewModel.trySendAction(VaultItemAction.DismissDialogClick)
        }
    }

    @Test
    fun `loading dialog should be displayed according to state`() {
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText("Loading").assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(dialog = VaultItemState.DialogState.Loading)
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
            it.copy(dialog = VaultItemState.DialogState.MasterPasswordDialog)
        }

        composeTestRule
            .onNodeWithText("Master password confirmation")
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))
    }

    @Test
    fun `Ok click on master password dialog should emit DismissDialogClick`() {
        val enteredPassword = "pass1234"
        mutableStateFlow.update {
            it.copy(dialog = VaultItemState.DialogState.MasterPasswordDialog)
        }

        composeTestRule.onNodeWithText("Master password").performTextInput(enteredPassword)
        composeTestRule
            .onAllNodesWithText("Submit")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.MasterPasswordSubmit(enteredPassword))
        }
    }

    @Test
    fun `in login state, on username copy click should send CopyUsernameClick`() {
        val username = "username1234"
        mutableStateFlow.update {
            it.copy(viewState = EMPTY_LOGIN_VIEW_STATE.copy(username = username))
        }

        composeTestRule
            .onNodeWithTextAfterScroll(username)
            .onSiblings()
            .filterToOne(hasContentDescription("Copy username"))
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.Login.CopyUsernameClick)
        }
    }

    @Test
    fun `in login state, on breach check click should send CheckForBreachClick`() {
        val passwordData = VaultItemState.ViewState.Content.PasswordData(
            password = "12345",
            isVisible = true,
        )
        mutableStateFlow.update {
            it.copy(viewState = EMPTY_LOGIN_VIEW_STATE.copy(passwordData = passwordData))
        }

        composeTestRule
            .onNodeWithTextAfterScroll(passwordData.password)
            .onSiblings()
            .filterToOne(hasContentDescription("Check known data breaches for this password"))
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.Login.CheckForBreachClick)
        }
    }

    @Test
    fun `in login state, on show password click should send CopyPasswordClick`() {
        mutableStateFlow.update { it.copy(viewState = DEFAULT_LOGIN_VIEW_STATE) }

        composeTestRule
            .onNodeWithTextAfterScroll("Password")
            .onChildren()
            .filterToOne(hasContentDescription("Show"))
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(VaultItemAction.Login.PasswordVisibilityClicked(true))
        }
    }

    @Test
    fun `in login state, on copy password click should send CopyPasswordClick`() {
        val passwordData = VaultItemState.ViewState.Content.PasswordData(
            password = "12345",
            isVisible = true,
        )
        mutableStateFlow.update {
            it.copy(viewState = EMPTY_LOGIN_VIEW_STATE.copy(passwordData = passwordData))
        }

        composeTestRule
            .onNodeWithTextAfterScroll(passwordData.password)
            .onSiblings()
            .filterToOne(hasContentDescription("Copy password"))
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.Login.CopyPasswordClick)
        }
    }

    @Test
    fun `in login state, launch uri button should be displayed according to state`() {
        val uriData = VaultItemState.ViewState.Content.UriData(
            uri = "www.example.com",
            isCopyable = true,
            isLaunchable = true,
        )
        mutableStateFlow.update {
            it.copy(viewState = EMPTY_LOGIN_VIEW_STATE.copy(uris = listOf(uriData)))
        }

        composeTestRule
            .onNodeWithTextAfterScroll(uriData.uri)
            .onSiblings()
            .filterToOne(hasContentDescription("Launch"))
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                viewState = EMPTY_LOGIN_VIEW_STATE.copy(
                    uris = listOf(uriData.copy(isLaunchable = false)),
                ),
            )
        }

        composeTestRule
            .onNodeWithTextAfterScroll(uriData.uri)
            .onSiblings()
            .filterToOne(hasContentDescription("Launch"))
            .assertDoesNotExist()
    }

    @Test
    fun `in login state, copy uri button should be displayed according to state`() {
        val uriData = VaultItemState.ViewState.Content.UriData(
            uri = "www.example.com",
            isCopyable = true,
            isLaunchable = true,
        )
        mutableStateFlow.update {
            it.copy(viewState = EMPTY_LOGIN_VIEW_STATE.copy(uris = listOf(uriData)))
        }

        composeTestRule
            .onNodeWithTextAfterScroll(uriData.uri)
            .onSiblings()
            .filterToOne(hasContentDescription("Copy"))
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                viewState = EMPTY_LOGIN_VIEW_STATE.copy(
                    uris = listOf(uriData.copy(isCopyable = false)),
                ),
            )
        }

        composeTestRule
            .onNodeWithTextAfterScroll(uriData.uri)
            .onSiblings()
            .filterToOne(hasContentDescription("Copy"))
            .assertDoesNotExist()
    }

    @Test
    fun `in login state, on launch URI click should send LaunchClick`() {
        val uriData = VaultItemState.ViewState.Content.UriData(
            uri = "www.example.com",
            isCopyable = true,
            isLaunchable = true,
        )
        mutableStateFlow.update {
            it.copy(viewState = EMPTY_LOGIN_VIEW_STATE.copy(uris = listOf(uriData)))
        }

        composeTestRule
            .onNodeWithTextAfterScroll(uriData.uri)
            .onSiblings()
            .filterToOne(hasContentDescription("Launch"))
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.Login.LaunchClick(uriData.uri))
        }
    }

    @Test
    fun `in login state, on copy URI click should send CopyUriClick`() {
        val uriData = VaultItemState.ViewState.Content.UriData(
            uri = "www.example.com",
            isCopyable = true,
            isLaunchable = true,
        )
        mutableStateFlow.update {
            it.copy(viewState = EMPTY_LOGIN_VIEW_STATE.copy(uris = listOf(uriData)))
        }

        composeTestRule
            .onNodeWithTextAfterScroll(uriData.uri)
            .onSiblings()
            .filterToOne(hasContentDescription("Copy"))
            .performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.Login.CopyUriClick(uriData.uri))
        }
    }

    @Test
    fun `in login state, on show hidden field click should send HiddenFieldVisibilityClicked`() {
        val textField = VaultItemState.ViewState.Content.Custom.HiddenField(
            name = "hidden",
            value = "hidden password",
            isCopyable = true,
            isVisible = false,
        )
        mutableStateFlow.update {
            it.copy(viewState = EMPTY_LOGIN_VIEW_STATE.copy(customFields = listOf(textField)))
        }

        composeTestRule
            .onNodeWithTextAfterScroll(textField.name)
            .onChildren()
            .filterToOne(hasContentDescription("Show"))
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultItemAction.Login.HiddenFieldVisibilityClicked(
                    field = textField,
                    isVisible = true,
                ),
            )
        }
    }

    @Test
    fun `in login state, copy hidden field button should be displayed according to state`() {
        val hiddenField = VaultItemState.ViewState.Content.Custom.HiddenField(
            name = "hidden",
            value = "hidden password",
            isCopyable = true,
            isVisible = false,
        )
        mutableStateFlow.update {
            it.copy(viewState = EMPTY_LOGIN_VIEW_STATE.copy(customFields = listOf(hiddenField)))
        }

        composeTestRule
            .onNodeWithTextAfterScroll(hiddenField.name)
            .onSiblings()
            .filterToOne(hasContentDescription("Copy"))
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                viewState = EMPTY_LOGIN_VIEW_STATE.copy(
                    customFields = listOf(hiddenField.copy(isCopyable = false)),
                ),
            )
        }

        composeTestRule
            .onNodeWithTextAfterScroll(hiddenField.name)
            .onSiblings()
            .filterToOne(hasContentDescription("Copy"))
            .assertDoesNotExist()
    }

    @Test
    fun `in login state, on copy hidden field click should send CopyCustomHiddenFieldClick`() {
        val hiddenField = VaultItemState.ViewState.Content.Custom.HiddenField(
            name = "hidden",
            value = "hidden password",
            isCopyable = true,
            isVisible = false,
        )
        mutableStateFlow.update {
            it.copy(viewState = EMPTY_LOGIN_VIEW_STATE.copy(customFields = listOf(hiddenField)))
        }

        composeTestRule
            .onNodeWithTextAfterScroll(hiddenField.name)
            .onSiblings()
            .filterToOne(hasContentDescription("Copy"))
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultItemAction.Login.CopyCustomHiddenFieldClick(hiddenField.value),
            )
        }
    }

    @Test
    fun `in login state, on copy text field click should send CopyCustomTextFieldClick`() {
        val textField = VaultItemState.ViewState.Content.Custom.TextField(
            name = "text",
            value = "value",
            isCopyable = true,
        )
        mutableStateFlow.update {
            it.copy(viewState = EMPTY_LOGIN_VIEW_STATE.copy(customFields = listOf(textField)))
        }

        composeTestRule
            .onNodeWithTextAfterScroll(textField.name)
            .onSiblings()
            .filterToOne(hasContentDescription("Copy"))
            .performClick()

        verify {
            viewModel.trySendAction(
                VaultItemAction.Login.CopyCustomTextFieldClick(textField.value),
            )
        }
    }

    @Test
    fun `in login state, text field copy button should be displayed according to state`() {
        val textField = VaultItemState.ViewState.Content.Custom.TextField(
            name = "text",
            value = "value",
            isCopyable = true,
        )
        mutableStateFlow.update {
            it.copy(viewState = EMPTY_LOGIN_VIEW_STATE.copy(customFields = listOf(textField)))
        }

        composeTestRule
            .onNodeWithTextAfterScroll(textField.name)
            .onSiblings()
            .filterToOne(hasContentDescription("Copy"))
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                viewState = EMPTY_LOGIN_VIEW_STATE.copy(
                    customFields = listOf(textField.copy(isCopyable = false)),
                ),
            )
        }

        composeTestRule
            .onNodeWithTextAfterScroll(textField.name)
            .onSiblings()
            .filterToOne(hasContentDescription("Copy"))
            .assertDoesNotExist()
    }

    @Test
    fun `in login state, on password history click should send PasswordHistoryClick`() {
        mutableStateFlow.update {
            it.copy(viewState = EMPTY_LOGIN_VIEW_STATE.copy(passwordHistoryCount = 5))
        }

        composeTestRule.onNodeWithTextAfterScroll("5")
        composeTestRule.onNodeWithText("5").performClick()

        verify {
            viewModel.trySendAction(VaultItemAction.Login.PasswordHistoryClick)
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
            viewModel.trySendAction(VaultItemAction.EditClick)
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
        composeTestRule.onNode(isProgressBar).assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(viewState = VaultItemState.ViewState.Error("Fail".asText()))
        }
        composeTestRule.onNode(isProgressBar).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_LOGIN_VIEW_STATE)
        }
        composeTestRule.onNode(isProgressBar).assertDoesNotExist()
    }

    @Test
    fun `in login state, username should be displayed according to state`() {
        val username = "the username"
        mutableStateFlow.update { it.copy(viewState = DEFAULT_LOGIN_VIEW_STATE) }
        composeTestRule.onNodeWithTextAfterScroll(username).assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_LOGIN_VIEW_STATE.copy(username = null))
        }

        composeTestRule.assertScrollableNodeDoesNotExist(username)
    }

    @Test
    fun `in login state, uris should be displayed according to state`() {
        mutableStateFlow.update { it.copy(viewState = DEFAULT_LOGIN_VIEW_STATE) }
        composeTestRule.onNodeWithTextAfterScroll("URIs").assertIsDisplayed()
        composeTestRule.onNodeWithTextAfterScroll("URI").assertIsDisplayed()
        composeTestRule.onNodeWithTextAfterScroll("www.example.com").assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_LOGIN_VIEW_STATE.copy(uris = emptyList()))
        }

        composeTestRule.assertScrollableNodeDoesNotExist("URIs")
        composeTestRule.assertScrollableNodeDoesNotExist("URI")
        composeTestRule.assertScrollableNodeDoesNotExist("www.example.com")
    }

    @Test
    fun `in login state, notes should be displayed according to state`() {
        mutableStateFlow.update { it.copy(viewState = DEFAULT_LOGIN_VIEW_STATE) }
        composeTestRule.onFirstNodeWithTextAfterScroll("Notes").assertIsDisplayed()
        composeTestRule.onNodeWithTextAfterScroll("Lots of notes").assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_LOGIN_VIEW_STATE.copy(notes = null))
        }

        composeTestRule.assertScrollableNodeDoesNotExist("Notes")
        composeTestRule.assertScrollableNodeDoesNotExist("Lots of notes")
    }

    @Test
    fun `in login state, custom views should be displayed according to state`() {
        mutableStateFlow.update { it.copy(viewState = DEFAULT_LOGIN_VIEW_STATE) }
        composeTestRule.onNodeWithTextAfterScroll("Custom fields").assertIsDisplayed()
        composeTestRule.onNodeWithTextAfterScroll("text").assertIsDisplayed()
        composeTestRule.onNodeWithTextAfterScroll("value").assertIsDisplayed()
        composeTestRule.onNodeWithTextAfterScroll("hidden").assertIsDisplayed()
        composeTestRule.onNodeWithTextAfterScroll("boolean").assertIsDisplayed()
        composeTestRule.onNodeWithTextAfterScroll("linked username").assertIsDisplayed()
        composeTestRule.onNodeWithTextAfterScroll("linked password").assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_LOGIN_VIEW_STATE.copy(customFields = emptyList()))
        }

        composeTestRule.assertScrollableNodeDoesNotExist("Custom fields")
        composeTestRule.assertScrollableNodeDoesNotExist("text")
        composeTestRule.assertScrollableNodeDoesNotExist("value")
        composeTestRule.assertScrollableNodeDoesNotExist("hidden")
        composeTestRule.assertScrollableNodeDoesNotExist("boolean")
        composeTestRule.assertScrollableNodeDoesNotExist("linked username")
        composeTestRule.assertScrollableNodeDoesNotExist("linked password")
    }

    @Test
    fun `in login state, password updated should be displayed according to state`() {
        mutableStateFlow.update { it.copy(viewState = DEFAULT_LOGIN_VIEW_STATE) }
        composeTestRule.onNodeWithTextAfterScroll("Password updated: ").assertIsDisplayed()
        composeTestRule.onNodeWithTextAfterScroll("4/14/83 3:56 PM").assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_LOGIN_VIEW_STATE.copy(passwordRevisionDate = null))
        }

        composeTestRule.assertScrollableNodeDoesNotExist("Password updated: ")
        composeTestRule.assertScrollableNodeDoesNotExist("4/14/83 3:56 PM")
    }

    @Test
    fun `in login state, password history should be displayed according to state`() {
        mutableStateFlow.update { it.copy(viewState = DEFAULT_LOGIN_VIEW_STATE) }
        composeTestRule.onNodeWithTextAfterScroll("Password history: ").assertIsDisplayed()
        composeTestRule.onNodeWithTextAfterScroll("1").assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_LOGIN_VIEW_STATE.copy(passwordHistoryCount = null))
        }

        composeTestRule.assertScrollableNodeDoesNotExist("Password history: ")
        composeTestRule.assertScrollableNodeDoesNotExist("1")
    }
}

private const val VAULT_ITEM_ID = "vault_item_id"

private val DEFAULT_STATE: VaultItemState = VaultItemState(
    vaultItemId = VAULT_ITEM_ID,
    viewState = VaultItemState.ViewState.Loading,
    dialog = null,
)

private val DEFAULT_LOGIN_VIEW_STATE: VaultItemState.ViewState.Content.Login =
    VaultItemState.ViewState.Content.Login(
        name = "login cipher",
        lastUpdated = "12/31/69 06:16 PM",
        passwordHistoryCount = 1,
        notes = "Lots of notes",
        isPremiumUser = true,
        customFields = listOf(
            VaultItemState.ViewState.Content.Custom.TextField(
                name = "text",
                value = "value",
                isCopyable = true,
            ),
            VaultItemState.ViewState.Content.Custom.HiddenField(
                name = "hidden",
                value = "hidden password",
                isCopyable = true,
                isVisible = false,
            ),
            VaultItemState.ViewState.Content.Custom.BooleanField(
                name = "boolean",
                value = true,
            ),
            VaultItemState.ViewState.Content.Custom.LinkedField(
                name = "linked username",
                vaultLinkedFieldType = VaultLinkedFieldType.USERNAME,
            ),
            VaultItemState.ViewState.Content.Custom.LinkedField(
                name = "linked password",
                vaultLinkedFieldType = VaultLinkedFieldType.PASSWORD,
            ),
        ),
        requiresReprompt = true,
        username = "the username",
        passwordData = VaultItemState.ViewState.Content.PasswordData(
            password = "the password",
            isVisible = false,
        ),
        uris = listOf(
            VaultItemState.ViewState.Content.UriData(
                uri = "www.example.com",
                isCopyable = true,
                isLaunchable = true,
            ),
        ),
        passwordRevisionDate = "4/14/83 3:56 PM",
        totp = "otpauth://totp/Example:alice@google.com?secret=JBSWY3DPEHPK3PXP&issuer=Example",
    )

private val EMPTY_LOGIN_VIEW_STATE: VaultItemState.ViewState.Content.Login =
    VaultItemState.ViewState.Content.Login(
        name = "login cipher",
        lastUpdated = "12/31/69 06:16 PM",
        passwordHistoryCount = null,
        notes = null,
        isPremiumUser = true,
        customFields = emptyList(),
        requiresReprompt = true,
        username = null,
        passwordData = null,
        uris = emptyList(),
        passwordRevisionDate = null,
        totp = null,
    )
