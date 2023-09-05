package com.x8bit.bitwarden.ui.auth.feature.landing

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.ui.auth.feature.landing.LandingAction
import com.x8bit.bitwarden.ui.auth.feature.landing.LandingScreen
import com.x8bit.bitwarden.ui.auth.feature.landing.LandingState
import com.x8bit.bitwarden.ui.auth.feature.landing.LandingViewModel
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Test

class LandingScreenTest : BaseComposeTest() {

    @Test
    fun `continue button click should send ContinueButtonClicked action`() {
        val viewModel = mockk<LandingViewModel>(relaxed = true) {
            every { eventFlow } returns emptyFlow()
            every { stateFlow } returns MutableStateFlow(
                LandingState(
                    initialEmailAddress = "",
                    isContinueButtonEnabled = true,
                    isRememberMeEnabled = false,
                ),
            )
            every { trySendAction(LandingAction.ContinueButtonClick) } returns Unit
        }
        composeTestRule.setContent {
            LandingScreen(
                onNavigateToCreateAccount = {},
                viewModel = viewModel,
            )
        }
        composeTestRule.onNodeWithTag("Continue button").performClick()
        verify {
            viewModel.trySendAction(LandingAction.ContinueButtonClick)
        }
    }
}
