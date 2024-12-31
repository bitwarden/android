package com.x8bit.bitwarden.ui.auth.feature.newdevicenotice

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.After
import org.junit.Before
import org.junit.Test

class NewDeviceNoticeTwoFactorScreenTest : BaseComposeTest() {
    private val intentManager = mockk<IntentManager>(relaxed = true) {
        every { startCustomTabsActivity(any()) } just runs
    }
    private var onNavigateBackCalled = false
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val mutableEventFlow = bufferedMutableSharedFlow<NewDeviceNoticeTwoFactorEvent>()
    private val viewModel = mockk<NewDeviceNoticeTwoFactorViewModel>(relaxed = true) {
        every { stateFlow } returns mutableStateFlow
        every { eventFlow } returns mutableEventFlow
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            NewDeviceNoticeTwoFactorScreen(
                onNavigateBackToVault = { onNavigateBackCalled = true },
                intentManager,
                viewModel = viewModel,
            )
        }
    }

    @After
    fun tearDown() {
        onNavigateBackCalled = false
    }

    @Test
    fun `Turn on two-step verification click should send TurnOnTwoFactorClick action`() {
        composeTestRule
            .onNodeWithText("Turn on", substring = true)
            .performScrollTo()
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
            .performScrollTo()
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
            .performScrollTo()
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
        mutableEventFlow.tryEmit(NewDeviceNoticeTwoFactorEvent.NavigateBackToVault)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `remind me later button visibility should update according to state`() {
        composeTestRule
            .onNodeWithText("Remind me later")
            .performScrollTo()
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(shouldShowRemindMeLater = false)
        }
        composeTestRule
            .onNodeWithText("Remind me later")
            .assertDoesNotExist()
    }

    @Test
    @Suppress("MaxLineLength")
    fun `turn on two factor dialog should be shown or hidden according to the state`() {
        composeTestRule.onNode(isDialog()).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState = NewDeviceNoticeTwoFactorDialogState.TurnOnTwoFactorDialog,
            )
        }

        composeTestRule.onNode(isDialog()).assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Continue to web app", substring = true, ignoreCase = true)
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                "Make your account more secure by setting up two-step login in the Bitwarden web app.",
                substring = true,
                ignoreCase = true,
                )
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Continue", substring = true, ignoreCase = true)
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Cancel", substring = true, ignoreCase = true)
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `change account email dialog should be shown or hidden according to the state`() {
        composeTestRule.onNode(isDialog()).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState = NewDeviceNoticeTwoFactorDialogState.ChangeAccountEmailDialog,
            )
        }

        composeTestRule.onNode(isDialog()).assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Continue to web app", substring = true, ignoreCase = true)
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                "You can change your account email on the Bitwarden web app.",
                substring = true,
                ignoreCase = true,
            )
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Continue", substring = true, ignoreCase = true)
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Cancel", substring = true, ignoreCase = true)
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `dialog should be hidden according to the state`() {
        composeTestRule.onNode(isDialog()).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState = NewDeviceNoticeTwoFactorDialogState.ChangeAccountEmailDialog,
            )
        }

        composeTestRule.onNode(isDialog()).assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                dialogState = null,
            )
        }

        composeTestRule.onNode(isDialog()).assertDoesNotExist()
    }
}

private val DEFAULT_STATE =
    NewDeviceNoticeTwoFactorState(
        shouldShowRemindMeLater = true,
        dialogState = null,
    )
