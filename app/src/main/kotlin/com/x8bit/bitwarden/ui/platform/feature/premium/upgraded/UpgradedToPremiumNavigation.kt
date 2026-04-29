@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.platform.feature.premium.upgraded

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the "Upgraded to Premium" screen.
 */
@OmitFromCoverage
@Serializable
data object UpgradedToPremiumRoute

/**
 * Add the "Upgraded to Premium" screen to the nav graph.
 */
fun NavGraphBuilder.upgradedToPremiumDestination(
    onDismiss: () -> Unit,
) {
    composableWithSlideTransitions<UpgradedToPremiumRoute> {
        UpgradedToPremiumScreen(onDismiss = onDismiss)
    }
}

/**
 * Navigate to the "Upgraded to Premium" screen.
 */
fun NavController.navigateToUpgradedToPremium() {
    this.navigate(route = UpgradedToPremiumRoute) {
        launchSingleTop = true
    }
}
