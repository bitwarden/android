package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithPushTransitions
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import com.x8bit.bitwarden.ui.platform.util.toObjectRoute
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the setup autofill screen.
 */
sealed class SetupAutofillRoute {
    /**
     * The [isInitialSetup] value used in the setup autofill screen.
     */
    abstract val isInitialSetup: Boolean

    /**
     * The type-safe route for the standard setup autofill screen.
     */
    @Serializable
    data object Standard : SetupAutofillRoute() {
        override val isInitialSetup: Boolean get() = false
    }

    /**
     * The type-safe route for the root setup autofill screen.
     */
    @Serializable
    data object AsRoot : SetupAutofillRoute() {
        override val isInitialSetup: Boolean get() = true
    }
}

/**
 * Arguments for the [SetupAutoFillScreen] using [SavedStateHandle].
 */
data class SetupAutoFillScreenArgs(val isInitialSetup: Boolean)

/**
 * Constructs a [SetupAutoFillScreenArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toSetupAutoFillArgs(): SetupAutoFillScreenArgs {
    val route = (this.toObjectRoute<SetupAutofillRoute.AsRoot>()
        ?: this.toObjectRoute<SetupAutofillRoute.Standard>())
    return route
        ?.let { SetupAutoFillScreenArgs(isInitialSetup = it.isInitialSetup) }
        ?: throw IllegalStateException("Missing correct route for SetupAutofillScreen")
}

/**
 * Navigate to the setup autofill screen.
 */
fun NavController.navigateToSetupAutoFillScreen(navOptions: NavOptions? = null) {
    this.navigate(route = SetupAutofillRoute.Standard, navOptions = navOptions)
}

/**
 * Navigate to the setup autofill screen as the root.
 */
fun NavController.navigateToSetupAutoFillAsRootScreen(navOptions: NavOptions? = null) {
    this.navigate(route = SetupAutofillRoute.AsRoot, navOptions = navOptions)
}

/**
 * Add the setup autofill screen to the nav graph.
 */
fun NavGraphBuilder.setupAutoFillDestination(onNavigateBack: () -> Unit) {
    composableWithSlideTransitions<SetupAutofillRoute.Standard> {
        SetupAutoFillScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Add the setup autofill screen to the root nav graph.
 */
fun NavGraphBuilder.setupAutoFillDestinationAsRoot() {
    composableWithPushTransitions<SetupAutofillRoute.AsRoot> {
        SetupAutoFillScreen(
            onNavigateBack = {
                // No-Op
            },
        )
    }
}
