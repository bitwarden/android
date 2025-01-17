package com.x8bit.bitwarden.ui.auth.feature.masterpasswordguidance

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MasterPasswordGuidanceScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled = false
    private var onNavigateToGeneratorCalled = false
    private val mutableEventFlow = bufferedMutableSharedFlow<MasterPasswordGuidanceEvent>()
    private val viewModel = mockk<MasterPasswordGuidanceViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
    }

    @Before
    fun setup() {
        composeTestRule.setContent {
            MasterPasswordGuidanceScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToGeneratePassword = { onNavigateToGeneratorCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `Close button click should invoke send of CloseAction`() {
        composeTestRule
            .onNodeWithContentDescription("Close")
            .performClick()

        verify { viewModel.trySendAction(MasterPasswordGuidanceAction.CloseAction) }
    }

    @Test
    fun `Generator card click should invoke send of TryPasswordGeneratorAction`() {
        composeTestRule
            .onNodeWithText("Check out the passphrase generator")
            .performScrollTo()
            .performClick()

        verify { viewModel.trySendAction(MasterPasswordGuidanceAction.TryPasswordGeneratorAction) }
    }

    @Test
    fun `NavigateBack event should invoke onNavigateBack lambda`() {
        assertFalse(onNavigateBackCalled)
        mutableEventFlow.tryEmit(MasterPasswordGuidanceEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `NavigateToPasswordGenerator event should invoke onNavigateToGeneratePassword lambda`() {
        assertFalse(onNavigateToGeneratorCalled)
        mutableEventFlow.tryEmit(MasterPasswordGuidanceEvent.NavigateToPasswordGenerator)
        assertTrue(onNavigateToGeneratorCalled)
    }
}
