@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.tools.feature.send

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.bitwarden.core.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.navigateToSendItemListing
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.sendItemListingDestination
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the send graph.
 */
@Serializable
data object SendGraphRoute

/**
 * Add send destination to the nav graph.
 */
fun NavGraphBuilder.sendGraph(
    navController: NavController,
    onNavigateToAddSend: () -> Unit,
    onNavigateToEditSend: (sendItemId: String) -> Unit,
    onNavigateToSearchSend: (searchType: SearchType.Sends) -> Unit,
) {
    navigation<SendGraphRoute>(
        startDestination = SendRoute,
    ) {
        sendDestination(
            onNavigateToAddSend = onNavigateToAddSend,
            onNavigateToEditSend = onNavigateToEditSend,
            onNavigateToSendFilesList = {
                navController.navigateToSendItemListing(VaultItemListingType.SendFile)
            },
            onNavigateToSendTextList = {
                navController.navigateToSendItemListing(VaultItemListingType.SendText)
            },
            onNavigateToSearchSend = onNavigateToSearchSend,
        )
        sendItemListingDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToAddSendItem = onNavigateToAddSend,
            onNavigateToEditSendItem = onNavigateToEditSend,
            onNavigateToSearchSend = onNavigateToSearchSend,
        )
    }
}

/**
 * Navigate to the send screen. Note this will only work if send screen was added
 * via [sendGraph].
 */
fun NavController.navigateToSendGraph(navOptions: NavOptions? = null) {
    navigate(route = SendGraphRoute, navOptions = navOptions)
}
