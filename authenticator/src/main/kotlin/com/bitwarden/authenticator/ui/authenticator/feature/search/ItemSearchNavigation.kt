package com.bitwarden.authenticator.ui.authenticator.feature.search

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the item search screen.
 */
@Serializable
data object ItemSearchRoute

/**
 * Add item search destination to the nav graph.
 */
fun NavGraphBuilder.itemSearchDestination(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
) {
    composableWithSlideTransitions<ItemSearchRoute> {
        ItemSearchScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToEdit = onNavigateToEdit,
        )
    }
}

/**
 * Navigate to the item search screen.
 */
fun NavController.navigateToSearch() {
    navigate(route = ItemSearchRoute)
}
