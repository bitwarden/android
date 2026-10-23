package com.x8bit.bitwarden.ui.tools.feature.generator

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.rootPushEntry
import com.bitwarden.ui.platform.navigation.util.slideUpEntry
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraph
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorMode

/**
 * Add the standard generator destination to the generator navigation entries.
 */
fun EntryProviderScope<GeneratorGraph>.generatorDestination(
    navigateToVaultAddItem: (route: VaultUnlockedGraph.VaultAddEditRoute) -> Unit,
    onNavigateToPasswordHistory: () -> Unit,
    onDimNavBarRequest: (Boolean) -> Unit,
) {
    rootPushEntry<GeneratorGraph.GeneratorRoute> {
        GeneratorScreen(
            viewModel = generatorViewModel(generatorMode = GeneratorMode.Default),
            navigateToVaultAddItem = navigateToVaultAddItem,
            onNavigateToPasswordHistory = onNavigateToPasswordHistory,
            onNavigateBack = {},
            onDimNavBarRequest = onDimNavBarRequest,
        )
    }
}

/**
 * Add the generator modal destination to the vault unlocked navigation entries.
 */
fun EntryProviderScope<VaultUnlockedGraph>.generatorModalDestination(
    onNavigateBack: () -> Unit,
) {
    slideUpEntry<VaultUnlockedGraph.GeneratorModalRoute> { route ->
        GeneratorScreen(
            viewModel = generatorViewModel(generatorMode = route.generatorMode),
            navigateToVaultAddItem = {},
            onNavigateToPasswordHistory = {},
            onNavigateBack = onNavigateBack,
            onDimNavBarRequest = {},
        )
    }
}

/**
 * Creates the [GeneratorViewModel] for the given [generatorMode].
 */
@Composable
private fun generatorViewModel(
    generatorMode: GeneratorMode,
): GeneratorViewModel =
    hiltViewModel<GeneratorViewModel, GeneratorViewModel.Factory>(
        creationCallback = { factory -> factory.create(generatorMode = generatorMode) },
    )

/**
 * Navigate to the generator screen in the given mode with the corresponding website, if one exists.
 */
fun NavigationController<VaultUnlockedGraph>.navigateToGeneratorModal(
    mode: GeneratorMode.Modal,
) {
    navigate(route = VaultUnlockedGraph.GeneratorModalRoute(generatorMode = mode))
}
