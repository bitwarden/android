package com.x8bit.bitwarden.ui.auth.feature.masterpasswordgenerator

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MasterPasswordGeneratorScreenTest : BaseComposeTest() {
    private var onNavigateBackCalled = false
    private var onNavigateToPreventLockoutCalled = false
    private var navigateBackWithPasswordCalled = false
    private val mutableEventFlow = bufferedMutableSharedFlow<MasterPasswordGeneratorEvent>()
    private val mutableStateFlow = MutableStateFlow(
        value = MasterPasswordGeneratorState(generatedPassword = "-"),
    )
    private val viewModel = mockk<MasterPasswordGeneratorViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow.asStateFlow()
    }

    @Before
    fun setup() {
        composeTestRule.setContent {
            MasterPasswordGeneratorScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToPreventLockout = { onNavigateToPreventLockoutCalled = true },
                onNavigateBackWithPassword = { navigateBackWithPasswordCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `Generated password field state should update with ViewModel state`() {
        val updatedValue = PASSWORD_INPUT
        mutableStateFlow.update { it.copy(generatedPassword = updatedValue) }

        composeTestRule
            .onNodeWithText(updatedValue)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `NavigateBack event should invoke onNavigateBack lambda`() {
        mutableEventFlow.tryEmit(MasterPasswordGeneratorEvent.NavigateBack)

        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `NavigateToPreventLockout event should invoke onNavigateToPreventLockout lambda`() {
        mutableEventFlow.tryEmit(MasterPasswordGeneratorEvent.NavigateToPreventLockout)

        assertTrue(onNavigateToPreventLockoutCalled)
    }

    @Test
    fun `Verify clicking the back navigation button sends correct action`() {
        composeTestRule
            .onNodeWithContentDescription("Back")
            .performClick()

        verify { viewModel.trySendAction(MasterPasswordGeneratorAction.BackClickAction) }
    }

    @Test
    fun `Verify clicking the save text button sends correct action`() {
        composeTestRule
            .onNodeWithText("Save")
            .performClick()

        verify { viewModel.trySendAction(MasterPasswordGeneratorAction.SavePasswordClickAction) }
    }

    @Test
    fun `Verify clicking the generate button sends correct action`() {
        composeTestRule
            .onNodeWithText("Generate")
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(MasterPasswordGeneratorAction.GeneratePasswordClickAction)
        }
    }

    @Test
    fun `Verify clicking the learn to prevent lockout text sends correct action`() {
        composeTestRule
            .onNodeWithText("Learn about other ways to prevent account lockout")
            .performScrollTo()
            .performClick()

        verify { viewModel.trySendAction(MasterPasswordGeneratorAction.PreventLockoutClickAction) }
    }

    @Test
    fun `Verify navigating back with password invokes the lambda`() {
        mutableEventFlow.tryEmit(
            MasterPasswordGeneratorEvent.NavigateBackToRegistration,
        )

        assertTrue(navigateBackWithPasswordCalled)
    }
}

private const val PASSWORD_INPUT = "password1234"
