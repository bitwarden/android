package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.AccountSecurityAction.ConfirmLogoutClick
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.AccountSecurityAction.DismissDialog
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountSecurityScreenTest : BaseComposeTest() {

    @Test
    fun `on Log out click should send LogoutClick`() {
        val viewModel: AccountSecurityViewModel = mockk {
            every { stateFlow } returns MutableStateFlow(DEFAULT_STATE)
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
            every { stateFlow } returns MutableStateFlow(DEFAULT_STATE)
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
            every { stateFlow } returns MutableStateFlow(DEFAULT_STATE)
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

    @Test
    fun `confirm dialog be shown or hidden according to the state`() {
        val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
        val viewModel = mockk<AccountSecurityViewModel> {
            every { stateFlow } returns mutableStateFlow
            every { eventFlow } returns emptyFlow()
            every { trySendAction(ConfirmLogoutClick) } returns Unit
        }
        composeTestRule.setContent {
            AccountSecurityScreen(
                viewModel = viewModel,
                onNavigateBack = { },
            )
        }
        composeTestRule.onNode(isDialog()).assertDoesNotExist()

        mutableStateFlow.update { it.copy(shouldShowConfirmLogoutDialog = true) }

        composeTestRule
            .onNodeWithText("Yes")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Cancel")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Are you sure you want to log out?")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `on confirm logout click should send ConfirmLogoutClick`() {
        val viewModel = mockk<AccountSecurityViewModel> {
            every { stateFlow } returns MutableStateFlow(
                DEFAULT_STATE.copy(shouldShowConfirmLogoutDialog = true),
            )
            every { eventFlow } returns emptyFlow()
            every { trySendAction(ConfirmLogoutClick) } returns Unit
        }
        composeTestRule.setContent {
            AccountSecurityScreen(
                viewModel = viewModel,
                onNavigateBack = { },
            )
        }
        composeTestRule
            .onNodeWithText("Yes")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(ConfirmLogoutClick) }
    }

    @Test
    fun `on cancel click should send DismissDialog`() {
        val viewModel = mockk<AccountSecurityViewModel> {
            every { stateFlow } returns MutableStateFlow(
                DEFAULT_STATE.copy(shouldShowConfirmLogoutDialog = true),
            )
            every { eventFlow } returns emptyFlow()
            every { trySendAction(DismissDialog) } returns Unit
        }
        composeTestRule.setContent {
            AccountSecurityScreen(
                viewModel = viewModel,
                onNavigateBack = { },
            )
        }
        composeTestRule
            .onNodeWithText("Cancel")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(DismissDialog) }
    }

    companion object {
        private val DEFAULT_STATE = AccountSecurityState(
            shouldShowConfirmLogoutDialog = false,
        )
    }
}
