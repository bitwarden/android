package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.privilegedapps.list

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithPushTransitions

private const val PRIVILEGED_APPS_LIST_ROUTE = "settings_privileged_apps"

/**
 * Add privileged apps list destination to the nav graph.
 */
fun NavGraphBuilder.privilegedAppsListDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithPushTransitions(
        route = PRIVILEGED_APPS_LIST_ROUTE,
    ) {
        PrivilegedAppsListScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the privileged apps list screen.
 */
fun NavController.navigateToPrivilegedAppsList(navOptions: NavOptions? = null) {
    navigate(PRIVILEGED_APPS_LIST_ROUTE, navOptions)
}
