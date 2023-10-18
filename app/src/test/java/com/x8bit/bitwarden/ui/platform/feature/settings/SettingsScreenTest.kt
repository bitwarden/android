package com.x8bit.bitwarden.ui.platform.feature.settings

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Test

class SettingsScreenTest : BaseComposeTest() {

    @Test
    fun `on account row click should emit AccountSecurityClick`() {
        val viewModel = mockk<SettingsViewModel> {
            every { eventFlow } returns emptyFlow()
            every { trySendAction(SettingsAction.AccountSecurityClick) } returns Unit
        }
        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToAccountSecurity = { },
            )
        }
        composeTestRule.onNodeWithText("Account").performClick()
        verify { viewModel.trySendAction(SettingsAction.AccountSecurityClick) }
    }

    @Test
    fun `on NavigateAccountSecurity should call onNavigateToAccountSecurity`() {
        var haveCalledNavigateToAccountSecurity = false
        val viewModel = mockk<SettingsViewModel> {
            every { eventFlow } returns flowOf(SettingsEvent.NavigateAccountSecurity)
        }
        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToAccountSecurity = {
                    haveCalledNavigateToAccountSecurity = true
                },
            )
        }
        assert(haveCalledNavigateToAccountSecurity)
    }
}
