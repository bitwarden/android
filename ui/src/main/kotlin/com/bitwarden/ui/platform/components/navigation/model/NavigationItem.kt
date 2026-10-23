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

    /**
     * The test tag of the tab.
     */
    val testTag: String

    /**
     * The amount of notifications for items that fall under this tab.
     */
    val notificationCount: Int
}
