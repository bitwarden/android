package com.bitwarden.authenticator.ui.platform.feature.tutorial

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val TUTORIAL_ROUTE = "tutorial"
const val SETTINGS_TUTORIAL_ROUTE = "settings/tutorial"

fun NavGraphBuilder.tutorialDestination(onTutorialFinished: () -> Unit) {
    composable(TUTORIAL_ROUTE) {
        TutorialScreen(
            onTutorialFinished = onTutorialFinished,
        )
    }
}

fun NavGraphBuilder.tutorialSettingsDestination(onTutorialFinished: () -> Unit) {
    composable(SETTINGS_TUTORIAL_ROUTE) {
        TutorialScreen(
            onTutorialFinished = onTutorialFinished,
        )
    }
}

fun NavController.navigateToTutorial(navOptions: NavOptions? = null) {
    navigate(route = TUTORIAL_ROUTE, navOptions = navOptions)
}

fun NavController.navigateToSettingsTutorial(navOptions: NavOptions? = null) {
    navigate(route = SETTINGS_TUTORIAL_ROUTE, navOptions)
}
