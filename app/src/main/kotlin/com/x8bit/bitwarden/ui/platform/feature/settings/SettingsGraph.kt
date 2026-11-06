package com.x8bit.bitwarden.ui.platform.feature.settings

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Represents the hierarchy of the Settings tab UI navigation graph and provides type-safe routes
 * to each destination available in the graph.
 */
@Serializable
sealed class SettingsGraph : NavKey {
    /**
     * The type-safe route for the settings screen.
     */
    @Serializable
    data object SettingsRoute : SettingsGraph()

    /**
     * The type-safe route for the account security screen.
     */
    @Serializable
    data object AccountSecurityRoute : SettingsGraph()

    /**
     * The type-safe route for the autofill screen.
     */
    @Serializable
    data object AutofillRoute : SettingsGraph()

    /**
     * The type-safe route for the block autofill screen.
     */
    @Serializable
    data object BlockAutofillRoute : SettingsGraph()

    /**
     * The type-safe route for the privileged apps list screen.
     */
    @Serializable
    data object PrivilegedAppListRoute : SettingsGraph()

    /**
     * The type-safe route for the vault settings screen.
     */
    @Serializable
    data object VaultSettingsRoute : SettingsGraph()

    /**
     * The type-safe route for the import items screen.
     */
    @Serializable
    data object ImportItemsRoute : SettingsGraph()

    /**
     * The type-safe route for the appearance screen.
     */
    @Serializable
    data object AppearanceRoute : SettingsGraph()

    /**
     * The type-safe route for the other screen.
     */
    @Serializable
    data object OtherRoute : SettingsGraph()

    /**
     * The type-safe route for the about screen.
     */
    @Serializable
    data object AboutRoute : SettingsGraph()

    /**
     * The type-safe route for the plan screen, with the navigation bar visible.
     */
    @Serializable
    data object PlanRoute : SettingsGraph()
}
