package com.x8bit.bitwarden.authenticator.ui.platform.feature.tutorial

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

const val TUTORIAL_ROUTE = "tutorial"

fun NavGraphBuilder.tutorialDestination(onTutorialFinished: () -> Unit) {
    composable(TUTORIAL_ROUTE) {
        TutorialScreen(
            onTutorialFinished = onTutorialFinished,
        )
    }
}

fun NavController.navigateToTutorial(navOptions: NavOptions? = null) {
    navigate(route = TUTORIAL_ROUTE, navOptions = navOptions)
}
