package com.x8bit.bitwarden.ui.platform.feature.debugmenu

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.manager.IntentManager
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DebugMenuScreenTest : BitwardenComposeTest() {
    private var onNavigateBackCalled = false
    private val mutableEventFlow = bufferedMutableSharedFlow<DebugMenuEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<DebugMenuViewModel>(relaxed = true) {
        every { stateFlow } returns mutableStateFlow
        every { eventFlow } returns mutableEventFlow
    }
    private val intentManager = mockk<IntentManager> {
        every { shareText(text = any()) } just runs
    }

    @Before
    fun setup() {
        setContent(
            intentManager = intentManager,
        ) {
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
    fun `on ShareText event should share the text via the IntentManager`() {
        mutableEventFlow.tryEmit(DebugMenuEvent.ShareText(text = "settingsLogData"))

        verify(exactly = 1) { intentManager.shareText(text = "settingsLogData") }
    }

    @Test
    fun `on share settings click should send ShareSettingsClick action`() {
        mutableStateFlow.update { it.copy(mainTypeOption = DebugMenuState.MainTypeOption.OPTIONS) }
        composeTestRule
            .onNodeWithText(text = "Share settings")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        verify(exactly = 1) { viewModel.trySendAction(DebugMenuAction.ShareSettingsClick) }
    }

    @Test
    fun `on generate crash click should send GenerateCrashClick action`() {
        mutableStateFlow.update { it.copy(mainTypeOption = DebugMenuState.MainTypeOption.OPTIONS) }
        composeTestRule
            .onNodeWithText(text = "Generate crash")
            .performScrollTo()
            .performClick()

        verify { viewModel.trySendAction(DebugMenuAction.GenerateCrashClick) }
    }

    @Test
    fun `on generate error report click should send GenerateErrorReportClick action`() {
        mutableStateFlow.update { it.copy(mainTypeOption = DebugMenuState.MainTypeOption.OPTIONS) }
        composeTestRule
            .onNodeWithText(text = "Generate error report")
            .performScrollTo()
            .performClick()

        verify { viewModel.trySendAction(DebugMenuAction.GenerateErrorReportClick) }
    }

    @Test
    fun `feature flag content should not display if the state is empty`() {
        mutableStateFlow.update { it.copy(featureFlags = persistentMapOf()) }
        composeTestRule
            .onNodeWithText(text = "dummy-boolean")
            .assertDoesNotExist()
    }

    @Test
    fun `feature flag content should display if the state is not empty`() {
        mutableStateFlow.update {
            it.copy(
                featureFlags = persistentMapOf(
                    FlagKey.DummyBoolean to true,
                ),
            )
        }
        composeTestRule
            .onNodeWithText(text = "dummy-boolean", ignoreCase = true)
            .assertExists()
    }

    @Test
    fun `boolean feature flag content should send action when clicked`() {
        mutableStateFlow.update {
            it.copy(
                featureFlags = persistentMapOf(
                    FlagKey.DummyBoolean to true,
                ),
            )
        }
        composeTestRule
            .onNodeWithText(text = "dummy-boolean")
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(
                DebugMenuAction.UpdateFeatureFlag(
                    FlagKey.DummyBoolean,
                    false,
                ),
            )
        }
    }

    @Test
    fun `reset feature flag values should send action when clicked`() {
        composeTestRule
            .onNodeWithText("Reset values")
            .performScrollTo()
            .performClick()

        verify(exactly = 1) { viewModel.trySendAction(DebugMenuAction.ResetFeatureFlagValues) }
    }

    @Test
    fun `restart onboarding should send action when clicked`() {
        mutableStateFlow.update { it.copy(mainTypeOption = DebugMenuState.MainTypeOption.OPTIONS) }
        composeTestRule
            .onNodeWithText("Restart Onboarding", ignoreCase = true)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        verify(exactly = 1) { viewModel.trySendAction(DebugMenuAction.RestartOnboarding) }
    }

    @Test
    fun `Show onboarding carousel should send action when enabled and clicked`() {
        mutableStateFlow.update { it.copy(mainTypeOption = DebugMenuState.MainTypeOption.OPTIONS) }
        composeTestRule
            .onNodeWithText("Show Onboarding Carousel")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        verify(exactly = 1) { viewModel.trySendAction(DebugMenuAction.RestartOnboardingCarousel) }
    }

    @Test
    fun `clear SSO cookies should send ClearSsoCookies action`() {
        mutableStateFlow.update { it.copy(mainTypeOption = DebugMenuState.MainTypeOption.OPTIONS) }
        composeTestRule
            .onNodeWithText("Clear SSO cookies")
            .performScrollTo()
            .performClick()

        verify(exactly = 1) { viewModel.trySendAction(DebugMenuAction.ClearSsoCookies) }
    }

    @Test
    fun `reset Premium upgrade banner should send ResetPremiumUpgradeBanner action`() {
        mutableStateFlow.update { it.copy(mainTypeOption = DebugMenuState.MainTypeOption.OPTIONS) }
        composeTestRule
            .onNodeWithText("Reset Premium upgrade banner")
            .performScrollTo()
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(DebugMenuAction.ResetPremiumUpgradeBanner)
        }
    }

    @Test
    fun `reset accessibility disclaimer should send ResetAccessibilityDisclaimer action`() {
        mutableStateFlow.update { it.copy(mainTypeOption = DebugMenuState.MainTypeOption.OPTIONS) }
        composeTestRule
            .onNodeWithText("Reset accessibility disclaimer")
            .performScrollTo()
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(DebugMenuAction.ResetAccessibilityDisclaimer)
        }
    }

    @Test
    fun `reset all coach mark tours should send ResetCoachMarkTourStatuses action`() {
        mutableStateFlow.update { it.copy(mainTypeOption = DebugMenuState.MainTypeOption.OPTIONS) }
        composeTestRule
            .onNodeWithText("Reset all coach mark tours")
            .performScrollTo()
            .performClick()

        verify(exactly = 1) { viewModel.trySendAction(DebugMenuAction.ResetCoachMarkTourStatuses) }
    }
}

private val DEFAULT_STATE: DebugMenuState = DebugMenuState(
    featureFlags = persistentMapOf(
        FlagKey.DummyBoolean to true,
    ),
    mainTypeOption = DebugMenuState.MainTypeOption.FLAGS,
)
