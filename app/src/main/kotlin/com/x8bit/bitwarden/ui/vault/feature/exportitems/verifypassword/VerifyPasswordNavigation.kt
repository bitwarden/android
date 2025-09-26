@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.vault.feature.exportitems.verifypassword

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.base.util.composableWithPushTransitions
import com.bitwarden.ui.platform.util.ParcelableRouteSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the verify password screen.
 */
@Parcelize
@Serializable(with = VerifyPasswordRoute.Serializer::class)
@OmitFromCoverage
data class VerifyPasswordRoute(
    val userId: String,
) : Parcelable {

    /**
     * Custom serializer to support polymorphic routes.
     */
    class Serializer : ParcelableRouteSerializer<VerifyPasswordRoute>(
        kClass = VerifyPasswordRoute::class,
    )
}

/**
 * Class to retrieve verify password arguments from the [SavedStateHandle].
 */
@OmitFromCoverage
data class VerifyPasswordArgs(
    val userId: String,
)

/**
 * Constructs a [VerifyPasswordArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toVerifyPasswordArgs(): VerifyPasswordArgs {
    val route = this.toRoute<VerifyPasswordRoute>()
    return VerifyPasswordArgs(
        userId = route.userId,
    )
}

/**
 * Add the [VerifyPasswordScreen] to the nav graph.
 */
fun NavGraphBuilder.verifyPasswordDestination(
    onNavigateBack: () -> Unit,
    onPasswordVerified: (userId: String) -> Unit,
) {
    composableWithPushTransitions<VerifyPasswordRoute> {
        VerifyPasswordScreen(
            onNavigateBack = onNavigateBack,
            onPasswordVerified = onPasswordVerified,
        )
    }
}

/**
 * Navigate to the [VerifyPasswordScreen].
 */
fun NavController.navigateToVerifyPassword(
    userId: String,
    navOptions: NavOptions? = null,
) {
    navigate(
        route = VerifyPasswordRoute(userId = userId),
        navOptions = navOptions,
    )
}
