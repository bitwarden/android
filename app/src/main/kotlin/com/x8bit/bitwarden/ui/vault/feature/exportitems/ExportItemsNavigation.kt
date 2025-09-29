@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.vault.feature.exportitems

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.bitwarden.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.vault.feature.exportitems.selectaccount.SelectAccountRoute
import com.x8bit.bitwarden.ui.vault.feature.exportitems.selectaccount.selectAccountDestination
import com.x8bit.bitwarden.ui.vault.feature.exportitems.verifypassword.navigateToVerifyPassword
import com.x8bit.bitwarden.ui.vault.feature.exportitems.verifypassword.verifyPasswordDestination
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the export items graph.
 */
@OmitFromCoverage
@Serializable
data object ExportItemsRoute

/**
 * Add export items destinations to the nav graph.
 */
fun NavGraphBuilder.exportItemsGraph(
    navController: NavController,
) {
    navigation<ExportItemsRoute>(
        startDestination = SelectAccountRoute,
    ) {
        selectAccountDestination(
            onAccountSelected = {
                navController.navigateToVerifyPassword(userId = it)
            },
        )
        verifyPasswordDestination(
            onNavigateBack = { navController.popBackStack() },
            onPasswordVerified = {
                // TODO: [PM-26111] Navigate to confirm export screen.
            },
        )
    }
}

/**
 * Navigate to the export items graph.
 */
fun NavController.navigateToExportItemsGraph(
    navOptions: NavOptions? = null,
) {
    navigate(route = ExportItemsRoute, navOptions = navOptions)
}
