package com.x8bit.bitwarden.ui.tools.feature.send

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.AddEditSendRoute
import com.x8bit.bitwarden.ui.tools.feature.send.viewsend.ViewSendRoute
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
    onNavigateToAddEditSend: (route: AddEditSendRoute) -> Unit,
    onNavigateToViewSend: (ViewSendRoute) -> Unit,
    onNavigateToSearchSend: (searchType: SearchType.Sends) -> Unit,
) {
    navigation<SendGraphRoute>(
        startDestination = SendRoute,
    ) {
        sendDestination(
            onNavigateToAddEditSend = onNavigateToAddEditSend,
            onNavigateToViewSend = onNavigateToViewSend,
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
            onNavigateToAddEditSendItem = onNavigateToAddEditSend,
            onNavigateToViewSendItem = onNavigateToViewSend,
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
