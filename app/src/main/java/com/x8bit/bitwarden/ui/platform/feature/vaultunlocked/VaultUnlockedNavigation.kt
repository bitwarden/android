package com.x8bit.bitwarden.ui.platform.feature.vaultunlocked

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar.VAULT_UNLOCKED_NAV_BAR_ROUTE
import com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar.vaultUnlockedNavBarDestination

const val VAULT_UNLOCKED_ROUTE: String = "VaultUnlocked"

/**
 * Navigate to the vault unlocked screen.
 */
fun NavController.navigateToVaultUnlocked(navOptions: NavOptions? = null) {
    navigate(VAULT_UNLOCKED_ROUTE, navOptions)
}

/**
 * Add vault unlocked destinations to the root nav graph.
 */
fun NavGraphBuilder.vaultUnlockedDestinations(navController: NavHostController) {
    navigation(
        startDestination = VAULT_UNLOCKED_NAV_BAR_ROUTE,
        route = VAULT_UNLOCKED_ROUTE,
    ) {
        vaultUnlockedNavBarDestination()
    }
}
