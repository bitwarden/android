package com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.vaultUnlockedDestinations

/**
 * The functions below pertain to entry into the [VaultUnlockedNavBarScreen].
 */
const val VAULT_UNLOCKED_NAV_BAR_ROUTE: String = "VaultUnlockedNavBar"

/**
 * Navigate to the vault unlocked nav bar screen.
 * Note this will only work if vault unlocked nav bar destination was added
 * via [vaultUnlockedDestinations].
 */
fun NavController.navigateToVaultUnlockedNavBar(navOptions: NavOptions? = null) {
    navigate(VAULT_UNLOCKED_NAV_BAR_ROUTE, navOptions)
}

/**
 * Add vault unlocked destination to the root nav graph.
 */
fun NavGraphBuilder.vaultUnlockedNavBarDestination() {
    composable(VAULT_UNLOCKED_NAV_BAR_ROUTE) {
        VaultUnlockedNavBarScreen()
    }
}
