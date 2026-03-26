package com.x8bit.bitwarden.ui.vault.feature.cardscanner

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the card scan screen.
 */
@Serializable
data object CardScanRoute

/**
 * Add the card scan screen to the nav graph.
 */
fun NavGraphBuilder.cardScanDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions<CardScanRoute> {
        CardScanScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the card scan screen.
 */
fun NavController.navigateToCardScanScreen(
    navOptions: NavOptions? = null,
) {
    this.navigate(route = CardScanRoute, navOptions = navOptions)
}
