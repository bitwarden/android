package com.bitwarden.authenticator.ui.platform.feature.tutorial

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import com.bitwarden.ui.platform.base.util.composableWithStayTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the tutorial screen.
 */
@Serializable
data object TutorialRoute

/**
 * The type-safe route for the settings tutorial screen.
 */
@Serializable
data object SettingsTutorialRoute

/**
 * Add the top level Tutorial screen to the nav graph.
 */
fun NavGraphBuilder.tutorialDestination(onTutorialFinished: () -> Unit) {
    composableWithStayTransitions<TutorialRoute> {
        TutorialScreen(
            onTutorialFinished = onTutorialFinished,
        )
    }
}

/**
 * Add the Settings Tutorial screen to the nav graph.
 */
fun NavGraphBuilder.tutorialSettingsDestination(onTutorialFinished: () -> Unit) {
    composableWithSlideTransitions<SettingsTutorialRoute> {
        TutorialScreen(
            onTutorialFinished = onTutorialFinished,
        )
    }
}

/**
 * Navigate to the top level Tutorial screen.
 */
fun NavController.navigateToTutorial(navOptions: NavOptions? = null) {
    navigate(route = TutorialRoute, navOptions = navOptions)
}

/**
 * Navigate to the Tutorial screen within Settings.
 */
fun NavController.navigateToSettingsTutorial(navOptions: NavOptions? = null) {
    navigate(route = SettingsTutorialRoute, navOptions = navOptions)
}
