package com.x8bit.bitwarden.ui.auth.feature.createaccount

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Test

class CreateAccountScreenTest : BaseComposeTest() {

    @Test
    fun `submit click should send SubmitClick action`() {
        val viewModel = mockk<CreateAccountViewModel>(relaxed = true) {
            every { eventFlow } returns emptyFlow()
            every { trySendAction(CreateAccountAction.SubmitClick) } returns Unit
        }
        composeTestRule.setContent {
            CreateAccountScreen(viewModel)
        }
        composeTestRule.onNodeWithText("SUBMIT").performClick()
        verify { viewModel.trySendAction(CreateAccountAction.SubmitClick) }
    }
}
