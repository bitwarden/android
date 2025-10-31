package com.bitwarden.authenticator.ui.authenticator.feature.itemlisting

import androidx.navigation.NavGraphBuilder
import com.bitwarden.ui.platform.base.util.composableWithRootPushTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the item listing screen.
 */
@Serializable
data object ItemListingRoute

/**
 * Add the item listing screen to the nav graph.
 */
fun NavGraphBuilder.itemListingDestination(
    onNavigateBack: () -> Unit = { },
    onNavigateToSearch: () -> Unit,
    onNavigateToQrCodeScanner: () -> Unit = { },
    onNavigateToManualKeyEntry: () -> Unit = { },
    onNavigateToEditItemScreen: (id: String) -> Unit = { },
) {
    composableWithRootPushTransitions<ItemListingRoute> {
        ItemListingScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToSearch = onNavigateToSearch,
            onNavigateToQrCodeScanner = onNavigateToQrCodeScanner,
            onNavigateToManualKeyEntry = onNavigateToManualKeyEntry,
            onNavigateToEditItemScreen = onNavigateToEditItemScreen,
        )
    }
}
