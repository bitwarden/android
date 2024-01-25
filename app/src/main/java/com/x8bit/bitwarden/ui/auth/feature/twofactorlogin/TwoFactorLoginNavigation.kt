package com.x8bit.bitwarden.ui.auth.feature.twofactorlogin

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val TWO_FACTOR_LOGIN_ROUTE = "two_factor_login"

/**
 * Navigate to the Two-Factor Login screen.
 */
fun NavController.navigateToTwoFactorLogin(navOptions: NavOptions? = null) {
    this.navigate(TWO_FACTOR_LOGIN_ROUTE, navOptions)
}

/**
 * Add the Two-Factor Login screen to the nav graph.
 */
fun NavGraphBuilder.twoFactorLoginDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions(
        route = TWO_FACTOR_LOGIN_ROUTE,
    ) {
        TwoFactorLoginScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}
