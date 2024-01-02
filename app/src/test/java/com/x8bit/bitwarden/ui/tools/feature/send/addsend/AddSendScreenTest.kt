package com.x8bit.bitwarden.ui.tools.feature.send.addsend

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AddSendScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled = false
    private val mutableEventFlow = MutableSharedFlow<AddSendEvent>(
        extraBufferCapacity = Int.MAX_VALUE,
    )
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<NewSendViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            AddSendScreen(
                viewModel = viewModel,
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
            it.copy(name = "input")
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
            selectedType = AddSendState.SendType.File,
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
                selectedType = AddSendState.SendType.Text(
                    input = "input",
                    isHideByDefaultChecked = false,
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
                selectedType = AddSendState.SendType.Text(
                    input = "",
                    isHideByDefaultChecked = true,
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
            it.copy(maxAccessCount = 3)
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
    fun `max access count decrement when set to 1 should do nothing`() =
        runTest {
            mutableStateFlow.update {
                it.copy(maxAccessCount = 1)
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
            verify(exactly = 0) { viewModel.trySendAction(any()) }
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
                passwordInput = "input",
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
                noteInput = "input",
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
                isHideEmailChecked = true,
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
                isDeactivateChecked = true,
            )
        }
        composeTestRule
            .onNodeWithText("Deactivate this Send", substring = true)
            .assertIsOn()
    }

    companion object {
        private val DEFAULT_STATE = AddSendState(
            name = "",
            maxAccessCount = null,
            passwordInput = "",
            noteInput = "",
            isHideEmailChecked = false,
            isDeactivateChecked = false,
            selectedType = AddSendState.SendType.Text(
                input = "",
                isHideByDefaultChecked = false,
            ),
        )
    }
}
