package com.x8bit.bitwarden.ui.platform.feature.settings.autofill

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoFillScreenTest : BaseComposeTest() {

    @Test
    fun `on back click should send BackClick`() {
        val viewModel: AutoFillViewModel = mockk {
            every { eventFlow } returns emptyFlow()
            every { trySendAction(AutoFillAction.BackClick) } returns Unit
        }
        composeTestRule.setContent {
            AutoFillScreen(
                viewModel = viewModel,
                onNavigateBack = { },
            )
        }
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        verify { viewModel.trySendAction(AutoFillAction.BackClick) }
    }

    @Test
    fun `on NavigateAbout should call onNavigateToAutoFill`() {
        var haveCalledNavigateBack = false
        val viewModel = mockk<AutoFillViewModel> {
            every { eventFlow } returns flowOf(AutoFillEvent.NavigateBack)
        }
        composeTestRule.setContent {
            AutoFillScreen(
                viewModel = viewModel,
                onNavigateBack = { haveCalledNavigateBack = true },
            )
        }
        assertTrue(haveCalledNavigateBack)
    }
}
