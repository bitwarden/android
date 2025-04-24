@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.platform.feature.settings.other

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.bitwarden.core.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.base.util.composableWithPushTransitions

private const val IS_PRE_AUTH: String = "isPreAuth"
private const val PRE_AUTH_OTHER_ROUTE = "pre_auth_settings_other"
private const val OTHER_ROUTE = "settings_other"

/**
 * Class to retrieve other settings arguments from the [SavedStateHandle].
 */
data class OtherArgs(val isPreAuth: Boolean) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        isPreAuth = requireNotNull(savedStateHandle[IS_PRE_AUTH]),
    )
}

/**
 * Add settings destinations to the nav graph.
 */
fun NavGraphBuilder.otherDestination(
    isPreAuth: Boolean,
    onNavigateBack: () -> Unit,
) {
    composableWithPushTransitions(
        route = getRoute(isPreAuth = isPreAuth),
        arguments = listOf(
            navArgument(name = IS_PRE_AUTH) {
                type = NavType.BoolType
                defaultValue = isPreAuth
            },
        ),
    ) {
        OtherScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the about screen.
 */
fun NavController.navigateToOther(
    isPreAuth: Boolean,
    navOptions: NavOptions? = null,
) {
    navigate(route = getRoute(isPreAuth = isPreAuth), navOptions = navOptions)
}

private fun getRoute(
    isPreAuth: Boolean,
): String = if (isPreAuth) PRE_AUTH_OTHER_ROUTE else OTHER_ROUTE
