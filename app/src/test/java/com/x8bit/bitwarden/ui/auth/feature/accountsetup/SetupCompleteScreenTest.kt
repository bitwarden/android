package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class SetupCompleteScreenTest : BaseComposeTest() {

    private val viewModel = mockk<SetupCompleteViewModel>(relaxed = true)

    @Before
    fun setup() {
        setContentWithBackDispatcher {
            SetupCompleteScreen(viewModel = viewModel)
        }
    }

    @Test
    fun `When continue button clicked sends CompleteSetup action`() {
        composeTestRule
            .onNodeWithText("Continue")
            .performScrollTo()
            .performClick()

        verify { viewModel.trySendAction(SetupCompleteAction.CompleteSetup) }
    }

    @Test
    fun `When system back behavior is triggered sends CompleteSetup action`() {
        backDispatcher?.onBackPressed()

        verify { viewModel.trySendAction(SetupCompleteAction.CompleteSetup) }
    }
}
