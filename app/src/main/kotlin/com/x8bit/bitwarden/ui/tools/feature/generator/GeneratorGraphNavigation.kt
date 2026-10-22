package com.x8bit.bitwarden.ui.tools.feature.generator

import androidx.navigation3.runtime.EntryProviderScope
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraph

/**
 * Add the generator destinations to the generator navigation entries. The
 * [GeneratorGraph.GeneratorRoute] is the root of the generator tab's back stack.
 */
fun EntryProviderScope<GeneratorGraph>.generatorDestinations(
    navigateToVaultAddItem: (route: VaultUnlockedGraph.VaultAddEditRoute) -> Unit,
    onNavigateToPasswordHistory: () -> Unit,
    onDimNavBarRequest: (Boolean) -> Unit,
) {
    generatorDestination(
        navigateToVaultAddItem = navigateToVaultAddItem,
        onNavigateToPasswordHistory = onNavigateToPasswordHistory,
        onDimNavBarRequest = onDimNavBarRequest,
    )
}
