package com.bitwarden.authenticator.ui.platform.feature.tutorial

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val TUTORIAL_ROUTE = "tutorial"
const val SETTINGS_TUTORIAL_ROUTE = "settings/tutorial"

/**
 * Add the top level Tutorial screen to the nav graph.
 */
fun NavGraphBuilder.tutorialDestination(onTutorialFinished: () -> Unit) {
    composable(TUTORIAL_ROUTE) {
        TutorialScreen(
            onTutorialFinished = onTutorialFinished,
        )
    }
}

/**
 * Add the Settings Tutorial screen to the nav graph.
 */
fun NavGraphBuilder.tutorialSettingsDestination(onTutorialFinished: () -> Unit) {
    composable(SETTINGS_TUTORIAL_ROUTE) {
        TutorialScreen(
            onTutorialFinished = onTutorialFinished,
        )
    }
}

/**
 * Navigate to the top level Tutorial screen.
 */
fun NavController.navigateToTutorial(navOptions: NavOptions? = null) {
    navigate(route = TUTORIAL_ROUTE, navOptions = navOptions)
}

/**
 * Navigate to the Tutorial screen within Settings.
 */
fun NavController.navigateToSettingsTutorial(navOptions: NavOptions? = null) {
    navigate(route = SETTINGS_TUTORIAL_ROUTE, navOptions)
}
