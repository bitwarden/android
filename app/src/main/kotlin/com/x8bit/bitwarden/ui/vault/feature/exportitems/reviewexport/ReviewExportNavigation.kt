@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.vault.feature.exportitems.reviewexport

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.base.util.composableWithPushTransitions
import com.x8bit.bitwarden.ui.vault.feature.exportitems.selectaccount.popUpToSelectAccountScreen
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the Review Export screen.
 *
 * This route is used to navigate to the screen where the user can review the items
 * that will be exported from their vault. This screen operates on the currently
 * active user context for determining which items to review for export.
 */
@OmitFromCoverage
@Serializable
data object ReviewExportRoute

/**
 * Defines the destination for the Review Export screen within the navigation graph.
 *
 * This extension function on [NavGraphBuilder] sets up the [ReviewExportScreen]
 * composable for the [ReviewExportRoute], using push transitions.
 *
 * @param navController The [NavController] used for handling back navigation from the screen.
 */
fun NavGraphBuilder.reviewExportDestination(
    navController: NavController,
) {
    composableWithPushTransitions<ReviewExportRoute> {
        ReviewExportScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToAccountSelection = { navController.popUpToSelectAccountScreen() },
        )
    }
}

/**
 * Navigates to the Review Export screen.
 *
 * This extension function on [NavController] provides a type-safe way to navigate
 * to the [ReviewExportRoute].
 *
 * @param navOptions Optional [NavOptions] for this navigation action.
 */
fun NavController.navigateToReviewExport(
    navOptions: NavOptions? = null,
) {
    navigate(route = ReviewExportRoute, navOptions = navOptions)
}
