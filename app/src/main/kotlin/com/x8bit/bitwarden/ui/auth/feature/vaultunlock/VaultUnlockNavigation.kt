package com.x8bit.bitwarden.ui.auth.feature.vaultunlock

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.model.UnlockType
import com.x8bit.bitwarden.ui.platform.util.toObjectRoute
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the vault unlock screen.
 */
@Serializable
sealed class VaultUnlockRoute {
    /**
     * The underlying [UnlockType] used in the vault unlock screen.
     */
    abstract val unlockType: UnlockType

    /**
     * The type-safe route for the standard vault unlock screen.
     */
    @Serializable
    data object Standard : VaultUnlockRoute() {
        override val unlockType: UnlockType get() = UnlockType.STANDARD
    }

    /**
     * The type-safe route for the TDE vault unlock screen.
     */
    @Serializable
    data object Tde : VaultUnlockRoute() {
        override val unlockType: UnlockType get() = UnlockType.TDE
    }
}

/**
 * Class to retrieve vault unlock arguments from the [SavedStateHandle].
 */
data class VaultUnlockArgs(
    val unlockType: UnlockType,
)

/**
 * Constructs a [VaultUnlockArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toVaultUnlockArgs(): VaultUnlockArgs {
    val route = this.toObjectRoute<VaultUnlockRoute.Tde>()
        ?: this.toObjectRoute<VaultUnlockRoute.Standard>()
    return route
        ?.let { VaultUnlockArgs(unlockType = it.unlockType) }
        ?: throw IllegalStateException("Missing correct route for VaultUnlockScreen")
}

/**
 * Navigate to the Vault Unlock screen.
 */
fun NavController.navigateToVaultUnlock(
    navOptions: NavOptions? = null,
) {
    navigate(
        route = VaultUnlockRoute.Standard,
        navOptions = navOptions,
    )
}

/**
 * Add the Vault Unlock screen to the nav graph.
 */
fun NavGraphBuilder.vaultUnlockDestination() {
    composable<VaultUnlockRoute.Standard> {
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
        route = VaultUnlockRoute.Tde,
        navOptions = navOptions,
    )
}

/**
 * Add the Vault Unlock screen to the TDE nav graph.
 */
fun NavGraphBuilder.tdeVaultUnlockDestination() {
    composable<VaultUnlockRoute.Tde> {
        VaultUnlockScreen()
    }
}
