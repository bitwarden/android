package com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar.model

import android.os.Parcelable
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.feature.settings.SETTINGS_GRAPH_ROUTE
import com.x8bit.bitwarden.ui.tools.feature.generator.GENERATOR_GRAPH_ROUTE
import com.x8bit.bitwarden.ui.tools.feature.send.SEND_GRAPH_ROUTE
import com.x8bit.bitwarden.ui.vault.feature.vault.VAULT_GRAPH_ROUTE
import kotlinx.parcelize.Parcelize

/**
 * Represents the different tabs available in the navigation bar
 * for the unlocked portion of the vault.
 *
 * Each tab is modeled with properties that provide information on:
 * - Regular icon resource
 * - Icon resource when selected
 * and other essential UI and navigational data.
 *
 * @property iconRes The resource ID for the regular (unselected) icon representing the tab.
 * @property iconResSelected The resource ID for the icon representing the tab when it's selected.
 */
@Parcelize
sealed class VaultUnlockedNavBarTab : Parcelable {
    /**
     * The resource ID for the icon representing the tab when it is selected.
     */
    abstract val iconResSelected: Int

    /**
     * Resource id for the icon representing the tab.
     */
    abstract val iconRes: Int

    /**
     * Resource id for the label describing the tab.
     */
    abstract val labelRes: Int

    /**
     * Resource id for the content description describing the tab.
     */
    abstract val contentDescriptionRes: Int

    /**
     * Route of the tab.
     */
    abstract val route: String

    /**
     * The test tag of the tab.
     */
    abstract val testTag: String

    /**
     * Show the Generator screen.
     */
    @Parcelize
    data object Generator : VaultUnlockedNavBarTab() {
        override val iconResSelected get() = R.drawable.ic_generator_filled
        override val iconRes get() = R.drawable.ic_generator
        override val labelRes get() = R.string.generator
        override val contentDescriptionRes get() = R.string.generator
        override val route get() = GENERATOR_GRAPH_ROUTE
        override val testTag get() = "GeneratorTab"
    }

    /**
     * Show the Send screen.
     */
    @Parcelize
    data object Send : VaultUnlockedNavBarTab() {
        override val iconResSelected get() = R.drawable.ic_send_filled
        override val iconRes get() = R.drawable.ic_send
        override val labelRes get() = R.string.send
        override val contentDescriptionRes get() = R.string.send
        override val route get() = SEND_GRAPH_ROUTE
        override val testTag get() = "SendTab"
    }

    /**
     * Show the Vault screen.
     */
    @Parcelize
    data class Vault(
        override val labelRes: Int,
        override val contentDescriptionRes: Int,
    ) : VaultUnlockedNavBarTab() {
        override val iconResSelected get() = R.drawable.ic_vault_filled
        override val iconRes get() = R.drawable.ic_vault
        override val route get() = VAULT_GRAPH_ROUTE
        override val testTag get() = "VaultTab"
    }

    /**
     * Show the Settings screen.
     */
    @Parcelize
    data object Settings : VaultUnlockedNavBarTab() {
        override val iconResSelected get() = R.drawable.ic_settings_filled
        override val iconRes get() = R.drawable.ic_settings
        override val labelRes get() = R.string.settings
        override val contentDescriptionRes get() = R.string.settings
        override val route get() = SETTINGS_GRAPH_ROUTE
        override val testTag get() = "SettingsTab"
    }
}
