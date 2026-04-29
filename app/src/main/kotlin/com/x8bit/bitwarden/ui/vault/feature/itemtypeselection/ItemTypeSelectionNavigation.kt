package com.x8bit.bitwarden.ui.vault.feature.itemtypeselection

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the [ItemTypeSelectionScreen].
 */
@Serializable
data object ItemTypeSelectionRoute

/**
 * Add the [ItemTypeSelectionScreen] destination to the nav graph.
 */
fun NavGraphBuilder.itemTypeSelectionDestination(
    onNavigateBack: () -> Unit,
    onNavigateToAddItem: (VaultItemCipherType) -> Unit,
) {
    composableWithSlideTransitions<ItemTypeSelectionRoute> {
        ItemTypeSelectionScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToAddItem = onNavigateToAddItem,
        )
    }
}

/**
 * Navigate to the [ItemTypeSelectionScreen].
 */
fun NavController.navigateToItemTypeSelection(navOptions: NavOptions? = null) {
    this.navigate(route = ItemTypeSelectionRoute, navOptions = navOptions)
}
