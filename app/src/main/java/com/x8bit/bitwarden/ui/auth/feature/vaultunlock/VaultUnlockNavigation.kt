package com.x8bit.bitwarden.ui.auth.feature.vaultunlock

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.model.UnlockType

private const val VAULT_UNLOCK_TYPE: String = "unlock_type"
private const val TDE_VAULT_UNLOCK_ROUTE_PREFIX: String = "tde_vault_unlock"
private const val TDE_VAULT_UNLOCK_ROUTE: String =
    "$TDE_VAULT_UNLOCK_ROUTE_PREFIX/{$VAULT_UNLOCK_TYPE}"
private const val VAULT_UNLOCK_ROUTE_PREFIX: String = "vault_unlock"
const val VAULT_UNLOCK_ROUTE: String = "$VAULT_UNLOCK_ROUTE_PREFIX/{$VAULT_UNLOCK_TYPE}"

/**
 * Class to retrieve vault unlock arguments from the [SavedStateHandle].
 */
@OmitFromCoverage
data class VaultUnlockArgs(
    val unlockType: UnlockType,
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        unlockType = checkNotNull(savedStateHandle.get<UnlockType>(VAULT_UNLOCK_TYPE)),
    )
}

/**
 * Navigate to the Vault Unlock screen.
 */
fun NavController.navigateToVaultUnlock(
    navOptions: NavOptions? = null,
) {
    navigate(
        route = "$VAULT_UNLOCK_ROUTE_PREFIX/${UnlockType.STANDARD}",
        navOptions = navOptions,
    )
}

/**
 * Add the Vault Unlock screen to the nav graph.
 */
fun NavGraphBuilder.vaultUnlockDestination() {
    composable(
        route = VAULT_UNLOCK_ROUTE,
        arguments = listOf(
            navArgument(VAULT_UNLOCK_TYPE) { type = NavType.EnumType(UnlockType::class.java) },
        ),
    ) {
        VaultUnlockScreen()
    }
}

/**
 * Navigate to the Vault Unlock screen for TDE.
 */
fun NavController.navigateToTdeVaultUnlock(
    navOptions: NavOptions? = null,
) {
    navigate(
        route = "$TDE_VAULT_UNLOCK_ROUTE_PREFIX/${UnlockType.TDE}",
        navOptions = navOptions,
    )
}

/**
 * Add the Vault Unlock screen to the TDE nav graph.
 */
fun NavGraphBuilder.tdeVaultUnlockDestination() {
    composable(
        route = TDE_VAULT_UNLOCK_ROUTE,
        arguments = listOf(
            navArgument(VAULT_UNLOCK_TYPE) { type = NavType.EnumType(UnlockType::class.java) },
        ),
    ) {
        VaultUnlockScreen()
    }
}
