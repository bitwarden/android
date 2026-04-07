package com.bitwarden.ui.platform.base.util

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.navOptions
import com.bitwarden.ui.platform.components.navigation.model.NavigationItem
import com.bitwarden.ui.platform.util.toObjectNavigationRoute

/**
 * A helper function to determine how to navigate to a specified [NavigationItem].
 *
 * This function intelligently handles navigation based on the current destination:
 * - If already at the target start destination, no action is taken.
 * - If in the correct graph but not at start, pops back to start destination of the graph.
 * - Otherwise, navigates to the target graph with appropriate [NavOptions].
 *
 * @param target The [NavigationItem] representing the desired navigation target
 */
fun NavController.navigateToTabOrRoot(target: NavigationItem) {
    if (target.startDestinationRoute.toObjectNavigationRoute() == currentDestination?.route) {
        // We are at the start destination already, so nothing to do.
        return
    } else if (target.graphRoute.toObjectNavigationRoute() == currentDestination?.parent?.route) {
        // We are not at the start destination but we are in the correct graph,
        // so lets pop up to the start destination.
        popBackStack(route = target.startDestinationRoute, inclusive = false)
        return
    } else {
        // We are not in correct graph at all, so navigate there.
        navigate(
            route = target.graphRoute,
            navOptions = navOptions {
                popUpTo(id = graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            },
        )
    }
}
