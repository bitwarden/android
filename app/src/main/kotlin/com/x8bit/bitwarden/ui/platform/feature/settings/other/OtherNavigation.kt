package com.x8bit.bitwarden.ui.platform.feature.settings.other

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.ui.platform.base.util.composableWithPushTransitions
import com.bitwarden.ui.platform.util.ParcelableRouteSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the settings other screen.
 */
@Parcelize
@Serializable(with = SettingsOtherRoute.Serializer::class)
sealed class SettingsOtherRoute : Parcelable {
    /**
     * Indicates that the settings other screen should be shown as a pre-authentication.
     */
    abstract val isPreAuth: Boolean

    /**
     * Custom serializer to support polymorphic routes.
     */
    class Serializer : ParcelableRouteSerializer<SettingsOtherRoute>(SettingsOtherRoute::class)

    /**
     * The type-safe route for the settings other screen.
     */
    @Parcelize
    @Serializable(with = Standard.Serializer::class)
    data object Standard : SettingsOtherRoute() {
        override val isPreAuth: Boolean get() = false

        /**
         * Custom serializer to support polymorphic routes.
         */
        class Serializer : ParcelableRouteSerializer<Standard>(Standard::class)
    }

    /**
     * The type-safe route for the pre-auth settings other screen.
     */
    @Parcelize
    @Serializable(with = PreAuth.Serializer::class)
    data object PreAuth : SettingsOtherRoute() {
        override val isPreAuth: Boolean get() = true

        /**
         * Custom serializer to support polymorphic routes.
         */
        class Serializer : ParcelableRouteSerializer<PreAuth>(PreAuth::class)
    }
}

/**
 * Class to retrieve other settings arguments from the [SavedStateHandle].
 */
data class OtherArgs(val isPreAuth: Boolean)

/**
 * Constructs a [OtherArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toOtherArgs(): OtherArgs {
    val route = this.toRoute<SettingsOtherRoute>()
    return OtherArgs(isPreAuth = route.isPreAuth)
}

/**
 * Add settings destinations to the nav graph.
 */
fun NavGraphBuilder.otherDestination(
    isPreAuth: Boolean,
    onNavigateBack: () -> Unit,
) {
    if (isPreAuth) {
        composableWithPushTransitions<SettingsOtherRoute.PreAuth> {
            OtherScreen(onNavigateBack = onNavigateBack)
        }
    } else {
        composableWithPushTransitions<SettingsOtherRoute.Standard> {
            OtherScreen(onNavigateBack = onNavigateBack)
        }
    }
}

/**
 * Navigate to the about screen.
 */
fun NavController.navigateToOther(
    isPreAuth: Boolean,
    navOptions: NavOptions? = null,
) {
    this.navigate(
        route = if (isPreAuth) SettingsOtherRoute.PreAuth else SettingsOtherRoute.Standard,
        navOptions = navOptions,
    )
}
