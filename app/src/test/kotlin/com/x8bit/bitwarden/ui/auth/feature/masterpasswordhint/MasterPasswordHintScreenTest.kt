package com.x8bit.bitwarden.ui.auth.feature.masterpasswordhint

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.assertNoDialogExists
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MasterPasswordHintScreenTest : BitwardenComposeTest() {

    private var onNavigateBackCalled = false
    private val mutableEventFlow = bufferedMutableSharedFlow<MasterPasswordHintEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<MasterPasswordHintViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        setContent {
            MasterPasswordHintScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `email input change should send EmailInputChange action`() {
        val emailInput = "newEmail"
        composeTestRule.onNodeWithText("currentEmail").performTextReplacement(emailInput)
        verify {
            viewModel.trySendAction(MasterPasswordHintAction.EmailInputChange(emailInput))
        }
    }

    @Test
    fun `should show success dialog when PasswordHintSent state is set`() {
        mutableStateFlow.value = MasterPasswordHintState(
            dialog = MasterPasswordHintState.DialogState.PasswordHintSent,
            emailInput = "test@example.com",
        )

        composeTestRule
            .onNodeWithText("We’ve sent you an email with your master password hint.")
            .assertIsDisplayed()
    }

    @Test
    fun `should show error dialog when Error state is set`() {
        val errorMessage = "Error occurred"
        mutableStateFlow.value = MasterPasswordHintState(
            dialog = MasterPasswordHintState.DialogState.Error(message = errorMessage.asText()),
            emailInput = "test@example.com",
        )

        composeTestRule
            .onNodeWithText(errorMessage)
            .assertIsDisplayed()
    }

    @Test
    fun `should show loading dialog when Loading state is set`() {
        val loadingMessage = "Submitting"
        mutableStateFlow.value = MasterPasswordHintState(
            dialog = MasterPasswordHintState.DialogState.Loading(message = loadingMessage.asText()),
            emailInput = "test@example.com",
        )

        composeTestRule
            .onNodeWithText(loadingMessage)
            .assertIsDisplayed()
    }

    @Test
    fun `clicking ok in dialog should send DismissDialog action`() {
        composeTestRule.assertNoDialogExists()

        mutableStateFlow.value = MasterPasswordHintState(
            dialog = MasterPasswordHintState.DialogState.Error(message = "".asText()),
            emailInput = "test@example.com",
        )

        composeTestRule
            .onNodeWithText(text = "Okay")
            .performClick()

        verify { viewModel.trySendAction(MasterPasswordHintAction.DismissDialog) }
    }

    @Test
    fun `NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(MasterPasswordHintEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }
}

private val DEFAULT_STATE =
    MasterPasswordHintState(
        emailInput = "currentEmail",
    )
