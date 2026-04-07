@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.vault.feature.exportitems

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.bitwarden.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.vault.feature.exportitems.reviewexport.navigateToReviewExport
import com.x8bit.bitwarden.ui.vault.feature.exportitems.reviewexport.reviewExportDestination
import com.x8bit.bitwarden.ui.vault.feature.exportitems.selectaccount.SelectAccountRoute
import com.x8bit.bitwarden.ui.vault.feature.exportitems.selectaccount.selectAccountDestination
import com.x8bit.bitwarden.ui.vault.feature.exportitems.verifypassword.navigateToVerifyPassword
import com.x8bit.bitwarden.ui.vault.feature.exportitems.verifypassword.verifyPasswordDestination
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the root of the export items feature graph.
 *
 * This route serves as the entry point for the entire export items flow.
 */
@OmitFromCoverage
@Serializable
data object ExportItemsGraphRoute

/**
 * Adds the export items feature graph to the NavGraphBuilder.
 *
 * This graph encompasses the entire flow for exporting items from the vault,
 * including account selection, password verification, and reviewing the export details.
 *
 * @param navController The [NavController] used for navigating within this graph and back to
 * previous screens.
 */
fun NavGraphBuilder.exportItemsGraph(
    navController: NavController,
) {
    navigation<ExportItemsGraphRoute>(
        startDestination = SelectAccountRoute,
    ) {
        selectAccountDestination(
            onAccountSelected = { userId, hasOtherAccounts ->
                navController.navigateToVerifyPassword(
                    userId = userId,
                    hasOtherAccounts = hasOtherAccounts,
                )
            },
        )
        verifyPasswordDestination(
            onNavigateBack = { navController.popBackStack() },
            onPasswordVerified = {
                navController.navigateToReviewExport()
            },
        )
        reviewExportDestination(navController = navController)
    }
}

/**
 * Navigates to the main export items graph.
 *
 * This function provides a convenient and type-safe way to initiate the export items flow.
 *
 * @param navOptions Optional [NavOptions] to apply to this navigation action.
 */
fun NavController.navigateToExportItemsGraph(
    navOptions: NavOptions? = null,
) {
    navigate(route = ExportItemsGraphRoute, navOptions = navOptions)
}
