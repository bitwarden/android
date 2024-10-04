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
 * Route constants for [SetupUnlockScreen]
 */
private const val SETUP_UNLOCK_PREFIX = "setup_unlock"
private const val SETUP_UNLOCK_AS_ROOT_PREFIX = "${SETUP_UNLOCK_PREFIX}_as_root"
private const val SETUP_UNLOCK_INITIAL_SETUP_ARG = "isInitialSetup"
const val SETUP_UNLOCK_AS_ROOT_ROUTE = "$SETUP_UNLOCK_AS_ROOT_PREFIX/" +
    "{$SETUP_UNLOCK_INITIAL_SETUP_ARG}"
private const val SETUP_UNLOCK_ROUTE = "$SETUP_UNLOCK_PREFIX/{$SETUP_UNLOCK_INITIAL_SETUP_ARG}"

/**
 * Class to retrieve setup unlock arguments from the [SavedStateHandle].
 */
@OmitFromCoverage
data class SetupUnlockArgs(
    val isInitialSetup: Boolean,
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        isInitialSetup = requireNotNull(savedStateHandle[SETUP_UNLOCK_INITIAL_SETUP_ARG]),
    )
}

/**
 * Navigate to the setup unlock screen.
 */
fun NavController.navigateToSetupUnlockScreen(navOptions: NavOptions? = null) {
    this.navigate("$SETUP_UNLOCK_PREFIX/false", navOptions)
}

/**
 * Navigate to the setup unlock screen as root.
 */
fun NavController.navigateToSetupUnlockScreenAsRoot(navOptions: NavOptions? = null) {
    this.navigate("$SETUP_UNLOCK_AS_ROOT_PREFIX/true", navOptions)
}

/**
 * Add the setup unlock screen to a nav graph.
 */
fun NavGraphBuilder.setupUnlockDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions(
        route = SETUP_UNLOCK_ROUTE,
        arguments = setupUnlockArguments,
    ) {
        SetupUnlockScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Add the setup unlock screen to the root nav graph.
 */
fun NavGraphBuilder.setupUnlockDestinationAsRoot() {
    composableWithPushTransitions(
        route = SETUP_UNLOCK_AS_ROOT_ROUTE,
        arguments = setupUnlockArguments,
    ) {
        SetupUnlockScreen(
            onNavigateBack = {
                // No-Op
            },
        )
    }
}

private val setupUnlockArguments = listOf(
    navArgument(
        name = SETUP_UNLOCK_INITIAL_SETUP_ARG,
        builder = {
            type = NavType.BoolType
        },
    ),
)
