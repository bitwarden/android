package com.x8bit.bitwarden.ui.platform.feature.settings.other

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

class OtherScreenTest : BaseComposeTest() {

    @Test
    fun `on back click should send BackClick`() {
        val viewModel: OtherViewModel = mockk {
            every { eventFlow } returns emptyFlow()
            every { trySendAction(OtherAction.BackClick) } returns Unit
        }
        composeTestRule.setContent {
            OtherScreen(
                viewModel = viewModel,
                onNavigateBack = { },
            )
        }
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        verify { viewModel.trySendAction(OtherAction.BackClick) }
    }

    @Test
    fun `on NavigateOther should call onNavigateToOther`() {
        var haveCalledNavigateBack = false
        val viewModel = mockk<OtherViewModel> {
            every { eventFlow } returns flowOf(OtherEvent.NavigateBack)
        }
        composeTestRule.setContent {
            OtherScreen(
                viewModel = viewModel,
                onNavigateBack = { haveCalledNavigateBack = true },
            )
        }
        assertTrue(haveCalledNavigateBack)
    }
}
