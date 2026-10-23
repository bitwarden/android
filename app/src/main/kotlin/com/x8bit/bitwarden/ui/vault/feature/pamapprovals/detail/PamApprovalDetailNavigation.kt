package com.x8bit.bitwarden.ui.vault.feature.pamapprovals.detail

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.ui.platform.base.util.composableWithPushTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the PAM access request detail screen.
 */
@Serializable
data class PamApprovalDetailRoute(val requestId: String)

/**
 * Class to retrieve the access request detail arguments from the [SavedStateHandle].
 */
data class PamApprovalDetailArgs(val requestId: String)

/**
 * Constructs a [PamApprovalDetailArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toPamApprovalDetailArgs(): PamApprovalDetailArgs {
    val route = this.toRoute<PamApprovalDetailRoute>()
    return PamApprovalDetailArgs(requestId = route.requestId)
}

/**
 * Add the PAM access request detail destination to the nav graph.
 */
fun NavGraphBuilder.pamApprovalDetailDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithPushTransitions<PamApprovalDetailRoute> {
        PamApprovalDetailScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the detail of the access request with the given [requestId].
 */
fun NavController.navigateToPamApprovalDetail(
    requestId: String,
    navOptions: NavOptions? = null,
) {
    this.navigate(route = PamApprovalDetailRoute(requestId = requestId), navOptions = navOptions)
}
