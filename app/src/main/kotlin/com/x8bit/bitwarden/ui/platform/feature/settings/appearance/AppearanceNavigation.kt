package com.x8bit.bitwarden.ui.platform.feature.settings.appearance

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithPushTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the settings appearance screen.
 */
@Serializable
sealed class SettingsAppearanceRoute {
    /**
     * The type-safe route for the settings appearance screen.
     */
    @Serializable
    data object Standard : SettingsAppearanceRoute()

    /**
     * The type-safe route for the pre-auth settings appearance screen.
     */
    @Serializable
    data object PreAuth : SettingsAppearanceRoute()
}

/**
 * Add settings destinations to the nav graph.
 */
fun NavGraphBuilder.appearanceDestination(
    isPreAuth: Boolean,
    onNavigateBack: () -> Unit,
) {
    if (isPreAuth) {
        composableWithPushTransitions<SettingsAppearanceRoute.PreAuth> {
            AppearanceScreen(onNavigateBack = onNavigateBack)
        }
    } else {
        composableWithPushTransitions<SettingsAppearanceRoute.Standard> {
            AppearanceScreen(onNavigateBack = onNavigateBack)
        }
    }
}

/**
 * Navigate to the appearance screen.
 */
fun NavController.navigateToAppearance(
    isPreAuth: Boolean,
    navOptions: NavOptions? = null,
) {
    this.navigate(
        route = if (isPreAuth) {
            SettingsAppearanceRoute.PreAuth
        } else {
            SettingsAppearanceRoute.Standard
        },
        navOptions = navOptions,
    )
}
