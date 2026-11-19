@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.platform.feature.premium.plan

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.pushEntry
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.platform.feature.settings.SettingsGraph
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraph

/**
 * Add the standard plan screen to the navigation entries of the settings tab — bottom nav
 * visible.
 */
fun EntryProviderScope<SettingsGraph>.planDestination(
    onNavigateBack: () -> Unit,
    onNavigateToUpgradedToPremium: () -> Unit,
) {
    pushEntry<SettingsGraph.PlanRoute> {
        PlanScreen(
            viewModel = planViewModel(planMode = PlanMode.Standard),
            onNavigateBack = onNavigateBack,
            onNavigateToUpgradedToPremium = onNavigateToUpgradedToPremium,
        )
    }
}

/**
 * Add the modal plan screen to the vault unlocked navigation entries — bottom nav hidden.
 */
fun EntryProviderScope<VaultUnlockedGraph>.planModalDestination(
    onNavigateBack: () -> Unit,
    onNavigateToUpgradedToPremium: () -> Unit,
) {
    slideUpEntry<VaultUnlockedGraph.PlanModalRoute> {
        PlanScreen(
            viewModel = planViewModel(planMode = PlanMode.Modal),
            onNavigateBack = onNavigateBack,
            onNavigateToUpgradedToPremium = onNavigateToUpgradedToPremium,
        )
    }
}

@Composable
private fun planViewModel(
    planMode: PlanMode,
): PlanViewModel = hiltViewModel<PlanViewModel, PlanViewModel.Factory>(
    creationCallback = { factory -> factory.create(planMode = planMode) },
)

/**
 * Navigate to the plan screen (standard, within the settings tab).
 */
fun NavigationController<SettingsGraph>.navigateToPlan() {
    this.navigate(route = SettingsGraph.PlanRoute)
}

/**
 * Navigate to the plan screen (modal, at the vault unlocked root level).
 */
fun NavigationController<VaultUnlockedGraph>.navigateToPlanModal() {
    this.navigate(route = VaultUnlockedGraph.PlanModalRoute)
}
