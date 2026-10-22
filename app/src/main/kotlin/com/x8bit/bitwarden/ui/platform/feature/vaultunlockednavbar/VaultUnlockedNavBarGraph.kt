package com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar

import androidx.navigation3.runtime.NavKey
import com.x8bit.bitwarden.ui.platform.feature.settings.SettingsGraph
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorGraph
import com.x8bit.bitwarden.ui.tools.feature.send.SendGraph
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultGraph
import kotlinx.serialization.Serializable

/**
 * Represents the hierarchy of the Vault Unlocked Navigation Bar UI navigation graph and provides
 * type-safe routes to each tab available in the graph. Each tab hosts its own nested navigation
 * graph.
 */
@Serializable
sealed class VaultUnlockedNavBarGraph : NavKey {
    /**
     * The type-safe route for the vault tab, which hosts the [VaultGraph].
     */
    @Serializable
    data object Vault : VaultUnlockedNavBarGraph()

    /**
     * The type-safe route for the send tab, which hosts the [SendGraph].
     */
    @Serializable
    data object Send : VaultUnlockedNavBarGraph()

    /**
     * The type-safe route for the generator tab, which hosts the [GeneratorGraph].
     */
    @Serializable
    data object Generator : VaultUnlockedNavBarGraph()

    /**
     * The type-safe route for the settings tab, which hosts the [SettingsGraph].
     */
    @Serializable
    data object Settings : VaultUnlockedNavBarGraph()
}
