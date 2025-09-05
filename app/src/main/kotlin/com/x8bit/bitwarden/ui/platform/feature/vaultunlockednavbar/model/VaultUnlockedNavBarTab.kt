package com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar.model

import android.os.Parcelable
import com.bitwarden.ui.platform.components.navigation.model.NavigationItem
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.ui.platform.feature.settings.SettingsGraphRoute
import com.x8bit.bitwarden.ui.platform.feature.settings.SettingsRoute
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorGraphRoute
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorRoute
import com.x8bit.bitwarden.ui.tools.feature.send.SendGraphRoute
import com.x8bit.bitwarden.ui.tools.feature.send.SendRoute
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultGraphRoute
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultRoute
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
     * Show the Generator screen.
     */
    @Parcelize
    data object Generator : VaultUnlockedNavBarTab() {
        override val iconResSelected get() = BitwardenDrawable.ic_generator_filled
        override val iconRes get() = BitwardenDrawable.ic_generator
        override val labelRes get() = BitwardenString.generator
        override val contentDescriptionRes get() = BitwardenString.generator
        override val graphRoute get() = GeneratorGraphRoute
        override val startDestinationRoute get() = GeneratorRoute.Standard
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
        override val contentDescriptionRes get() = BitwardenString.send
        override val graphRoute get() = SendGraphRoute
        override val startDestinationRoute get() = SendRoute
        override val testTag get() = "SendTab"
        override val notificationCount get() = 0
    }

    /**
     * Show the Vault screen.
     */
    @Parcelize
    data class Vault(
        override val labelRes: Int,
        override val contentDescriptionRes: Int,
    ) : VaultUnlockedNavBarTab() {
        override val iconResSelected get() = BitwardenDrawable.ic_vault_filled
        override val iconRes get() = BitwardenDrawable.ic_vault
        override val graphRoute get() = VaultGraphRoute
        override val startDestinationRoute get() = VaultRoute
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
        override val contentDescriptionRes get() = BitwardenString.settings
        override val graphRoute get() = SettingsGraphRoute
        override val startDestinationRoute get() = SettingsRoute.Standard
        override val testTag get() = "SettingsTab"
    }
}
