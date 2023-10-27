package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountSecurityScreenTest : BaseComposeTest() {

    @Test
    fun `on Log out click should send LogoutClick`() {
        val viewModel: AccountSecurityViewModel = mockk {
            every { eventFlow } returns emptyFlow()
            every { trySendAction(AccountSecurityAction.LogoutClick) } returns Unit
        }
        composeTestRule.setContent {
            AccountSecurityScreen(
                viewModel = viewModel,
                onNavigateBack = { },
            )
        }
        composeTestRule.onNodeWithText("Log out").performClick()
        verify { viewModel.trySendAction(AccountSecurityAction.LogoutClick) }
    }

    @Test
    fun `on back click should send BackClick`() {
        val viewModel: AccountSecurityViewModel = mockk {
            every { eventFlow } returns emptyFlow()
            every { trySendAction(AccountSecurityAction.BackClick) } returns Unit
        }
        composeTestRule.setContent {
            AccountSecurityScreen(
                viewModel = viewModel,
                onNavigateBack = { },
            )
        }
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        verify { viewModel.trySendAction(AccountSecurityAction.BackClick) }
    }

    @Test
    fun `on NavigateAccountSecurity should call onNavigateToAccountSecurity`() {
        var haveCalledNavigateBack = false
        val viewModel = mockk<AccountSecurityViewModel> {
            every { eventFlow } returns flowOf(AccountSecurityEvent.NavigateBack)
        }
        composeTestRule.setContent {
            AccountSecurityScreen(
                viewModel = viewModel,
                onNavigateBack = { haveCalledNavigateBack = true },
            )
        }
        assertTrue(haveCalledNavigateBack)
    }
}
