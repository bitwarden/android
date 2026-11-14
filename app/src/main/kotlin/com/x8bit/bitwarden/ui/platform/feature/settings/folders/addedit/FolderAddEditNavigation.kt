package com.x8bit.bitwarden.ui.platform.feature.settings.folders.addedit

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.model.FolderAddEditType
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraph

/**
 * Add the folder add & edit screen to the navigation entries.
 */
fun EntryProviderScope<VaultUnlockedGraph>.folderAddEditDestination(
    onNavigateBack: () -> Unit,
) {
    slideUpEntry<VaultUnlockedGraph.FolderAddEditRoute> { route ->
        FolderAddEditScreen(
            viewModel = hiltViewModel<FolderAddEditViewModel, FolderAddEditViewModel.Factory>(
                creationCallback = { factory -> factory.create(route = route) },
            ),
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the folder add & edit screen.
 */
fun NavigationController<VaultUnlockedGraph>.navigateToFolderAddEdit(
    folderAddEditType: FolderAddEditType,
    parentFolderName: String? = null,
) {
    this.navigate(
        route = VaultUnlockedGraph.FolderAddEditRoute(
            folderAddEditType = folderAddEditType,
            parentFolderName = parentFolderName,
        ),
    )
}
