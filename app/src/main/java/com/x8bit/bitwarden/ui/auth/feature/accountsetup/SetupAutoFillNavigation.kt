package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.base.util.composableWithPushTransitions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

/**
 * Route constant for navigating to the [SetupAutoFillScreen].
 */
private const val SETUP_AUTO_FILL_PREFIX = "setup_auto_fill"
private const val SETUP_AUTO_FILL_AS_ROOT_PREFIX = "${SETUP_AUTO_FILL_PREFIX}_as_root"
private const val SETUP_AUTO_FILL_NAV_ARG = "isInitialSetup"
private const val SETUP_AUTO_FILL_ROUTE = "$SETUP_AUTO_FILL_PREFIX/{$SETUP_AUTO_FILL_NAV_ARG}"
const val SETUP_AUTO_FILL_AS_ROOT_ROUTE =
    "$SETUP_AUTO_FILL_AS_ROOT_PREFIX/{$SETUP_AUTO_FILL_NAV_ARG}"

/**
 * Arguments for the [SetupAutoFillScreen] using [SavedStateHandle].
 */
@OmitFromCoverage
data class SetupAutoFillScreenArgs(val isInitialSetup: Boolean) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        isInitialSetup = requireNotNull(savedStateHandle[SETUP_AUTO_FILL_NAV_ARG]),
    )
}

/**
 * Navigate to the setup auto-fill screen.
 */
fun NavController.navigateToSetupAutoFillScreen(navOptions: NavOptions? = null) {
    this.navigate("$SETUP_AUTO_FILL_PREFIX/false", navOptions)
}

/**
 * Navigate to the setup auto-fill screen as the root.
 */
fun NavController.navigateToSetupAutoFillAsRootScreen(navOptions: NavOptions? = null) {
    this.navigate("$SETUP_AUTO_FILL_AS_ROOT_PREFIX/true", navOptions)
}

/**
 * Add the setup auto-fil screen to the nav graph.
 */
fun NavGraphBuilder.setupAutoFillDestination(onNavigateBack: () -> Unit) {
    composableWithSlideTransitions(
        route = SETUP_AUTO_FILL_ROUTE,
        arguments = setupAutofillNavArgs,
    ) {
        SetupAutoFillScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Add the setup autofil screen to the root nav graph.
 */
fun NavGraphBuilder.setupAutoFillDestinationAsRoot() {
    composableWithPushTransitions(
        route = SETUP_AUTO_FILL_AS_ROOT_ROUTE,
        arguments = setupAutofillNavArgs,
    ) {
        SetupAutoFillScreen(
            onNavigateBack = {
                // No-Op
            },
        )
    }
}

private val setupAutofillNavArgs = listOf(
    navArgument(SETUP_AUTO_FILL_NAV_ARG) {
        type = NavType.BoolType
    },
)
