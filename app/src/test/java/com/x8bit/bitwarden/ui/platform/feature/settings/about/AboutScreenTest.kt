package com.x8bit.bitwarden.ui.platform.feature.settings.about

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

class AboutScreenTest : BaseComposeTest() {

    @Test
    fun `on back click should send BackClick`() {
        val viewModel: AboutViewModel = mockk {
            every { eventFlow } returns emptyFlow()
            every { trySendAction(AboutAction.BackClick) } returns Unit
        }
        composeTestRule.setContent {
            AboutScreen(
                viewModel = viewModel,
                onNavigateBack = { },
            )
        }
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        verify { viewModel.trySendAction(AboutAction.BackClick) }
    }

    @Test
    fun `on NavigateAbout should call onNavigateToAbout`() {
        var haveCalledNavigateBack = false
        val viewModel = mockk<AboutViewModel> {
            every { eventFlow } returns flowOf(AboutEvent.NavigateBack)
        }
        composeTestRule.setContent {
            AboutScreen(
                viewModel = viewModel,
                onNavigateBack = { haveCalledNavigateBack = true },
            )
        }
        assertTrue(haveCalledNavigateBack)
    }
}
