package com.x8bit.bitwarden.ui.platform.feature.settings.folders

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the folders screen.
 */
@Serializable
data object FoldersRoute

/**
 * Add folders destinations to the nav graph.
 */
fun NavGraphBuilder.foldersDestination(
    onNavigateBack: () -> Unit,
    onNavigateToAddFolderScreen: () -> Unit,
    onNavigateToEditFolderScreen: (folderId: String) -> Unit,
) {
    composableWithSlideTransitions<FoldersRoute> {
        FoldersScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToAddFolderScreen = onNavigateToAddFolderScreen,
            onNavigateToEditFolderScreen = onNavigateToEditFolderScreen,
        )
    }
}

/**
 * Navigate to the folders screen.
 */
fun NavController.navigateToFolders(navOptions: NavOptions? = null) {
    this.navigate(route = FoldersRoute, navOptions = navOptions)
}
