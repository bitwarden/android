package com.x8bit.bitwarden.ui.auth.feature.environment

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the environment screen.
 */
@Serializable
data object EnvironmentRoute

/**
 * Add the environment destination to the nav graph.
 */
fun NavGraphBuilder.environmentDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions<EnvironmentRoute> {
        EnvironmentScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the environment screen.
 */
fun NavController.navigateToEnvironment(navOptions: NavOptions? = null) {
    this.navigate(route = EnvironmentRoute, navOptions = navOptions)
}
