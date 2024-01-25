package com.x8bit.bitwarden.ui.auth.feature.auth

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.navOptions
import androidx.navigation.navigation
import com.x8bit.bitwarden.ui.auth.feature.createaccount.createAccountDestination
import com.x8bit.bitwarden.ui.auth.feature.createaccount.navigateToCreateAccount
import com.x8bit.bitwarden.ui.auth.feature.enterprisesignon.enterpriseSignOnDestination
import com.x8bit.bitwarden.ui.auth.feature.enterprisesignon.navigateToEnterpriseSignOn
import com.x8bit.bitwarden.ui.auth.feature.environment.environmentDestination
import com.x8bit.bitwarden.ui.auth.feature.environment.navigateToEnvironment
import com.x8bit.bitwarden.ui.auth.feature.landing.LANDING_ROUTE
import com.x8bit.bitwarden.ui.auth.feature.landing.landingDestination
import com.x8bit.bitwarden.ui.auth.feature.login.loginDestination
import com.x8bit.bitwarden.ui.auth.feature.login.navigateToLogin
import com.x8bit.bitwarden.ui.auth.feature.loginwithdevice.loginWithDeviceDestination
import com.x8bit.bitwarden.ui.auth.feature.loginwithdevice.navigateToLoginWithDevice
import com.x8bit.bitwarden.ui.auth.feature.masterpasswordhint.masterPasswordHintDestination
import com.x8bit.bitwarden.ui.auth.feature.masterpasswordhint.navigateToMasterPasswordHint
import com.x8bit.bitwarden.ui.auth.feature.twofactorlogin.navigateToTwoFactorLogin
import com.x8bit.bitwarden.ui.auth.feature.twofactorlogin.twoFactorLoginDestination

const val AUTH_GRAPH_ROUTE: String = "auth_graph"

/**
 * Add auth destinations to the nav graph.
 */
@Suppress("LongMethod")
fun NavGraphBuilder.authGraph(navController: NavHostController) {
    navigation(
        startDestination = LANDING_ROUTE,
        route = AUTH_GRAPH_ROUTE,
    ) {
        createAccountDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToLogin = { emailAddress, captchaToken ->
                navController.navigateToLogin(
                    emailAddress = emailAddress,
                    captchaToken = captchaToken,
                    navOptions = navOptions {
                        popUpTo(LANDING_ROUTE)
                    },
                )
            },
        )
        enterpriseSignOnDestination(
            onNavigateBack = { navController.popBackStack() },
        )
        landingDestination(
            onNavigateToCreateAccount = { navController.navigateToCreateAccount() },
            onNavigateToLogin = { emailAddress ->
                navController.navigateToLogin(
                    emailAddress = emailAddress,
                    captchaToken = null,
                )
            },
            onNavigateToEnvironment = {
                navController.navigateToEnvironment()
            },
        )
        loginDestination(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToMasterPasswordHint = { emailAddress ->
                navController.navigateToMasterPasswordHint(
                    emailAddress = emailAddress,
                )
            },
            onNavigateToEnterpriseSignOn = { navController.navigateToEnterpriseSignOn() },
            onNavigateToLoginWithDevice = { emailAddress ->
                navController.navigateToLoginWithDevice(
                    emailAddress = emailAddress,
                )
            },
            onNavigateToTwoFactorLogin = { navController.navigateToTwoFactorLogin() },
        )
        loginWithDeviceDestination(
            onNavigateBack = { navController.popBackStack() },
        )
        environmentDestination(
            onNavigateBack = { navController.popBackStack() },
        )
        masterPasswordHintDestination(
            onNavigateBack = { navController.popBackStack() },
        )
        twoFactorLoginDestination(
            onNavigateBack = { navController.popBackStack() },
        )
    }
}

/**
 * Navigate to the auth screen. Note this will only work if auth destination was added
 * via [authGraph].
 */
fun NavController.navigateToAuthGraph(
    navOptions: NavOptions? = null,
) {
    navigate(AUTH_GRAPH_ROUTE, navOptions)
}
