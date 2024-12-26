package com.x8bit.bitwarden.ui.auth.feature.newdevicenotice

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.After
import org.junit.Before
import org.junit.Test

class NewDeviceNoticeEmailAccessScreenTest : BaseComposeTest() {
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val mutableEventFlow = bufferedMutableSharedFlow<NewDeviceNoticeEmailAccessEvent>()
    private var onNavigateBackToVaultCalled = false
    private var onNavigateToTwoFactorOptionsCalled = false
    private val viewModel = mockk<NewDeviceNoticeEmailAccessViewModel>(relaxed = true) {
        every { stateFlow } returns mutableStateFlow
        every { eventFlow } returns mutableEventFlow
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            NewDeviceNoticeEmailAccessScreen(
                onNavigateBackToVault = { onNavigateBackToVaultCalled = true },
                onNavigateToTwoFactorOptions = { onNavigateToTwoFactorOptionsCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @After
    fun tearDown() {
        onNavigateBackToVaultCalled = false
        onNavigateToTwoFactorOptionsCalled = false
    }

    @Test
    @Suppress("MaxLineLength")
    fun `Do you have reliable access to your email should be toggled on or off according to the state`() {
        composeTestRule
            .onNodeWithText("Yes, I can reliably access my email", substring = true)
            .assertIsOff()

        mutableStateFlow.update { it.copy(isEmailAccessEnabled = true) }

        composeTestRule
            .onNodeWithText("Yes, I can reliably access my email", substring = true)
            .assertIsOn()
    }

    @Test
    fun `Do you have reliable access to your email click should send EmailAccessToggle action`() {
        composeTestRule
            .onNodeWithText("Yes, I can reliably access my email")
            .performClick()
        verify {
            viewModel.trySendAction(
                NewDeviceNoticeEmailAccessAction.EmailAccessToggle(true),
            )
        }
    }

    @Test
    fun `Continue button click should send ContinueButtonClick action`() {
        composeTestRule.onNodeWithText("Continue").performScrollTo().performClick()
        verify {
            viewModel.trySendAction(NewDeviceNoticeEmailAccessAction.ContinueClick)
        }
    }

    @Test
    fun `ContinueClick should call onNavigateBackToVault if isEmailAccessEnabled is false`() {
        mutableStateFlow.update { it.copy(isEmailAccessEnabled = false) }
        mutableEventFlow.tryEmit(NewDeviceNoticeEmailAccessEvent.NavigateBackToVault)
        assertTrue(onNavigateBackToVaultCalled)
    }

    @Test
    fun `ContinueClick should call onNavigateToTwoFactorOptions if isEmailAccessEnabled is true`() {
        mutableStateFlow.update { it.copy(isEmailAccessEnabled = true) }
        mutableEventFlow.tryEmit(NewDeviceNoticeEmailAccessEvent.NavigateToTwoFactorOptions)
        assertTrue(onNavigateToTwoFactorOptionsCalled)
    }
}

private const val EMAIL = "active@bitwarden.com"

private val DEFAULT_STATE =
    NewDeviceNoticeEmailAccessState(
        email = EMAIL,
        isEmailAccessEnabled = false,
    )
