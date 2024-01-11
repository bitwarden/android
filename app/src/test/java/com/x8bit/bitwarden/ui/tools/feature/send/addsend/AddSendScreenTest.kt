package com.x8bit.bitwarden.ui.tools.feature.send.addsend

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
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.IntentHandler
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.model.AddSendType
import com.x8bit.bitwarden.ui.util.isEditableText
import com.x8bit.bitwarden.ui.util.isProgressBar
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.ZonedDateTime

@Suppress("LargeClass")
class AddSendScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled = false

    private val intentHandler: IntentHandler = mockk {
        every { shareText(any()) } just runs
    }
    private val mutableEventFlow = bufferedMutableSharedFlow<AddSendEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<AddSendViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            AddSendScreen(
                viewModel = viewModel,
                intentHandler = intentHandler,
                onNavigateBack = { onNavigateBackCalled = true },
            )
        }
    }

    @Test
    fun `on NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(AddSendEvent.NavigateBack)
        assert(onNavigateBackCalled)
    }

    @Test
    fun `on ShowShareSheet should call shareText on IntentHandler`() {
        val text = "sharable stuff"
        mutableEventFlow.tryEmit(AddSendEvent.ShowShareSheet(text))
        verify {
            intentHandler.shareText(text)
        }
    }

    @Test
    fun `on close icon click should send CloseClick`() {
        composeTestRule
            .onNodeWithContentDescription("Close")
            .performClick()
        verify { viewModel.trySendAction(AddSendAction.CloseClick) }
    }

    @Test
    fun `on save click should send SaveClick`() {
        composeTestRule
            .onNodeWithText("Save")
            .performClick()
        verify { viewModel.trySendAction(AddSendAction.SaveClick) }
    }

    @Test
    fun `on overflow button click should display overflow menu`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            addSendType = AddSendType.EditItem(sendItemId = "sendId"),
        )

        composeTestRule
            .onNodeWithContentDescription("More")
            .performClick()

        composeTestRule
            .onNodeWithText("Remove password")
            .assert(hasAnyAncestor(isPopup()))
            .isDisplayed()
        composeTestRule
            .onNodeWithText("Copy link")
            .assert(hasAnyAncestor(isPopup()))
            .isDisplayed()
        composeTestRule
            .onNodeWithText("Share link")
            .assert(hasAnyAncestor(isPopup()))
            .isDisplayed()
        composeTestRule
            .onNodeWithText("Delete")
            .assert(hasAnyAncestor(isPopup()))
            .isDisplayed()
    }

    @Test
    fun `on overflow remove password button click should send RemovePasswordClick`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            addSendType = AddSendType.EditItem(sendItemId = "sendId"),
        )

        composeTestRule
            .onNodeWithContentDescription("More")
            .performClick()

        composeTestRule
            .onNodeWithText("Remove password")
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(AddSendAction.RemovePasswordClick)
        }
    }

    @Test
    fun `on overflow remove Share link button click should send ShareLinkClick`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            addSendType = AddSendType.EditItem(sendItemId = "sendId"),
        )

        composeTestRule
            .onNodeWithContentDescription("More")
            .performClick()

        composeTestRule
            .onNodeWithText("Share link")
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(AddSendAction.ShareLinkClick)
        }
    }

    @Test
    fun `on overflow remove Delete button click should send DeleteClick`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            addSendType = AddSendType.EditItem(sendItemId = "sendId"),
        )

        composeTestRule
            .onNodeWithContentDescription("More")
            .performClick()

        composeTestRule
            .onNodeWithText("Delete")
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(AddSendAction.DeleteClick)
        }
    }

    @Test
    fun `on overflow remove Copy link button click should send CopyLinkClick`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            addSendType = AddSendType.EditItem(sendItemId = "sendId"),
        )

        composeTestRule
            .onNodeWithContentDescription("More")
            .performClick()

        composeTestRule
            .onNodeWithText("Copy link")
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(AddSendAction.CopyLinkClick)
        }
    }

    @Test
    fun `on name input change should send NameChange`() {
        composeTestRule
            .onNodeWithText("Name")
            .performTextInput("input")
        verify { viewModel.trySendAction(AddSendAction.NameChange("input")) }
    }

    @Test
    fun `name input should change according to the state`() {
        composeTestRule
            .onNodeWithText("Name")
            .assertTextEquals(
                "Name",
                "A friendly name to describe this Send.",
                "",
            )

        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_VIEW_STATE.copy(
                    common = DEFAULT_COMMON_STATE.copy(name = "input"),
                ),
            )
        }
        composeTestRule
            .onNodeWithText("Name")
            .assertTextEquals(
                "Name",
                "A friendly name to describe this Send.",
                "input",
            )
    }

    @Test
    fun `segmented buttons should appear based on state`() {
        mutableStateFlow.update { it.copy(addSendType = AddSendType.AddItem) }
        composeTestRule
            .onNodeWithText("Type")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("File")
            .filterToOne(!isEditableText)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Text")
            .filterToOne(!isEditableText)
            .performScrollTo()
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(addSendType = AddSendType.EditItem(sendItemId = "sendId"))
        }

        composeTestRule
            .onNodeWithText("Type")
            .assertIsNotDisplayed()
        composeTestRule
            .onAllNodesWithText("File")
            .filterToOne(!isEditableText)
            .assertIsNotDisplayed()
        composeTestRule
            .onAllNodesWithText("Text")
            .filterToOne(!isEditableText)
            .assertIsNotDisplayed()
    }

    @Test
    fun `File segmented button click should send FileTypeClick`() {
        composeTestRule
            .onNodeWithText("File")
            .performClick()
        verify { viewModel.trySendAction(AddSendAction.FileTypeClick) }
    }

    @Test
    fun `Text segmented button click should send TextTypeClick`() {
        composeTestRule
            .onAllNodesWithText("Text")[0]
            .performClick()
        verify { viewModel.trySendAction(AddSendAction.TextTypeClick) }
    }

    @Test
    fun `Choose file button click should send ChooseFileClick`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(
            viewState = DEFAULT_VIEW_STATE.copy(
                selectedType = AddSendState.ViewState.Content.SendType.File,
            ),
        )
        composeTestRule
            .onNodeWithText("Choose file")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(AddSendAction.ChooseFileClick) }
    }

    @Test
    fun `text input change should send TextChange`() {
        composeTestRule
            .onAllNodesWithText("Text")[1]
            .performTextInput("input")
        viewModel.trySendAction(AddSendAction.TextChange("input"))
    }

    @Test
    fun `text input should change according to the state`() {
        composeTestRule
            .onAllNodesWithText("Text")
            .filterToOne(hasSetTextAction())
            .assertTextEquals(
                "Text",
                "The text you want to send.",
                "",
            )

        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_VIEW_STATE.copy(
                    selectedType = AddSendState.ViewState.Content.SendType.Text(
                        input = "input",
                        isHideByDefaultChecked = false,
                    ),
                ),
            )
        }
        composeTestRule
            .onAllNodesWithText("Text")
            .filterToOne(hasSetTextAction())
            .assertTextEquals(
                "Text",
                "The text you want to send.",
                "input",
            )
    }

    @Test
    fun `hide by default toggle should send HideByDefaultToggle`() {
        composeTestRule
            .onNodeWithText(text = "When accessing the Send", substring = true)
            .performClick()
        viewModel.trySendAction(AddSendAction.HideByDefaultToggle(true))
    }

    @Test
    fun `hide text toggle should change according to the state`() {
        composeTestRule
            .onNodeWithText("When accessing the Send,", substring = true)
            .assertIsOff()

        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_VIEW_STATE.copy(
                    selectedType = AddSendState.ViewState.Content.SendType.Text(
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
            .onNodeWithContentDescription("Deletion date", substring = true)
            .assertDoesNotExist()
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
            .onNodeWithText("Notes")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText("Hide my email address from recipients")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText("Deactivate this Send", substring = true)
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText("Options")
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNodeWithContentDescription("Deletion date", substring = true)
            .assertExists()
        composeTestRule
            .onNodeWithContentDescription("Expiration date", substring = true)
            .assertExists()
        composeTestRule
            .onNodeWithText("Maximum access count")
            .assertExists()
        composeTestRule
            .onNodeWithText("New password")
            .assertExists()
        composeTestRule
            .onNodeWithText("Notes")
            .assertExists()
        composeTestRule
            .onNodeWithText("Hide my email address from recipients")
            .assertExists()
        composeTestRule
            .onNodeWithText("Deactivate this Send", substring = true)
            .assertExists()
    }

    @Test
    fun `max access count decrement should be disabled when max access count is null`() = runTest {
        // Expand options section:
        composeTestRule
            .onNodeWithText("Options")
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
            .onNodeWithText("Options")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onNodeWithContentDescription("\u2212")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(AddSendAction.MaxAccessCountChange(2)) }
    }

    @Test
    fun `on max access count increment should send MaxAccessCountChange`() = runTest {
        // Expand options section:
        composeTestRule
            .onNodeWithText("Options")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onNodeWithContentDescription("+")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(AddSendAction.MaxAccessCountChange(1)) }
    }

    @Test
    fun `on password input change should send PasswordChange`() = runTest {
        // Expand options section:
        composeTestRule
            .onNodeWithText("Options")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onNodeWithText("New password")
            .performTextInput("input")
        verify { viewModel.trySendAction(AddSendAction.PasswordChange("input")) }
    }

    @Test
    fun `password input should change according to the state`() {
        // Expand options section:
        composeTestRule
            .onNodeWithText("Options")
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNodeWithText("New password")
            .assertTextEquals(
                "New password",
                "Optionally require a password for users to access this Send.",
                "",
            )

        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_VIEW_STATE.copy(
                    common = DEFAULT_COMMON_STATE.copy(passwordInput = "input"),
                ),
            )
        }
        composeTestRule
            .onNodeWithText("New password")
            .assertTextEquals(
                "New password",
                "Optionally require a password for users to access this Send.",
                "•••••",
            )
    }

    @Test
    fun `on notes input change should send NoteChange`() = runTest {
        // Expand options section:
        composeTestRule
            .onNodeWithText("Options")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onNodeWithText("Notes")
            .performTextInput("input")
        verify { viewModel.trySendAction(AddSendAction.NoteChange("input")) }
    }

    @Test
    fun `note input should change according to the state`() {
        // Expand options section:
        composeTestRule
            .onNodeWithText("Options")
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNodeWithText("Notes")
            .assertTextEquals(
                "Notes",
                "Private notes about this Send.",
                "",
            )

        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_VIEW_STATE.copy(
                    common = DEFAULT_COMMON_STATE.copy(noteInput = "input"),
                ),
            )
        }
        composeTestRule
            .onNodeWithText("Notes")
            .assertTextEquals(
                "Notes",
                "Private notes about this Send.",
                "input",
            )
    }

    @Test
    fun `on hide email toggle should send HideMyEmailToggle`() = runTest {
        // Expand options section:
        composeTestRule
            .onNodeWithText("Options")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onNodeWithText("Hide my email address", substring = true)
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(AddSendAction.HideMyEmailToggle(true)) }
    }

    @Test
    fun `hide email toggle should change according to the state`() {
        // Expand options section:
        composeTestRule
            .onNodeWithText("Options")
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
    fun `on deactivate this send toggle should send DeactivateThisSendToggle`() = runTest {
        // Expand options section:
        composeTestRule
            .onNodeWithText("Options")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onNodeWithText("Deactivate this Send", substring = true)
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(AddSendAction.DeactivateThisSendToggle(true)) }
    }

    @Test
    fun `deactivate send toggle should change according to the state`() {
        // Expand options section:
        composeTestRule
            .onNodeWithText("Options")
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNodeWithText("Deactivate this Send", substring = true)
            .assertIsOff()

        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_VIEW_STATE.copy(
                    common = DEFAULT_COMMON_STATE.copy(isDeactivateChecked = true),
                ),
            )
        }
        composeTestRule
            .onNodeWithText("Deactivate this Send", substring = true)
            .assertIsOn()
    }

    @Test
    fun `in edit mode, clear button should be enabled based on state`() {
        mutableStateFlow.update {
            it.copy(addSendType = AddSendType.EditItem(sendItemId = "sendId"))
        }

        composeTestRule
            .onNodeWithText("Options")
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNodeWithText("Clear")
            .performScrollTo()
            .assertIsNotEnabled()

        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_VIEW_STATE.copy(
                    common = DEFAULT_COMMON_STATE.copy(
                        expirationDate = ZonedDateTime.parse("2023-10-27T12:00:00Z"),
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithText("Clear")
            .performScrollTo()
            .assertIsEnabled()
    }

    @Test
    fun `in edit mode, clear button should send ClearExpirationDate`() {
        mutableStateFlow.update {
            it.copy(
                addSendType = AddSendType.EditItem(sendItemId = "sendId"),
                viewState = DEFAULT_VIEW_STATE.copy(
                    common = DEFAULT_COMMON_STATE.copy(
                        expirationDate = ZonedDateTime.parse("2023-10-27T12:00:00Z"),
                    ),
                ),
            )
        }

        composeTestRule
            .onNodeWithText("Options")
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNodeWithText("Clear")
            .performScrollTo()
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(AddSendAction.ClearExpirationDate)
        }
    }

    @Test
    fun `progressbar should be displayed according to state`() {
        mutableStateFlow.update {
            it.copy(viewState = AddSendState.ViewState.Loading)
        }
        composeTestRule.onNode(isProgressBar).assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(viewState = AddSendState.ViewState.Error("Fail".asText()))
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
            it.copy(viewState = AddSendState.ViewState.Error(errorMessage.asText()))
        }
        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(viewState = AddSendState.ViewState.Loading)
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
                dialogState = AddSendState.DialogState.Error(
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
                dialogState = AddSendState.DialogState.Error(
                    title = "Fail Title".asText(),
                    message = "Fail Message".asText(),
                ),
            )
        }
        composeTestRule
            .onNodeWithText("Ok")
            .performClick()
        verify { viewModel.trySendAction(AddSendAction.DismissDialogClick) }
    }

    @Test
    fun `loading dialog should be displayed according to state`() {
        val loadingMessage = "syncing"
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText(loadingMessage).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(dialogState = AddSendState.DialogState.Loading(loadingMessage.asText()))
        }

        composeTestRule
            .onNodeWithText(loadingMessage)
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))
    }

    companion object {
        private val DEFAULT_COMMON_STATE = AddSendState.ViewState.Content.Common(
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
        )

        private val DEFAULT_SELECTED_TYPE_STATE = AddSendState.ViewState.Content.SendType.Text(
            input = "",
            isHideByDefaultChecked = false,
        )

        private val DEFAULT_VIEW_STATE = AddSendState.ViewState.Content(
            common = DEFAULT_COMMON_STATE,
            selectedType = DEFAULT_SELECTED_TYPE_STATE,
        )

        private val DEFAULT_STATE = AddSendState(
            addSendType = AddSendType.AddItem,
            viewState = DEFAULT_VIEW_STATE,
            dialogState = null,
            isPremiumUser = false,
            baseWebSendUrl = "https://vault.bitwarden.com/#/send/",
        )
    }
}
