package com.x8bit.bitwarden.ui.platform.feature.debugmenu.handler

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.bitwarden.core.data.manager.model.FlagKey
import com.x8bit.bitwarden.ui.platform.feature.debugmenu.DebugMenuAction
import com.x8bit.bitwarden.ui.platform.feature.debugmenu.DebugMenuState
import com.x8bit.bitwarden.ui.platform.feature.debugmenu.DebugMenuViewModel

/**
 * Handler for the debug menu screen lambda invocations.
 */
@Suppress("LongParameterList")
class DebugMenuHandler(
    val onNavigateBack: () -> Unit,
    val onMainTypeOptionClick: (option: DebugMenuState.MainTypeOption) -> Unit,
    val onUpdateFeatureFlag: (flagKey: FlagKey<Any>, newValue: Any) -> Unit,
    val onResetFeatureFlagValues: () -> Unit,
    val onRestartOnboarding: () -> Unit,
    val onRestartOnboardingCarousel: () -> Unit,
    val onResetAccessibilityDisclaimer: () -> Unit,
    val onResetCoachMarkTourStatuses: () -> Unit,
    val onTriggerCookieAcquisition: () -> Unit,
    val onClearSsoCookies: () -> Unit,
    val onResetPremiumUpgradeBanner: () -> Unit,
    val onShowUpgradedToPremiumCard: () -> Unit,
    val onShareSettingsClick: () -> Unit,
    val onGenerateErrorReportClick: () -> Unit,
    val onGenerateCrashClick: () -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Create [DebugMenuHandler] with the given [viewModel] to send actions to.
         */
        fun create(viewModel: DebugMenuViewModel): DebugMenuHandler = DebugMenuHandler(
            onNavigateBack = { viewModel.trySendAction(DebugMenuAction.NavigateBack) },
            onMainTypeOptionClick = {
                viewModel.trySendAction(DebugMenuAction.MainTypeOptionClick(it))
            },
            onUpdateFeatureFlag = { key, value ->
                viewModel.trySendAction(DebugMenuAction.UpdateFeatureFlag(key, value))
            },
            onResetFeatureFlagValues = {
                viewModel.trySendAction(DebugMenuAction.ResetFeatureFlagValues)
            },
            onRestartOnboarding = { viewModel.trySendAction(DebugMenuAction.RestartOnboarding) },
            onRestartOnboardingCarousel = {
                viewModel.trySendAction(DebugMenuAction.RestartOnboardingCarousel)
            },
            onResetAccessibilityDisclaimer = {
                viewModel.trySendAction(DebugMenuAction.ResetAccessibilityDisclaimer)
            },
            onResetCoachMarkTourStatuses = {
                viewModel.trySendAction(DebugMenuAction.ResetCoachMarkTourStatuses)
            },
            onTriggerCookieAcquisition = {
                viewModel.trySendAction(DebugMenuAction.TriggerCookieAcquisition)
            },
            onClearSsoCookies = { viewModel.trySendAction(DebugMenuAction.ClearSsoCookies) },
            onResetPremiumUpgradeBanner = {
                viewModel.trySendAction(DebugMenuAction.ResetPremiumUpgradeBanner)
            },
            onShowUpgradedToPremiumCard = {
                viewModel.trySendAction(DebugMenuAction.ShowUpgradedToPremiumCard)
            },
            onShareSettingsClick = { viewModel.trySendAction(DebugMenuAction.ShareSettingsClick) },
            onGenerateErrorReportClick = {
                viewModel.trySendAction(DebugMenuAction.GenerateErrorReportClick)
            },
            onGenerateCrashClick = { viewModel.trySendAction(DebugMenuAction.GenerateCrashClick) },
        )

        /**
         * Create [DebugMenuHandler] with all empty callbacks. This should only be used for
         * previews.
         */
        fun createEmpty(): DebugMenuHandler = DebugMenuHandler(
            onNavigateBack = { },
            onMainTypeOptionClick = { },
            onUpdateFeatureFlag = { _, _ -> },
            onResetFeatureFlagValues = { },
            onRestartOnboarding = { },
            onRestartOnboardingCarousel = { },
            onResetAccessibilityDisclaimer = { },
            onResetCoachMarkTourStatuses = { },
            onTriggerCookieAcquisition = { },
            onClearSsoCookies = { },
            onResetPremiumUpgradeBanner = { },
            onShowUpgradedToPremiumCard = { },
            onShareSettingsClick = { },
            onGenerateErrorReportClick = { },
            onGenerateCrashClick = { },
        )
    }
}

/**
 * Remember [DebugMenuHandler] with the given [viewModel] within a [Composable] scope.
 */
@Composable
fun rememberDebugMenuHandler(
    viewModel: DebugMenuViewModel,
): DebugMenuHandler =
    remember(viewModel) {
        DebugMenuHandler.create(viewModel = viewModel)
    }
