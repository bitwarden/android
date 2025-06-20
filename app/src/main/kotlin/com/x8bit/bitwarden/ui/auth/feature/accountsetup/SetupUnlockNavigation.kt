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
 * The type-safe route for the setup unlock screen.
 */
@Parcelize
@Serializable(with = SetupUnlockRoute.Serializer::class)
sealed class SetupUnlockRoute : Parcelable {
    /**
     * The [isInitialSetup] value used in the setup unlock screen.
     */
    abstract val isInitialSetup: Boolean

    /**
     * Custom serializer to support polymorphic routes.
     */
    class Serializer : ParcelableRouteSerializer<SetupUnlockRoute>(SetupUnlockRoute::class)

    /**
     * The type-safe route for the standard setup unlock screen.
     */
    @Parcelize
    @Serializable(with = Standard.Serializer::class)
    data object Standard : SetupUnlockRoute() {
        override val isInitialSetup: Boolean get() = false

        /**
         * Custom serializer to support polymorphic routes.
         */
        class Serializer : ParcelableRouteSerializer<Standard>(Standard::class)
    }

    /**
     * The type-safe route for the root setup unlock screen.
     */
    @Parcelize
    @Serializable(with = AsRoot.Serializer::class)
    data object AsRoot : SetupUnlockRoute() {
        override val isInitialSetup: Boolean get() = true

        /**
         * Custom serializer to support polymorphic routes.
         */
        class Serializer : ParcelableRouteSerializer<AsRoot>(AsRoot::class)
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
    val route = this.toRoute<SetupUnlockRoute>()
    return SetupUnlockArgs(isInitialSetup = route.isInitialSetup)
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
