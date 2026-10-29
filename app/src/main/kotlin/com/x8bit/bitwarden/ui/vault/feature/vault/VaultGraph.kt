package com.x8bit.bitwarden.ui.vault.feature.vault

import androidx.navigation3.runtime.NavKey
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType
import kotlinx.serialization.Serializable

/**
 * Represents the hierarchy of the Vault tab UI navigation graph and provides type-safe routes to
 * each destination available in the graph.
 */
@Serializable
sealed class VaultGraph : NavKey {
    /**
     * The type-safe route for the vault screen.
     */
    @Serializable
    data object VaultRoute : VaultGraph()

    /**
     * The type-safe route for the vault item listing screen.
     */
    @Serializable
    data class VaultItemListingRoute(
        val vaultItemListingType: VaultItemListingType,
    ) : VaultGraph()

    /**
     * The type-safe route for the verification code screen.
     */
    @Serializable
    data object VerificationCodeRoute : VaultGraph()
}
