package com.x8bit.bitwarden.example.ui.feature.createaccount

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.example.ui.BaseComposeTest
import com.x8bit.bitwarden.ui.feature.createaccount.CreateAccountAction
import com.x8bit.bitwarden.ui.feature.createaccount.CreateAccountScreen
import com.x8bit.bitwarden.ui.feature.createaccount.CreateAccountViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Test

class CreateAccountScreenTest : BaseComposeTest() {

    @Test
    fun `on submit click should send SubmitClick action`() {
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
