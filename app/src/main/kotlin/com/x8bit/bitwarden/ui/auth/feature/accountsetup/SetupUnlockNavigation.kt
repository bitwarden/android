package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithPushTransitions
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import com.x8bit.bitwarden.ui.platform.util.toObjectRoute
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the setup unlock screen.
 */
sealed class SetupUnlockRoute {
    /**
     * The [isInitialSetup] value used in the setup unlock screen.
     */
    abstract val isInitialSetup: Boolean

    /**
     * The type-safe route for the standard setup unlock screen.
     */
    @Serializable
    data object Standard : SetupUnlockRoute() {
        override val isInitialSetup: Boolean get() = false
    }

    /**
     * The type-safe route for the root setup unlock screen.
     */
    @Serializable
    data object AsRoot : SetupUnlockRoute() {
        override val isInitialSetup: Boolean get() = true
    }
}

/**
 * Class to retrieve setup unlock arguments from the [SavedStateHandle].
 */
data class SetupUnlockArgs(
    val isInitialSetup: Boolean,
)

/**
 * Constructs a [SetupUnlockArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toSetupUnlockArgs(): SetupUnlockArgs {
    val route = this.toObjectRoute<SetupUnlockRoute.AsRoot>()
        ?: this.toObjectRoute<SetupUnlockRoute.Standard>()
    return route
        ?.let { SetupUnlockArgs(isInitialSetup = it.isInitialSetup) }
        ?: throw IllegalStateException("Missing correct route for SetupUnlockScreen")
}

/**
 * Navigate to the setup unlock screen.
 */
fun NavController.navigateToSetupUnlockScreen(navOptions: NavOptions? = null) {
    this.navigate(route = SetupUnlockRoute.Standard, navOptions = navOptions)
}

/**
 * Navigate to the setup unlock screen as root.
 */
fun NavController.navigateToSetupUnlockScreenAsRoot(navOptions: NavOptions? = null) {
    this.navigate(route = SetupUnlockRoute.AsRoot, navOptions = navOptions)
}

/**
 * Add the setup unlock screen to a nav graph.
 */
fun NavGraphBuilder.setupUnlockDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions<SetupUnlockRoute.Standard> {
        SetupUnlockScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Add the setup unlock screen to the root nav graph.
 */
fun NavGraphBuilder.setupUnlockDestinationAsRoot() {
    composableWithPushTransitions<SetupUnlockRoute.AsRoot> {
        SetupUnlockScreen(
            onNavigateBack = {
                // No-Op
            },
        )
    }
}
