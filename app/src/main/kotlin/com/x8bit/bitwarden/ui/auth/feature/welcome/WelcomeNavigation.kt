package com.x8bit.bitwarden.ui.auth.feature.welcome

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithStayTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the welcome screen.
 */
@Serializable
data object WelcomeRoute

/**
 * Navigate to the welcome screen.
 */
fun NavController.navigateToWelcome(navOptions: NavOptions? = null) {
    this.navigate(route = WelcomeRoute, navOptions = navOptions)
}

/**
 * Add the Welcome screen to the nav graph.
 */
fun NavGraphBuilder.welcomeDestination(
    onNavigateToLogin: () -> Unit,
    onNavigateToStartRegistration: () -> Unit,
) {
    composableWithStayTransitions<WelcomeRoute> {
        WelcomeScreen(
            onNavigateToLogin = onNavigateToLogin,
            onNavigateToStartRegistration = onNavigateToStartRegistration,
        )
    }
}
