package com.bitwarden.ui.platform.base.util

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.navOptions
import com.bitwarden.ui.platform.components.navigation.model.NavigationItem
import com.bitwarden.ui.platform.util.toObjectNavigationRoute

/**
 * Helper function to determine how to navigate to a specified [NavigationItem]. If direct
 * navigation is required, the [navigate] lambda will be invoked with the appropriate [NavOptions].
 */
fun NavController.navigateToTabOrRoot(
    target: NavigationItem,
    navigate: (NavOptions) -> Unit,
) {
    if (target.startDestinationRoute.toObjectNavigationRoute() == currentDestination?.route) {
        // We are at the start destination already, so nothing to do.
        return
    } else if (target.graphRoute.toObjectNavigationRoute() == currentDestination?.parent?.route) {
        // We are not at the start destination but we are in the correct graph,
        // so lets pop up to the start destination.
        popBackStack(route = target.startDestinationRoute, inclusive = false)
    } else {
        // We are not in correct graph at all, so navigate there.
        navigate(
            navOptions {
                popUpTo(id = graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            },
        )
    }
}
