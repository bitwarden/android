package com.x8bit.bitwarden.ui.auth.feature.vaultunlock

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.bitwarden.ui.platform.util.ParcelableRouteSerializer
import com.x8bit.bitwarden.ui.auth.feature.vaultunlock.model.UnlockType
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the vault unlock screen.
 */
@Parcelize
@Serializable(with = VaultUnlockRoute.Serializer::class)
sealed class VaultUnlockRoute : Parcelable {
    /**
     * The underlying [UnlockType] used in the vault unlock screen.
     */
    abstract val unlockType: UnlockType

    /**
     * Custom serializer to support polymorphic routes.
     */
    class Serializer : ParcelableRouteSerializer<VaultUnlockRoute>(VaultUnlockRoute::class)

    /**
     * The type-safe route for the standard vault unlock screen.
     */
    @Parcelize
    @Serializable(with = Standard.Serializer::class)
    data object Standard : VaultUnlockRoute() {
        override val unlockType: UnlockType get() = UnlockType.STANDARD

        /**
         * Custom serializer to support polymorphic routes.
         */
        class Serializer : ParcelableRouteSerializer<Standard>(Standard::class)
    }

    /**
     * The type-safe route for the TDE vault unlock screen.
     */
    @Parcelize
    @Serializable(with = Tde.Serializer::class)
    data object Tde : VaultUnlockRoute() {
        override val unlockType: UnlockType get() = UnlockType.TDE

        /**
         * Custom serializer to support polymorphic routes.
         */
        class Serializer : ParcelableRouteSerializer<Tde>(Tde::class)
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
    val route = this.toRoute<VaultUnlockRoute>()
    return VaultUnlockArgs(unlockType = route.unlockType)
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
