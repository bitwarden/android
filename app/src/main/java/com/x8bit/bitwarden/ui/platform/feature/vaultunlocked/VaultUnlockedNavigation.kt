package com.x8bit.bitwarden.ui.platform.feature.vaultunlocked

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar.VAULT_UNLOCKED_NAV_BAR_ROUTE
import com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar.vaultUnlockedNavBarDestination

const val VAULT_UNLOCKED_GRAPH_ROUTE: String = "vault_unlocked_graph"

/**
 * Navigate to the vault unlocked screen.
 */
fun NavController.navigateToVaultUnlockedGraph(navOptions: NavOptions? = null) {
    navigate(VAULT_UNLOCKED_GRAPH_ROUTE, navOptions)
}

/**
 * Add vault unlocked destinations to the root nav graph.
 */
fun NavGraphBuilder.vaultUnlockedGraph() {
    navigation(
        startDestination = VAULT_UNLOCKED_NAV_BAR_ROUTE,
        route = VAULT_UNLOCKED_GRAPH_ROUTE,
    ) {
        vaultUnlockedNavBarDestination()
    }
}
