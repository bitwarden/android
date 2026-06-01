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
 * The type-safe route for the setup autofill screen.
 */
@Parcelize
@Serializable(with = SetupAutofillRoute.Serializer::class)
sealed class SetupAutofillRoute : Parcelable {
    /**
     * The [isInitialSetup] value used in the setup autofill screen.
     */
    abstract val isInitialSetup: Boolean

    /**
     * Custom serializer to support polymorphic routes.
     */
    class Serializer : ParcelableRouteSerializer<SetupAutofillRoute>(SetupAutofillRoute::class)

    /**
     * The type-safe route for the standard setup autofill screen.
     */
    @Parcelize
    @Serializable(with = Standard.Serializer::class)
    data object Standard : SetupAutofillRoute() {
        override val isInitialSetup: Boolean get() = false

        /**
         * Custom serializer to support polymorphic routes.
         */
        class Serializer : ParcelableRouteSerializer<Standard>(Standard::class)
    }

    /**
     * The type-safe route for the root setup autofill screen.
     */
    @Parcelize
    @Serializable(with = AsRoot.Serializer::class)
    data object AsRoot : SetupAutofillRoute() {
        override val isInitialSetup: Boolean get() = true

        /**
         * Custom serializer to support polymorphic routes.
         */
        class Serializer : ParcelableRouteSerializer<AsRoot>(AsRoot::class)
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
    val route = this.toRoute<SetupAutofillRoute>()
    return SetupAutoFillScreenArgs(isInitialSetup = route.isInitialSetup)
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
fun NavGraphBuilder.setupAutoFillDestination(
    onNavigateBack: () -> Unit,
    onNavigateToBrowserAutofill: () -> Unit,
) {
    composableWithSlideTransitions<SetupAutofillRoute.Standard> {
        SetupAutoFillScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToBrowserAutofill = {
                onNavigateBack()
                onNavigateToBrowserAutofill()
            },
        )
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
            onNavigateToBrowserAutofill = {
                // No-Op
            },
        )
    }
}
