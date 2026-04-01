@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.platform.feature.premium.plan

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import com.bitwarden.ui.platform.util.ParcelableRouteSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the plan screen.
 */
@OmitFromCoverage
@Parcelize
@Serializable(with = PlanRoute.Serializer::class)
sealed class PlanRoute : Parcelable {

    /**
     * Custom serializer to support polymorphic routes.
     */
    class Serializer : ParcelableRouteSerializer<PlanRoute>(PlanRoute::class)

    /**
     * Standard destination — inside settingsGraph, bottom nav visible, back arrow.
     */
    @Parcelize
    @Serializable(with = Standard.Serializer::class)
    data object Standard : PlanRoute() {
        /**
         * Custom serializer to support polymorphic routes.
         */
        class Serializer : ParcelableRouteSerializer<Standard>(Standard::class)
    }

    /**
     * Modal destination — parent vaultUnlockedGraph level, bottom nav hidden, close icon.
     */
    @Parcelize
    @Serializable(with = Modal.Serializer::class)
    data object Modal : PlanRoute() {
        /**
         * Custom serializer to support polymorphic routes.
         */
        class Serializer : ParcelableRouteSerializer<Modal>(Modal::class)
    }
}

/**
 * Class to retrieve plan arguments from the [SavedStateHandle].
 */
@OmitFromCoverage
data class PlanArgs(val planMode: PlanMode)

/**
 * Constructs [PlanArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toPlanArgs(): PlanArgs {
    val route = this.toRoute<PlanRoute>()
    return PlanArgs(
        planMode = when (route) {
            is PlanRoute.Standard -> PlanMode.Standard
            is PlanRoute.Modal -> PlanMode.Modal
        },
    )
}

/**
 * Register at parent vaultUnlockedGraph level — bottom nav hidden.
 */
fun NavGraphBuilder.planModalDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions<PlanRoute.Modal> {
        PlanScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the plan screen (modal, at parent level).
 */
fun NavController.navigateToPlanModal(navOptions: NavOptions? = null) {
    navigate(route = PlanRoute.Modal, navOptions = navOptions)
}
