@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.privilegedapps.about

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route to the about privileged apps screen.
 */
@Serializable
data object AboutPrivilegedAppsRoute

/**
 * Add about privileged apps destination to the nav graph.
 */
fun NavGraphBuilder.aboutPrivilegedAppsDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions<AboutPrivilegedAppsRoute> {
        AboutPrivilegedAppsScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the about privileged apps screen.
 */
fun NavController.navigateToAboutPrivilegedAppsScreen(
    navOptions: NavOptions? = null,
) {
    navigate(route = AboutPrivilegedAppsRoute, navOptions = navOptions)
}
