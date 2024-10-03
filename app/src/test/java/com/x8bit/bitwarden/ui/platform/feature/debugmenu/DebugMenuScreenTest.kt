package com.x8bit.bitwarden.ui.platform.feature.debugmenu

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.printToLog
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DebugMenuScreenTest : BaseComposeTest() {
    private var onNavigateBackCalled = false
    private val mutableEventFlow = bufferedMutableSharedFlow<DebugMenuEvent>()
    private val mutableStateFlow = MutableStateFlow(DebugMenuState(featureFlags = emptyMap()))
    private val viewModel = mockk<DebugMenuViewModel>(relaxed = true) {
        every { stateFlow } returns mutableStateFlow
        every { eventFlow } returns mutableEventFlow
    }

    @Before
    fun setup() {
        composeTestRule.setContent {
            DebugMenuScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `onNavigateBack should set onNavigateBackCalled to true`() {
        mutableEventFlow.tryEmit(DebugMenuEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `onNavigateBack should send action to viewModel`() {
        composeTestRule.onRoot().printToLog("djf")
        composeTestRule
            .onNodeWithContentDescription("Back")
            .performClick()

        verify { viewModel.trySendAction(DebugMenuAction.NavigateBack) }
    }

    @Test
    fun `feature flag content should not display if the state is empty`() {
        composeTestRule
            .onNodeWithText("Email Verification", ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun `feature flag content should display if the state is not empty`() {
        mutableStateFlow.tryEmit(
            DebugMenuState(
                featureFlags = mapOf(
                    FlagKey.EmailVerification to true,
                ),
            ),
        )

        composeTestRule
            .onNodeWithText("Email Verification", ignoreCase = true)
            .assertExists()
    }

    @Test
    fun `boolean feature flag content should send action when clicked`() {
        mutableStateFlow.tryEmit(
            DebugMenuState(
                featureFlags = mapOf(
                    FlagKey.EmailVerification to true,
                ),
            ),
        )
        composeTestRule
            .onNodeWithText("Email Verification", ignoreCase = true)
            .performClick()

        verify {
            viewModel.trySendAction(
                DebugMenuAction.UpdateFeatureFlag(
                    FlagKey.EmailVerification,
                    false,
                ),
            )
        }
    }

    @Test
    fun `reset feature flag values should send action when clicked`() {
        composeTestRule
            .onNodeWithText("Reset Values", ignoreCase = true)
            .performScrollTo()
            .performClick()

        verify { viewModel.trySendAction(DebugMenuAction.ResetFeatureFlagValues) }
    }

    @Test
    fun `restart onboarding should send action when enabled and clicked`() {
        mutableStateFlow.tryEmit(
            DebugMenuState(
                featureFlags = mapOf(
                    FlagKey.OnboardingFlow to true,
                ),
            ),
        )
        composeTestRule
            .onNodeWithText("Restart Onboarding", ignoreCase = true)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        verify { viewModel.trySendAction(DebugMenuAction.RestartOnboarding) }
    }

    @Test
    fun `restart onboarding should not send action when not enabled`() {
        mutableStateFlow.tryEmit(
            DebugMenuState(
                featureFlags = mapOf(
                    FlagKey.OnboardingFlow to false,
                ),
            ),
        )

        composeTestRule
            .onNodeWithText("Restart Onboarding", ignoreCase = true)
            .performScrollTo()
            .assertIsNotEnabled()
            .performClick()

        verify(exactly = 0) { viewModel.trySendAction(DebugMenuAction.RestartOnboarding) }
    }

    @Test
    fun `Show onboarding carousel should send action when enabled and clicked`() {
        mutableStateFlow.tryEmit(
            DebugMenuState(
                featureFlags = mapOf(
                    FlagKey.OnboardingCarousel to true,
                ),
            ),
        )
        composeTestRule
            .onNodeWithText("Show Onboarding Carousel", ignoreCase = true)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        verify { viewModel.trySendAction(DebugMenuAction.RestartOnboardingCarousel) }
    }

    @Test
    fun `show onboarding carousel should not send action when not enabled`() {
        mutableStateFlow.tryEmit(
            DebugMenuState(
                featureFlags = mapOf(
                    FlagKey.OnboardingCarousel to false,
                ),
            ),
        )

        composeTestRule
            .onNodeWithText("Show Onboarding Carousel", ignoreCase = true)
            .performScrollTo()
            .assertIsNotEnabled()
            .performClick()

        verify(exactly = 0) { viewModel.trySendAction(DebugMenuAction.RestartOnboardingCarousel) }
    }
}
