package com.x8bit.bitwarden.ui.platform.feature.settings.collections

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the collections screen.
 */
@Serializable
data object CollectionsRoute

/**
 * Add collections destinations to the nav graph.
 */
fun NavGraphBuilder.collectionsDestination(
    onNavigateBack: () -> Unit,
    onNavigateToAddCollectionScreen: (organizationId: String) -> Unit,
    onNavigateToEditCollectionScreen: (
        collectionId: String,
        organizationId: String,
    ) -> Unit,
) {
    composableWithSlideTransitions<CollectionsRoute> {
        CollectionsScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToAddCollectionScreen = onNavigateToAddCollectionScreen,
            onNavigateToEditCollectionScreen = onNavigateToEditCollectionScreen,
        )
    }
}

/**
 * Navigate to the collections screen.
 */
fun NavController.navigateToCollections(navOptions: NavOptions? = null) {
    this.navigate(route = CollectionsRoute, navOptions = navOptions)
}
