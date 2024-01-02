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

@Suppress("LargeClass")
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
            viewModel.trySendAction(VaultItemAction.Common.CloseClick)
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
            viewModel.trySendAction(VaultItemAction.Common.DismissDialogClick)
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
            viewModel.trySendAction(VaultItemAction.Common.MasterPasswordSubmit(enteredPassword))
        }
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
                        totp = null,
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
    fun `in login state, on show password click should send CopyPasswordClick`() {
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
    fun `on show hidden field click should send HiddenFieldVisibilityClicked`() {
        val textField = VaultItemState.ViewState.Content.Common.Custom.HiddenField(
            name = "hidden",
            value = "hidden password",
            isCopyable = true,
            isVisible = false,
        )
        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = EMPTY_LOGIN_VIEW_STATE.copy(
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

    @Test
    fun `copy hidden field button should be displayed according to state`() {
        val hiddenField = VaultItemState.ViewState.Content.Common.Custom.HiddenField(
            name = "hidden",
            value = "hidden password",
            isCopyable = true,
            isVisible = false,
        )
        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = EMPTY_LOGIN_VIEW_STATE.copy(
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

    @Test
    fun `on copy hidden field click should send CopyCustomHiddenFieldClick`() {
        val hiddenField = VaultItemState.ViewState.Content.Common.Custom.HiddenField(
            name = "hidden",
            value = "hidden password",
            isCopyable = true,
            isVisible = false,
        )
        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = EMPTY_LOGIN_VIEW_STATE.copy(
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

    @Test
    fun `on copy text field click should send CopyCustomTextFieldClick`() {
        val textField = VaultItemState.ViewState.Content.Common.Custom.TextField(
            name = "text",
            value = "value",
            isCopyable = true,
        )
        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = EMPTY_LOGIN_VIEW_STATE.copy(
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

    @Test
    fun `text field copy button should be displayed according to state`() {
        val textField = VaultItemState.ViewState.Content.Common.Custom.TextField(
            name = "text",
            value = "value",
            isCopyable = true,
        )
        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = EMPTY_LOGIN_VIEW_STATE.copy(
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

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(username = null) }
        }

        composeTestRule.assertScrollableNodeDoesNotExist(username)
    }

    @Test
    fun `in login state, uris should be displayed according to state`() {
        mutableStateFlow.update { it.copy(viewState = DEFAULT_LOGIN_VIEW_STATE) }
        composeTestRule.onNodeWithTextAfterScroll("URIs").assertIsDisplayed()
        composeTestRule.onNodeWithTextAfterScroll("URI").assertIsDisplayed()
        composeTestRule.onNodeWithTextAfterScroll("www.example.com").assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateLoginType(currentState) { copy(uris = emptyList()) }
        }

        composeTestRule.assertScrollableNodeDoesNotExist("URIs")
        composeTestRule.assertScrollableNodeDoesNotExist("URI")
        composeTestRule.assertScrollableNodeDoesNotExist("www.example.com")
    }

    @Test
    fun `notes should be displayed according to state`() {
        mutableStateFlow.update { it.copy(viewState = DEFAULT_LOGIN_VIEW_STATE) }
        composeTestRule.onFirstNodeWithTextAfterScroll("Notes").assertIsDisplayed()
        composeTestRule.onNodeWithTextAfterScroll("Lots of notes").assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateCommonContent(currentState) { copy(notes = null) }
        }

        composeTestRule.assertScrollableNodeDoesNotExist("Notes")
        composeTestRule.assertScrollableNodeDoesNotExist("Lots of notes")
    }

    @Test
    fun `custom views should be displayed according to state`() {
        mutableStateFlow.update { it.copy(viewState = DEFAULT_LOGIN_VIEW_STATE) }
        composeTestRule.onNodeWithTextAfterScroll("Custom fields").assertIsDisplayed()
        composeTestRule.onNodeWithTextAfterScroll("text").assertIsDisplayed()
        composeTestRule.onNodeWithTextAfterScroll("value").assertIsDisplayed()
        composeTestRule.onNodeWithTextAfterScroll("hidden").assertIsDisplayed()
        composeTestRule.onNodeWithTextAfterScroll("boolean").assertIsDisplayed()
        composeTestRule.onNodeWithTextAfterScroll("linked username").assertIsDisplayed()
        composeTestRule.onNodeWithTextAfterScroll("linked password").assertIsDisplayed()

        mutableStateFlow.update { currentState ->
            updateCommonContent(currentState) { copy(customFields = emptyList()) }
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
}

//region Helper functions

@Suppress("MaxLineLength")
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

@Suppress("MaxLineLength")
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

@Suppress("MaxLineLength")
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
        lastUpdated = "12/31/69 06:16 PM",
        name = "login cipher",
        notes = "Lots of notes",
        isPremiumUser = true,
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
            VaultItemState.ViewState.Content.Common.Custom.LinkedField(
                name = "linked username",
                vaultLinkedFieldType = VaultLinkedFieldType.USERNAME,
            ),
            VaultItemState.ViewState.Content.Common.Custom.LinkedField(
                name = "linked password",
                vaultLinkedFieldType = VaultLinkedFieldType.PASSWORD,
            ),
        ),
        requiresReprompt = true,
    )

private val DEFAULT_LOGIN: VaultItemState.ViewState.Content.ItemType.Login =
    VaultItemState.ViewState.Content.ItemType.Login(
        passwordHistoryCount = 1,
        username = "the username",
        passwordData = VaultItemState.ViewState.Content.ItemType.Login.PasswordData(
            password = "the password",
            isVisible = false,
        ),
        uris = listOf(
            VaultItemState.ViewState.Content.ItemType.Login.UriData(
                uri = "www.example.com",
                isCopyable = true,
                isLaunchable = true,
            ),
        ),
        passwordRevisionDate = "4/14/83 3:56 PM",
        totp = "otpauth://totp/Example:alice@google.com?secret=JBSWY3DPEHPK3PXP&issuer=Example",
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

private val EMPTY_COMMON: VaultItemState.ViewState.Content.Common =
    VaultItemState.ViewState.Content.Common(
        name = "login cipher",
        lastUpdated = "12/31/69 06:16 PM",
        notes = null,
        isPremiumUser = true,
        customFields = emptyList(),
        requiresReprompt = true,
    )

private val EMPTY_LOGIN_TYPE: VaultItemState.ViewState.Content.ItemType.Login =
    VaultItemState.ViewState.Content.ItemType.Login(
        username = null,
        passwordData = null,
        passwordHistoryCount = null,
        uris = emptyList(),
        passwordRevisionDate = null,
        totp = null,
    )

private val EMPTY_LOGIN_VIEW_STATE: VaultItemState.ViewState.Content =
    VaultItemState.ViewState.Content(
        common = EMPTY_COMMON,
        type = EMPTY_LOGIN_TYPE,
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
