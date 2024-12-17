package com.x8bit.bitwarden.ui.auth.feature.newdevicenotice

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.net.toUri
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test

class NewDeviceNoticeTwoFactorScreenTest : BaseComposeTest() {
    private val intentManager = mockk<IntentManager>(relaxed = true) {
        every { startCustomTabsActivity(any()) } just runs
    }
    private var onNavigateBackCalled = false
    private val mutableEventFlow = bufferedMutableSharedFlow<NewDeviceNoticeTwoFactorEvent>()
    private val viewModel = mockk<NewDeviceNoticeTwoFactorViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { trySendAction(action = any()) } just runs
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            NewDeviceNoticeTwoFactorScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                intentManager,
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `Turn on two-step verification click should send TurnOnTwoFactorClick action`() {
        composeTestRule
            .onNodeWithText("Turn on", substring = true)
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(
                NewDeviceNoticeTwoFactorAction.TurnOnTwoFactorClick,
            )
        }
    }

    @Test
    fun `Change account email click should send ChangeAccountEmailClick action`() {
        composeTestRule
            .onNodeWithText("Change account email")
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(
                NewDeviceNoticeTwoFactorAction.ChangeAccountEmailClick,
            )
        }
    }

    @Test
    fun `Remind me later click should send RemindMeLaterClick action`() {
        composeTestRule
            .onNodeWithText("Remind me later")
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(
                NewDeviceNoticeTwoFactorAction.RemindMeLaterClick,
            )
        }
    }

    @Test
    fun `on NavigateToTurnOnTwoFactor should call launchUri on IntentManager`() {
        mutableEventFlow.tryEmit(
            NewDeviceNoticeTwoFactorEvent.NavigateToTurnOnTwoFactor(
                url = "https://bitwarden.com/#/settings/security/two-factor",
            ),
        )
        verify(exactly = 1) {
            intentManager.launchUri("https://bitwarden.com/#/settings/security/two-factor".toUri())
        }
    }

    @Test
    fun `ChangeAccountEmailClick should call OnNavigateBack`() {
        mutableEventFlow.tryEmit(
            NewDeviceNoticeTwoFactorEvent.NavigateToChangeAccountEmail(
                url = "https://vault.bitwarden.com/#/settings/account",
            ),
        )
        verify(exactly = 1) {
            intentManager.launchUri("https://vault.bitwarden.com/#/settings/account".toUri())
        }
    }

    @Test
    fun `RemindMeLaterClick should call OnNavigateBack`() {
        mutableEventFlow.tryEmit(NewDeviceNoticeTwoFactorEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }
}
