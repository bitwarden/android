package com.x8bit.bitwarden.ui.tools.feature.send.viewsend

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraph

/**
 * Add the view send screen to the navigation entries.
 */
fun EntryProviderScope<VaultUnlockedGraph>.viewSendDestination(
    onNavigateBack: () -> Unit,
    onNavigateToAddEditSend: (route: VaultUnlockedGraph.AddEditSendRoute) -> Unit,
) {
    slideUpEntry<VaultUnlockedGraph.ViewSendRoute> { route ->
        ViewSendScreen(
            viewModel = hiltViewModel<ViewSendViewModel, ViewSendViewModel.Factory>(
                creationCallback = { factory -> factory.create(route = route) },
            ),
            onNavigateBack = onNavigateBack,
            onNavigateToAddEditSend = onNavigateToAddEditSend,
        )
    }
}

/**
 * Navigate to the view send screen.
 */
fun NavigationController<VaultUnlockedGraph>.navigateToViewSend(
    route: VaultUnlockedGraph.ViewSendRoute,
) {
    this.navigate(route = route)
}
