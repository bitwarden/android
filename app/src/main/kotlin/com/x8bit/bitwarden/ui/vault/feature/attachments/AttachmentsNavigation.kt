package com.x8bit.bitwarden.ui.vault.feature.attachments

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraph

/**
 * Add the attachments screen to the navigation entries.
 */
fun EntryProviderScope<VaultUnlockedGraph>.attachmentDestination(
    onNavigateBack: () -> Unit,
    onNavigateToPreviewAttachment: (route: VaultUnlockedGraph.PreviewAttachmentRoute) -> Unit,
    onNavigateToPlan: () -> Unit,
) {
    slideUpEntry<VaultUnlockedGraph.AttachmentsRoute> { route ->
        AttachmentsScreen(
            viewModel = hiltViewModel<AttachmentsViewModel, AttachmentsViewModel.Factory>(
                creationCallback = { factory -> factory.create(route = route) },
            ),
            onNavigateBack = onNavigateBack,
            onNavigateToPreview = onNavigateToPreviewAttachment,
            onNavigateToPlan = onNavigateToPlan,
        )
    }
}

/**
 * Navigate to the attachments screen.
 */
fun NavigationController<VaultUnlockedGraph>.navigateToAttachment(
    cipherId: String,
) {
    this.navigate(route = VaultUnlockedGraph.AttachmentsRoute(cipherId = cipherId))
}
