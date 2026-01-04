package com.x8bit.bitwarden.ui.tools.feature.send.addedit

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.manager.exit.ExitManager
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.assertNoDialogExists
import com.bitwarden.ui.util.isEditableText
import com.bitwarden.ui.util.isProgressBar
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import com.x8bit.bitwarden.ui.platform.manager.permissions.FakePermissionManager
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.model.AddEditSendType
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendItemType
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZonedDateTime

@Suppress("LargeClass")
class AddEditSendScreenTest : BitwardenComposeTest() {

    private var onNavigateBackCalled = false
    private var onNavigateUpToSearchOrRootCalled = false

    private val exitManager: ExitManager = mockk(relaxed = true) {
        every { exitApplication() } just runs
    }
    private val permissionsManager = FakePermissionManager()
    private val intentManager: IntentManager = mockk(relaxed = true) {
        every { shareText(any()) } just runs
    }
    private val mutableEventFlow = bufferedMutableSharedFlow<AddEditSendEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<AddEditSendViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        setContent(
            exitManager = exitManager,
            intentManager = intentManager,
            permissionsManager = permissionsManager,
        ) {
            AddEditSendScreen(
                viewModel = viewModel,
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateUpToSearchOrRoot = { onNavigateUpToSearchOrRootCalled = true },
            )
        }
    }

    @Test
    fun `on NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(AddEditSendEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `on NavigateUpToSearchOrRoot should call onNavigateUpToSearchOrRootCalled`() {
        mutableEventFlow.tryEmit(AddEditSendEvent.NavigateUpToSearchOrRoot)
        assertTrue(onNavigateUpToSearchOrRootCalled)
    }

    @Test
    fun `ExitApp should call exitApplication on ExitManager`() {
        mutableEventFlow.tryEmit(AddEditSendEvent.ExitApp)
        verify {
            exitManager.exitApplication()
        }
    }

    @Test
    fun `on ShowSnackbar event should display the snackbar`() {
        val message = "message"
        val data = BitwardenSnackbarData(message = message.asText())
        mutableEventFlow.tryEmit(AddEditSendEvent.ShowSnackbar(data = data))
        composeTestRule
            .onNodeWithText(text = message)
            .assertIsDisplayed()
    }

    @Test
    fun `on ShowShareSheet should call shareText on IntentManager`() {
        val text = "sharable stuff"
        mutableEventFlow.tryEmit(AddEditSendEvent.ShowShareSheet(text))
        verify {
            intentManager.shareText(text)
        }
    }

    @Test
    fun `on close icon click should send CloseClick`() {
        composeTestRule
            .onNodeWithContentDescription("Close")
            .performClick()
        verify { viewModel.trySendAction(AddEditSendAction.CloseClick) }
    }

    @Test
    fun `on system back should send CloseClick`() {
        backDispatcher?.onBackPressed()
        verify { viewModel.trySendAction(AddEditSendAction.CloseClick) }
    }

    @Test
    fun `display navigation icon according to state`() {
        mutableStateFlow.update { it.copy(isShared = false) }
        composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed()
        mutableStateFlow.update { it.copy(isShared = true) }
        composeTestRule.onNodeWithContentDescription("Close").assertDoesNotExist()
    }

    @Test
    fun `screen title should update according to state`() {
        mutableStateFlow.update {
            it.copy(sendType = SendItemType.TEXT, addEditSendType = AddEditSendType.AddItem)
        }
        composeTestRule.onNodeWithText(text = "New text Send").assertIsDisplayed()
        mutableStateFlow.update {
            it.copy(
                sendType = SendItemType.TEXT,
                addEditSendType = AddEditSendType.EditItem(sendItemId = "send_id"),
            )
        }
        composeTestRule.onNodeWithText(text = "Edit text Send").assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(sendType = SendItemType.FILE, addEditSendType = AddEditSendType.AddItem)
        }
        composeTestRule.onNodeWithText(text = "New file Send").assertIsDisplayed()
        mutableStateFlow.update {
            it.copy(
                sendType = SendItemType.FILE,
                addEditSendType = AddEditSendType.EditItem(sendItemId = "send_id"),
            )
        }
        composeTestRule.onNodeWithText(text = "Edit file Send").assertIsDisplayed()
    }

    @Test
    fun `on save click should send SaveClick`() {
        composeTestRule
            .onNodeWithText("Save")
            .performClick()
        verify { viewModel.trySendAction(AddEditSendAction.SaveClick) }
    }

    @Test
    fun `on overflow button click should display overflow menu`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            addEditSendType = AddEditSendType.EditItem(sendItemId = "sendId"),
        )

        composeTestRule
            .onNodeWithContentDescription("More")
            .performClick()

        composeTestRule
            .onNodeWithText("Remove password")
            .assert(hasAnyAncestor(isPopup()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Copy link")
            .assert(hasAnyAncestor(isPopup()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Share link")
            .assert(hasAnyAncestor(isPopup()))
            .assertIsDisplayed()
    }

    @Test
    fun `on overflow button should not be present when policy disables send`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            addEditSendType = AddEditSendType.EditItem(sendItemId = "sendId"),
            policyDisablesSend = true,
        )

        composeTestRule
            .onNodeWithContentDescription("More")
            .assertDoesNotExist()
    }

    @Test
    fun `overflow remove password button should be hidden when hasPassword is false`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            addEditSendType = AddEditSendType.EditItem(sendItemId = "sendId"),
            viewState = DEFAULT_VIEW_STATE.copy(
                common = DEFAULT_COMMON_STATE.copy(hasPassword = false),
            ),
        )

        composeTestRule
            .onNodeWithContentDescription("More")
            .performClick()

        composeTestRule
            .onNodeWithText("Remove password")
            .assertDoesNotExist()
    }

    @Test
    fun `on overflow remove password button click should send RemovePasswordClick`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            addEditSendType = AddEditSendType.EditItem(sendItemId = "sendId"),
        )

        composeTestRule
            .onNodeWithContentDescription("More")
            .performClick()

        composeTestRule
            .onNodeWithText("Remove password")
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(AddEditSendAction.RemovePasswordClick)
        }
    }

    @Test
    fun `on overflow remove Share link button click should send ShareLinkClick`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            addEditSendType = AddEditSendType.EditItem(sendItemId = "sendId"),
        )

        composeTestRule
            .onNodeWithContentDescription("More")
            .performClick()

        composeTestRule
            .onNodeWithText("Share link")
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(AddEditSendAction.ShareLinkClick)
        }
    }

    @Test
    fun `on overflow remove Copy link button click should send CopyLinkClick`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            addEditSendType = AddEditSendType.EditItem(sendItemId = "sendId"),
        )

        composeTestRule
            .onNodeWithContentDescription("More")
            .performClick()

        composeTestRule
            .onNodeWithText("Copy link")
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(AddEditSendAction.CopyLinkClick)
        }
    }

    @Test
    fun `on Delete button click should Display delete confirmation dialog`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            addEditSendType = AddEditSendType.EditItem(sendItemId = "sendId"),
        )

        composeTestRule
            .onNodeWithText(text = "Delete Send")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onNodeWithText(text = "Are you sure you want to delete this Send?")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `on delete confirmation dialog yes click should send DeleteClick`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            addEditSendType = AddEditSendType.EditItem(sendItemId = "sendId"),
        )

        composeTestRule
            .onNodeWithText(text = "Delete Send")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onNodeWithText(text = "Yes")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(AddEditSendAction.DeleteClick)
        }
    }

    @Test
    fun `policy warning should update according to state`() {
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
    fun `on name input change should send NameChange`() {
        composeTestRule
            .onNodeWithText("Send name (required)")
            .performTextInput("input")
        verify { viewModel.trySendAction(AddEditSendAction.NameChange("input")) }
    }

    @Test
    fun `name input should change according to the state`() {
        composeTestRule
            .onNodeWithText("Send name (required)")
            .assertTextEquals("Send name (required)", "")

        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_VIEW_STATE.copy(
                    common = DEFAULT_COMMON_STATE.copy(name = "input"),
                ),
            )
        }
        composeTestRule
            .onNodeWithText("Send name (required)")
            .assertTextEquals("Send name (required)", "input")
    }

    @Test
    fun `Choose file button click with permission should send ChooseFileClick`() {
        permissionsManager.checkPermissionResult = true
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = DEFAULT_VIEW_STATE.copy(
                selectedType = AddEditSendState.ViewState.Content.SendType.File(
                    name = null,
                    displaySize = null,
                    sizeBytes = null,
                    uri = null,
                ),
            ),
        )
        composeTestRule
            .onNodeWithText("Choose file")
            .performScrollTo()
            .performClick()
        verify {
            viewModel.trySendAction(
                AddEditSendAction.ChooseFileClick(isCameraPermissionGranted = true),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `Choose file button click without permission should request permission and send ChooseFileClick`() {
        permissionsManager.checkPermissionResult = false
        permissionsManager.getPermissionsResult = false
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = DEFAULT_VIEW_STATE.copy(
                selectedType = AddEditSendState.ViewState.Content.SendType.File(
                    name = null,
                    displaySize = null,
                    sizeBytes = null,
                    uri = null,
                ),
            ),
        )
        composeTestRule
            .onNodeWithText("Choose file")
            .performScrollTo()
            .performClick()
        verify {
            viewModel.trySendAction(
                AddEditSendAction.ChooseFileClick(isCameraPermissionGranted = false),
            )
        }
    }

    @Test
    fun `text input change should send TextChange`() {
        composeTestRule
            .onAllNodesWithText("Text to share")
            .filterToOne(isEditableText)
            .performScrollTo()
            .performTextInput("input")
        verify(exactly = 1) {
            viewModel.trySendAction(AddEditSendAction.TextChange("input"))
        }
    }

    @Test
    fun `text input should change according to the state`() {
        composeTestRule
            .onAllNodesWithText("Text to share")
            .filterToOne(hasSetTextAction())
            .assertTextEquals("Text to share", "")

        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_VIEW_STATE.copy(
                    selectedType = AddEditSendState.ViewState.Content.SendType.Text(
                        input = "input",
                        isHideByDefaultChecked = false,
                    ),
                ),
            )
        }
        composeTestRule
            .onAllNodesWithText("Text to share")
            .filterToOne(hasSetTextAction())
            .assertTextEquals("Text to share", "input")
    }

    @Test
    fun `hide by default toggle should send HideByDefaultToggle`() {
        composeTestRule
            .onNodeWithText(text = "When accessing the Send", substring = true)
            .performClick()
        viewModel.trySendAction(AddEditSendAction.HideByDefaultToggle(true))
    }

    @Test
    fun `hide text toggle should change according to the state`() {
        composeTestRule
            .onNodeWithText("When accessing the Send,", substring = true)
            .assertIsOff()

        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_VIEW_STATE.copy(
                    selectedType = AddEditSendState.ViewState.Content.SendType.Text(
                        input = "",
                        isHideByDefaultChecked = true,
                    ),
                ),
            )
        }
        composeTestRule
            .onNodeWithText("When accessing the Send,", substring = true)
            .assertIsOn()
    }

    @Test
    fun `options sections should start hidden and show after options clicked`() {
        composeTestRule
            .onNodeWithContentDescription("Expiration date", substring = true)
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText("Maximum access count")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText("New password")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText("Private notes")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText("Hide my email address from recipients")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText("Additional options")
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNodeWithText("Maximum access count")
            .assertExists()
        composeTestRule
            .onNodeWithText("New password")
            .assertExists()
        composeTestRule
            .onNodeWithText("Private notes")
            .assertExists()
        composeTestRule
            .onNodeWithText("Hide my email address from recipients")
            .assertExists()
    }

    @Test
    fun `max access count decrement should be disabled when max access count is null`() = runTest {
        // Expand options section:
        composeTestRule
            .onNodeWithText("Additional options")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onNodeWithContentDescription("\u2212")
            .performScrollTo()
            .performClick()
    }

    @Test
    fun `max access count decrement should send MaxAccessCountChange`() = runTest {
        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_VIEW_STATE.copy(
                    common = DEFAULT_COMMON_STATE.copy(maxAccessCount = 3),
                ),
            )
        }
        // Expand options section:
        composeTestRule
            .onNodeWithText("Additional options")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onNodeWithContentDescription("\u2212")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(AddEditSendAction.MaxAccessCountChange(2)) }
    }

    @Test
    fun `on max access count increment should send MaxAccessCountChange`() = runTest {
        // Expand options section:
        composeTestRule
            .onNodeWithText("Additional options")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onNodeWithContentDescription("+")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(AddEditSendAction.MaxAccessCountChange(1)) }
    }

    @Test
    fun `on password input change should send PasswordChange`() = runTest {
        // Expand options section:
        composeTestRule
            .onNodeWithText("Additional options")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onNodeWithText("New password")
            .performTextInput("input")
        verify { viewModel.trySendAction(AddEditSendAction.PasswordChange("input")) }
    }

    @Test
    fun `password input should change according to the state`() {
        // Expand options section:
        composeTestRule
            .onNodeWithText("Additional options")
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNodeWithText("New password")
            .assertTextEquals("New password", "")

        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_VIEW_STATE.copy(
                    common = DEFAULT_COMMON_STATE.copy(passwordInput = "input"),
                ),
            )
        }
        composeTestRule
            .onNodeWithText("New password")
            .assertTextEquals("New password", "•••••")
    }

    @Test
    fun `on notes input change should send NoteChange`() = runTest {
        // Expand options section:
        composeTestRule
            .onNodeWithText("Additional options")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onNodeWithText("Private notes")
            .performTextInput("input")
        verify { viewModel.trySendAction(AddEditSendAction.NoteChange("input")) }
    }

    @Test
    fun `note input should change according to the state`() {
        // Expand options section:
        composeTestRule
            .onNodeWithText("Additional options")
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNodeWithText("Private notes")
            .assertTextEquals("Private notes", "")

        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_VIEW_STATE.copy(
                    common = DEFAULT_COMMON_STATE.copy(noteInput = "input"),
                ),
            )
        }
        composeTestRule
            .onNodeWithText("Private notes")
            .assertTextEquals("Private notes", "input")
    }

    @Test
    fun `on hide email toggle should send HideMyEmailToggle`() = runTest {
        // Expand options section:
        composeTestRule
            .onNodeWithText("Additional options")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onNodeWithText("Hide my email address", substring = true)
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(AddEditSendAction.HideMyEmailToggle(true)) }
    }

    @Test
    fun `hide email toggle should change according to the state`() {
        // Expand options section:
        composeTestRule
            .onNodeWithText("Additional options")
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNodeWithText("Hide my email", substring = true)
            .assertIsOff()

        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_VIEW_STATE.copy(
                    common = DEFAULT_COMMON_STATE.copy(isHideEmailChecked = true),
                ),
            )
        }
        composeTestRule
            .onNodeWithText("Hide my email", substring = true)
            .assertIsOn()
    }

    @Test
    fun `hide email toggle should be disabled according to state`() = runTest {
        // Expand options section:
        composeTestRule
            .onNodeWithText("Additional options")
            .performScrollTo()
            .performClick()

        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_VIEW_STATE.copy(
                    common = DEFAULT_COMMON_STATE.copy(
                        isHideEmailAddressEnabled = false,
                    ),
                ),
            )
        }

        // Toggle should be disabled
        composeTestRule
            .onNodeWithText("Hide my email address", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsNotEnabled()

        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_VIEW_STATE.copy(
                    common = DEFAULT_COMMON_STATE.copy(
                        isHideEmailChecked = true,
                    ),
                ),
            )
        }

        // Toggle should be enabled
        composeTestRule
            .onNodeWithText("Hide my email address", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun `progressbar should be displayed according to state`() {
        mutableStateFlow.update {
            it.copy(viewState = AddEditSendState.ViewState.Loading)
        }
        composeTestRule.onNode(isProgressBar).assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(viewState = AddEditSendState.ViewState.Error("Fail".asText()))
        }
        composeTestRule.onNode(isProgressBar).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_VIEW_STATE)
        }
        composeTestRule.onNode(isProgressBar).assertDoesNotExist()
    }

    @Test
    fun `error should be displayed according to state`() {
        val errorMessage = "Fail"
        mutableStateFlow.update {
            it.copy(viewState = AddEditSendState.ViewState.Error(errorMessage.asText()))
        }
        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(viewState = AddEditSendState.ViewState.Loading)
        }
        composeTestRule.onNodeWithText(errorMessage).assertDoesNotExist()
    }

    @Test
    fun `error dialog should be displayed according to state`() {
        val errorTitle = "Fail Title"
        val errorMessage = "Fail Message"
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText(errorMessage).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState = AddEditSendState.DialogState.Error(
                    title = errorTitle.asText(),
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
    fun `error dialog Ok click should send DismissDialogClick`() {
        mutableStateFlow.update {
            it.copy(
                dialogState = AddEditSendState.DialogState.Error(
                    title = "Fail Title".asText(),
                    message = "Fail Message".asText(),
                ),
            )
        }
        composeTestRule
            .onNodeWithText(text = "Okay")
            .performClick()
        verify { viewModel.trySendAction(AddEditSendAction.DismissDialogClick) }
    }

    @Test
    fun `loading dialog should be displayed according to state`() {
        val loadingMessage = "syncing"
        composeTestRule.assertNoDialogExists()
        composeTestRule.onNodeWithText(loadingMessage).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(dialogState = AddEditSendState.DialogState.Loading(loadingMessage.asText()))
        }

        composeTestRule
            .onNodeWithText(loadingMessage)
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))
    }

    @Test
    fun `policy send options text should be displayed based on state`() {
        val text = "One or more organization policies are affecting your Send options."

        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_VIEW_STATE,
                policyDisablesSend = true,
            )
        }

        composeTestRule
            .onNodeWithText(text)
            .assertIsNotDisplayed()

        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_VIEW_STATE.copy(
                    common = DEFAULT_COMMON_STATE.copy(
                        isHideEmailAddressEnabled = false,
                    ),
                ),
                policyDisablesSend = false,
            )
        }

        composeTestRule
            .onNodeWithText(text)
            .assertIsDisplayed()
    }
}

private val DEFAULT_COMMON_STATE = AddEditSendState.ViewState.Content.Common(
    name = "",
    currentAccessCount = null,
    maxAccessCount = null,
    passwordInput = "",
    noteInput = "",
    isHideEmailChecked = false,
    isDeactivateChecked = false,
    deletionDate = ZonedDateTime.parse("2023-10-27T12:00:00Z"),
    expirationDate = null,
    sendUrl = null,
    hasPassword = true,
    isHideEmailAddressEnabled = true,
)

private val DEFAULT_SELECTED_TYPE_STATE = AddEditSendState.ViewState.Content.SendType.Text(
    input = "",
    isHideByDefaultChecked = false,
)

private val DEFAULT_VIEW_STATE = AddEditSendState.ViewState.Content(
    common = DEFAULT_COMMON_STATE,
    selectedType = DEFAULT_SELECTED_TYPE_STATE,
)

private val DEFAULT_STATE = AddEditSendState(
    addEditSendType = AddEditSendType.AddItem,
    viewState = DEFAULT_VIEW_STATE,
    dialogState = null,
    shouldFinishOnComplete = false,
    isShared = false,
    baseWebSendUrl = "https://vault.bitwarden.com/#/send/",
    policyDisablesSend = false,
    sendType = SendItemType.TEXT,
    isPremium = true,
)
