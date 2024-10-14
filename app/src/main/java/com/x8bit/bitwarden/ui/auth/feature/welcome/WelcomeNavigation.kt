package com.x8bit.bitwarden.ui.auth.feature.welcome

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithStayTransitions

private const val WELCOME_ROUTE: String = "welcome"

/**
 * Navigate to the welcome screen.
 */
fun NavController.navigateToWelcome(navOptions: NavOptions? = null) {
    this.navigate(WELCOME_ROUTE, navOptions)
}

/**
 * Add the Welcome screen to the nav graph.
 */
fun NavGraphBuilder.welcomeDestination(
    onNavigateToCreateAccount: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToStartRegistration: () -> Unit,
) {
    composableWithStayTransitions(
        route = WELCOME_ROUTE,
    ) {
        WelcomeScreen(
            onNavigateToCreateAccount = onNavigateToCreateAccount,
            onNavigateToLogin = onNavigateToLogin,
            onNavigateToStartRegistration = onNavigateToStartRegistration,
        )
    }
}
