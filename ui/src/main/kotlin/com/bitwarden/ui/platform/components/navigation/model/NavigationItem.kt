package com.bitwarden.ui.platform.components.navigation.model

import androidx.navigation3.runtime.NavKey

/**
 * Represents a user-interactable item to navigate a user via the bottom app bar or navigation rail.
 */
interface NavigationItem {
    /**
     * The resource ID for the icon representing the tab when it is selected.
     */
    val iconResSelected: Int

    /**
     * Resource id for the icon representing the tab.
     */
    val iconRes: Int

    /**
     * Resource id for the label describing the tab.
     */
    val labelRes: Int

    /**
     * The route of the tab's root destination, which also identifies the tab's back stack.
     */
    val rootRoute: NavKey
        // TODO: Remove this default once all modules have been migrated to Navigation3 (PM-45372).
        get() = error("rootRoute is not supported by legacy Navigation Compose tabs.")

    /**
     * Legacy route of the tab's Navigation Compose graph.
     *
     * TODO: Remove once all modules have been migrated to Navigation3 (PM-45372).
     */
    val graphRoute: Any
        get() = rootRoute

    /**
     * Legacy route of the tab's Navigation Compose start destination.
     *
     * TODO: Remove once all modules have been migrated to Navigation3 (PM-45372).
     */
    val startDestinationRoute: Any
        get() = rootRoute

    /**
     * The test tag of the tab.
     */
    val testTag: String

    /**
     * The amount of notifications for items that fall under this tab.
     */
    val notificationCount: Int
}
