package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.ui.platform.base.util.composableWithPushTransitions
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import com.bitwarden.ui.platform.util.ParcelableRouteSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the setup browser autofill screen.
 */
@Parcelize
@Serializable(with = SetupBrowserAutofillRoute.Serializer::class)
sealed class SetupBrowserAutofillRoute : Parcelable {
    /**
     * The [isInitialSetup] value used in the setup browser autofill screen.
     */
    abstract val isInitialSetup: Boolean

    /**
     * Custom serializer to support polymorphic routes.
     */
    class Serializer : ParcelableRouteSerializer<SetupBrowserAutofillRoute>(
        kClass = SetupBrowserAutofillRoute::class,
    )

    /**
     * The type-safe route for the standard setup browser autofill screen.
     */
    @Parcelize
    @Serializable(with = Standard.Serializer::class)
    data object Standard : SetupBrowserAutofillRoute() {
        override val isInitialSetup: Boolean get() = false

        /**
         * Custom serializer to support polymorphic routes.
         */
        class Serializer : ParcelableRouteSerializer<Standard>(Standard::class)
    }

    /**
     * The type-safe route for the root setup browser autofill screen.
     */
    @Parcelize
    @Serializable(with = AsRoot.Serializer::class)
    data object AsRoot : SetupBrowserAutofillRoute() {
        override val isInitialSetup: Boolean get() = true

        /**
         * Custom serializer to support polymorphic routes.
         */
        class Serializer : ParcelableRouteSerializer<AsRoot>(AsRoot::class)
    }
}

/**
 * Arguments for the [SetupBrowserAutofillScreen] using [SavedStateHandle].
 */
data class SetupBrowserAutofillScreenArgs(val isInitialSetup: Boolean)

/**
 * Constructs a [SetupAutoFillScreenArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toSetupBrowserAutofillArgs(): SetupBrowserAutofillScreenArgs {
    val route = this.toRoute<SetupBrowserAutofillRoute>()
    return SetupBrowserAutofillScreenArgs(isInitialSetup = route.isInitialSetup)
}

/**
 * Navigate to the setup browser autofill screen.
 */
fun NavController.navigateToSetupBrowserAutofillScreen(navOptions: NavOptions? = null) {
    this.navigate(route = SetupBrowserAutofillRoute.Standard, navOptions = navOptions)
}

/**
 * Navigate to the setup browser autofill screen as the root.
 */
fun NavController.navigateToSetupBrowserAutoFillAsRootScreen(navOptions: NavOptions? = null) {
    this.navigate(route = SetupBrowserAutofillRoute.AsRoot, navOptions = navOptions)
}

/**
 * Add the setup browser autofill screen to the nav graph.
 */
fun NavGraphBuilder.setupBrowserAutofillDestination(onNavigateBack: () -> Unit) {
    composableWithSlideTransitions<SetupBrowserAutofillRoute.Standard> {
        SetupBrowserAutofillScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Add the setup browser autofill screen to the nav graph as a root.
 */
fun NavGraphBuilder.setupBrowserAutofillDestinationAsRoot() {
    composableWithPushTransitions<SetupBrowserAutofillRoute.AsRoot> {
        SetupBrowserAutofillScreen(
            onNavigateBack = {
                // No-Op
            },
        )
    }
}
