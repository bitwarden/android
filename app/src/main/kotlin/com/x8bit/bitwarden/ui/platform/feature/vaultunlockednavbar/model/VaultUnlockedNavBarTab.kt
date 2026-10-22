package com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar.model

import android.os.Parcelable
import com.bitwarden.ui.platform.components.navigation.model.NavigationItem
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar.VaultUnlockedNavBarGraph
import kotlinx.parcelize.Parcelize

/**
 * Represents the different tabs available in the navigation bar
 * for the unlocked portion of the vault.
 *
 * Each tab is modeled with properties that provide information on:
 * - Regular icon resource
 * - Icon resource when selected
 * and other essential UI and navigational data.
 */
@Parcelize
sealed class VaultUnlockedNavBarTab : NavigationItem, Parcelable {
    /**
     * The route of the tab within the [VaultUnlockedNavBarGraph].
     */
    abstract override val rootRoute: VaultUnlockedNavBarGraph

    /**
     * Show the Generator screen.
     */
    @Parcelize
    data object Generator : VaultUnlockedNavBarTab() {
        override val iconResSelected get() = BitwardenDrawable.ic_generator_filled
        override val iconRes get() = BitwardenDrawable.ic_generator
        override val labelRes get() = BitwardenString.generator
        override val rootRoute get() = VaultUnlockedNavBarGraph.Generator
        override val testTag get() = "GeneratorTab"
        override val notificationCount get() = 0
    }

    /**
     * Show the Send screen.
     */
    @Parcelize
    data object Send : VaultUnlockedNavBarTab() {
        override val iconResSelected get() = BitwardenDrawable.ic_send_filled
        override val iconRes get() = BitwardenDrawable.ic_send
        override val labelRes get() = BitwardenString.send
        override val rootRoute get() = VaultUnlockedNavBarGraph.Send
        override val testTag get() = "SendTab"
        override val notificationCount get() = 0
    }

    /**
     * Show the Vault screen.
     */
    @Parcelize
    data class Vault(
        override val labelRes: Int,
    ) : VaultUnlockedNavBarTab() {
        override val iconResSelected get() = BitwardenDrawable.ic_vault_filled
        override val iconRes get() = BitwardenDrawable.ic_vault
        override val rootRoute get() = VaultUnlockedNavBarGraph.Vault
        override val testTag get() = "VaultTab"
        override val notificationCount get() = 0
    }

    /**
     * Show the Settings screen.
     */
    @Parcelize
    data class Settings(
        override val notificationCount: Int = 0,
    ) : VaultUnlockedNavBarTab() {
        override val iconResSelected get() = BitwardenDrawable.ic_settings_filled
        override val iconRes get() = BitwardenDrawable.ic_settings
        override val labelRes get() = BitwardenString.settings
        override val rootRoute get() = VaultUnlockedNavBarGraph.Settings
        override val testTag get() = "SettingsTab"
    }
}
