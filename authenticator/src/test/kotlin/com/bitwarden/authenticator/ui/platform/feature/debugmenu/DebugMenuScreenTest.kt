package com.bitwarden.authenticator.ui.platform.feature.debugmenu

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.bitwarden.authenticator.data.platform.manager.model.FlagKey
import com.bitwarden.authenticator.ui.platform.base.AuthenticatorComposeTest
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DebugMenuScreenTest : AuthenticatorComposeTest() {
    private var onNavigateBackCalled = false
    private val mutableEventFlow = bufferedMutableSharedFlow<DebugMenuEvent>()
    private val mutableStateFlow = MutableStateFlow(DebugMenuState(featureFlags = emptyMap()))
    private val viewModel = mockk<DebugMenuViewModel>(relaxed = true) {
        every { stateFlow } returns mutableStateFlow
        every { eventFlow } returns mutableEventFlow
    }

    @Before
    fun setup() {
        setContent {
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
        composeTestRule
            .onNodeWithContentDescription("Back")
            .performClick()

        verify { viewModel.trySendAction(DebugMenuAction.NavigateBack) }
    }

    @Test
    fun `feature flag content should not display if the state is empty`() {
        composeTestRule
            .onNodeWithText("Bitwarden authentication enabled", ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun `feature flag content should display if the state is not empty`() {
        mutableStateFlow.tryEmit(
            DebugMenuState(
                featureFlags = mapOf(
                    FlagKey.BitwardenAuthenticationEnabled to true,
                ),
            ),
        )

        composeTestRule
            .onNodeWithText("Bitwarden authentication enabled", ignoreCase = true)
            .assertExists()
    }

    @Test
    fun `boolean feature flag content should send action when clicked`() {
        mutableStateFlow.tryEmit(
            DebugMenuState(
                featureFlags = mapOf(
                    FlagKey.BitwardenAuthenticationEnabled to true,
                ),
            ),
        )
        composeTestRule
            .onNodeWithText("Bitwarden authentication enabled", ignoreCase = true)
            .performClick()

        verify {
            viewModel.trySendAction(
                DebugMenuAction.UpdateFeatureFlag(
                    FlagKey.BitwardenAuthenticationEnabled,
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
}
