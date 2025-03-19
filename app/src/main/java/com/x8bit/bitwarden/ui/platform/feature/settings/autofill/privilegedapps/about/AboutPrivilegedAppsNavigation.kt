package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.privilegedapps.about

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithPushTransitions

private const val ABOUT_PRIVILEGED_APPS_ROUTE = "about_privileged_apps"

/**
 * Add about privileged apps destination to the nav graph.
 */
fun NavGraphBuilder.aboutPrivilegedAppsDestination(
    navigateBack: () -> Unit,
) {
    composableWithPushTransitions(
        route = ABOUT_PRIVILEGED_APPS_ROUTE,
    ) {
        AboutPrivilegedAppsScreen(
            onNavigateBack = navigateBack,
        )
    }
}

/**
 * Navigate to the about privileged apps screen.
 */
fun NavController.navigateToAboutPrivilegedAppsScreen(
    navOptions: NavOptions? = null,
) {
    navigate(ABOUT_PRIVILEGED_APPS_ROUTE, navOptions)
}
