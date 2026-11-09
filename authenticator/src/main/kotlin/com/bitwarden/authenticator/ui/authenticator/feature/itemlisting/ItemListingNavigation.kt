package com.bitwarden.authenticator.ui.authenticator.feature.itemlisting

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.util.rootPushEntry

/**
 * Add the item listing screen to the navigation entries.
 */
fun EntryProviderScope<ItemListingGraph>.itemListingDestination(
    onNavigateBack: () -> Unit = { },
    onNavigateToSearch: () -> Unit,
    onNavigateToQrCodeScanner: () -> Unit = { },
    onNavigateToManualKeyEntry: () -> Unit = { },
    onNavigateToEditItemScreen: (id: String) -> Unit = { },
) {
    rootPushEntry<ItemListingGraph.ItemListingRoute> {
        ItemListingScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToSearch = onNavigateToSearch,
            onNavigateToQrCodeScanner = onNavigateToQrCodeScanner,
            onNavigateToManualKeyEntry = onNavigateToManualKeyEntry,
            onNavigateToEditItemScreen = onNavigateToEditItemScreen,
        )
    }
}
