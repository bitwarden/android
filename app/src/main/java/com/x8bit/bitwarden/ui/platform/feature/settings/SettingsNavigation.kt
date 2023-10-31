package com.x8bit.bitwarden.ui.platform.feature.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.x8bit.bitwarden.ui.platform.feature.settings.about.aboutDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.about.navigateToAbout
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.accountSecurityDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.navigateToAccountSecurity
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.appearanceDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.navigateToAppearance
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.autoFillDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.navigateToAutoFill
import com.x8bit.bitwarden.ui.platform.feature.settings.other.navigateToOther
import com.x8bit.bitwarden.ui.platform.feature.settings.other.otherDestination
import com.x8bit.bitwarden.ui.platform.feature.settings.vault.navigateToVault
import com.x8bit.bitwarden.ui.platform.feature.settings.vault.vaultDestination
import com.x8bit.bitwarden.ui.platform.theme.TransitionProviders

const val SETTINGS_GRAPH_ROUTE: String = "settings_graph"
private const val SETTINGS_ROUTE: String = "settings"

/**
 * Add settings destinations to the nav graph.
 */
fun NavGraphBuilder.settingsGraph(
    navController: NavController,
) {
    navigation(
        startDestination = SETTINGS_ROUTE,
        route = SETTINGS_GRAPH_ROUTE,
    ) {
        composable(
            route = SETTINGS_ROUTE,
            enterTransition = TransitionProviders.Enter.stay,
            exitTransition = TransitionProviders.Exit.pushLeft,
            popEnterTransition = TransitionProviders.Enter.pushRight,
            popExitTransition = TransitionProviders.Exit.fadeOut,
        ) {
            SettingsScreen(
                onNavigateToAbout = { navController.navigateToAbout() },
                onNavigateToAccountSecurity = { navController.navigateToAccountSecurity() },
                onNavigateToAppearance = { navController.navigateToAppearance() },
                onNavigateToAutoFill = { navController.navigateToAutoFill() },
                onNavigateToOther = { navController.navigateToOther() },
                onNavigateToVault = { navController.navigateToVault() },
            )
        }
        aboutDestination(onNavigateBack = { navController.popBackStack() })
        accountSecurityDestination(onNavigateBack = { navController.popBackStack() })
        appearanceDestination(onNavigateBack = { navController.popBackStack() })
        autoFillDestination(onNavigateBack = { navController.popBackStack() })
        otherDestination(onNavigateBack = { navController.popBackStack() })
        vaultDestination(onNavigateBack = { navController.popBackStack() })
    }
}

/**
 * Navigate to the settings screen screen.
 */
fun NavController.navigateToSettingsGraph(navOptions: NavOptions? = null) {
    navigate(SETTINGS_GRAPH_ROUTE, navOptions)
}
