package com.bitwarden.authenticator.ui.authenticator.feature.itemlisting

import androidx.navigation.NavGraphBuilder
import com.bitwarden.authenticator.ui.platform.base.util.composableWithPushTransitions

const val ITEM_LIST_ROUTE = "item_list"

/**
 * Add the item listing screen to the nav graph.
 */
fun NavGraphBuilder.itemListingDestination(
    onNavigateBack: () -> Unit = { },
    onNavigateToSearch: () -> Unit,
    onNavigateToQrCodeScanner: () -> Unit = { },
    onNavigateToManualKeyEntry: () -> Unit = { },
    onNavigateToEditItemScreen: (id: String) -> Unit = { },
    onNavigateToSyncWithBitwardenScreen: () -> Unit = { },
    onNavigateToImportScreen: () -> Unit = { },
) {
    composableWithPushTransitions(
        route = ITEM_LIST_ROUTE,
    ) {
        ItemListingScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToSearch = onNavigateToSearch,
            onNavigateToQrCodeScanner = onNavigateToQrCodeScanner,
            onNavigateToManualKeyEntry = onNavigateToManualKeyEntry,
            onNavigateToEditItemScreen = onNavigateToEditItemScreen,
            onNavigateToSyncWithBitwardenScreen = onNavigateToSyncWithBitwardenScreen,
            onNavigateToImportScreen = onNavigateToImportScreen
        )
    }
}
