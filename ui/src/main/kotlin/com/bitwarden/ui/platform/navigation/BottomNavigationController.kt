package com.bitwarden.ui.platform.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Controls navigation within a bottom navigation set of back stacks.
 */
interface BottomNavigationController<BottomNavGraph : NavKey> {
    /**
     * The controller for the bottom navigation graph.
     */
    val root: NavigationController<BottomNavGraph>

    /**
     * A list of all back stacks in the bottom navigation, including the root bottom nav.
     */
    val backStacks: List<List<NavKey>>

    /**
     * Navigates to the selected tab route or pops the backstack up to the root of the already
     * selected tab route.
     */
    fun navigateToTabOrPopToRoot(route: BottomNavGraph)

    /**
     * Navigates back within the back stack of the selected tab. When the selected tab is already
     * at its root, the start tab is selected instead, so the user always exits through the start
     * tab.
     *
     * @return `true` if the navigation was handled.
     */
    fun navigateBack(): Boolean
}
