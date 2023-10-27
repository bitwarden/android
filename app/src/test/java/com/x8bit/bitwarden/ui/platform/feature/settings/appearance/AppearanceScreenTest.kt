package com.x8bit.bitwarden.ui.platform.feature.settings.appearance

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

class AppearanceScreenTest : BaseComposeTest() {

    @Test
    fun `on back click should send BackClick`() {
        val viewModel: AppearanceViewModel = mockk {
            every { eventFlow } returns emptyFlow()
            every { trySendAction(AppearanceAction.BackClick) } returns Unit
        }
        composeTestRule.setContent {
            AppearanceScreen(
                viewModel = viewModel,
                onNavigateBack = { },
            )
        }
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        verify { viewModel.trySendAction(AppearanceAction.BackClick) }
    }

    @Test
    fun `on NavigateAbout should call onNavigateToAbout`() {
        var haveCalledNavigateBack = false
        val viewModel = mockk<AppearanceViewModel> {
            every { eventFlow } returns flowOf(AppearanceEvent.NavigateBack)
        }
        composeTestRule.setContent {
            AppearanceScreen(
                viewModel = viewModel,
                onNavigateBack = { haveCalledNavigateBack = true },
            )
        }
        assertTrue(haveCalledNavigateBack)
    }
}
