package com.x8bit.bitwarden.ui.platform.feature.settings.folders

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val FOLDERS_ROUTE = "settings_folders"

/**
 * Add folders destinations to the nav graph.
 */
fun NavGraphBuilder.foldersDestination(
    onNavigateBack: () -> Unit,
    onNavigateToAddFolderScreen: () -> Unit,
    onNavigateToEditFolderScreen: (folderId: String) -> Unit,
) {
    composableWithSlideTransitions(
        route = FOLDERS_ROUTE,
    ) {
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
    navigate(FOLDERS_ROUTE, navOptions)
}
