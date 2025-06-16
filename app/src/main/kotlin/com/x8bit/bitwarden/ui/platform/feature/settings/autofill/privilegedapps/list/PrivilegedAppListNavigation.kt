@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.privilegedapps.list

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.base.util.composableWithPushTransitions
import kotlinx.serialization.Serializable

/**
 * Type-safe route object for navigating to the privileged apps list screen.
 */
@Serializable
data object PrivilegedAppListRoute

/**
 * Add privileged apps list destination to the nav graph.
 */
fun NavGraphBuilder.privilegedAppsListDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithPushTransitions<PrivilegedAppListRoute> {
        PrivilegedAppsListScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the privileged apps list screen.
 */
fun NavController.navigateToPrivilegedAppsList(navOptions: NavOptions? = null) {
    navigate(route = PrivilegedAppListRoute, navOptions = navOptions)
}
