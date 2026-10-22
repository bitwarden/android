@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.platform.feature.premium.upgraded

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.platform.feature.premium.plan.PlanMode
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraph

/**
 * Add the "Upgraded to Premium" screen to the navigation entries. The dismiss callback receives
 * the originating [PlanMode] so the registrant can choose appropriate pop semantics (e.g., pop
 * only the Upgraded to Premium screen vs. pop it and the modal Plan screen together).
 */
fun EntryProviderScope<VaultUnlockedGraph>.upgradedToPremiumDestination(
    onDismiss: (PlanMode) -> Unit,
) {
    slideUpEntry<VaultUnlockedGraph.UpgradedToPremiumRoute> { route ->
        UpgradedToPremiumScreen(onDismiss = { onDismiss(route.planMode) })
    }
}

/**
 * Navigate to the "Upgraded to Premium" screen for the given [planMode].
 */
fun NavigationController<VaultUnlockedGraph>.navigateToUpgradedToPremium(planMode: PlanMode) {
    this.addSingleTop(route = VaultUnlockedGraph.UpgradedToPremiumRoute(planMode = planMode))
}
