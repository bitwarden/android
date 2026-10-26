package com.x8bit.bitwarden.ui.platform.feature.settings.folders

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraph

/**
 * Add the folders screen to the navigation entries.
 */
fun EntryProviderScope<VaultUnlockedGraph>.foldersDestination(
    onNavigateBack: () -> Unit,
    onNavigateToAddFolderScreen: () -> Unit,
    onNavigateToEditFolderScreen: (folderId: String) -> Unit,
) {
    slideUpEntry<VaultUnlockedGraph.FoldersRoute> {
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
fun NavigationController<VaultUnlockedGraph>.navigateToFolders() {
    this.navigate(route = VaultUnlockedGraph.FoldersRoute)
}
