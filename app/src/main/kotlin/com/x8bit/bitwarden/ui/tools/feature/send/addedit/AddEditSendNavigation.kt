package com.x8bit.bitwarden.ui.tools.feature.send.addedit

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraph
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorMode
import kotlinx.serialization.Serializable

/**
 * Indicates the mode of send to be displayed.
 */
@Serializable
enum class ModeType {
    ADD,
    COPY,
    EDIT,
}

/**
 * Add the add/edit send screen to the navigation entries.
 */
fun EntryProviderScope<VaultUnlockedGraph>.addEditSendDestination(
    onNavigateBack: () -> Unit,
    onNavigateUpToSearchOrRoot: () -> Unit,
    onNavigateToGeneratorModal: (GeneratorMode.Modal) -> Unit,
    onNavigateToPlan: () -> Unit,
) {
    slideUpEntry<VaultUnlockedGraph.AddEditSendRoute> { route ->
        AddEditSendScreen(
            viewModel = hiltViewModel<AddEditSendViewModel, AddEditSendViewModel.Factory>(
                creationCallback = { factory -> factory.create(route = route) },
            ),
            onNavigateBack = onNavigateBack,
            onNavigateUpToSearchOrRoot = onNavigateUpToSearchOrRoot,
            onNavigateToGeneratorModal = onNavigateToGeneratorModal,
            onNavigateToPlan = onNavigateToPlan,
        )
    }
}

/**
 * Navigate to the add/edit send screen.
 */
fun NavigationController<VaultUnlockedGraph>.navigateToAddEditSend(
    route: VaultUnlockedGraph.AddEditSendRoute,
) {
    this.navigate(route = route)
}
