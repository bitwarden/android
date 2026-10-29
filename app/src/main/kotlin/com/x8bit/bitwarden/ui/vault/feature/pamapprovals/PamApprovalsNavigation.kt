package com.x8bit.bitwarden.ui.vault.feature.pamapprovals

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.bitwarden.ui.platform.base.util.composableWithPushTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the PAM access approvals screen.
 */
@Serializable
data object PamApprovalsRoute

/**
 * Add the PAM access approvals destination to the nav graph.
 */
fun NavGraphBuilder.pamApprovalsDestination(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (requestId: String) -> Unit,
) {
    composableWithPushTransitions<PamApprovalsRoute> {
        PamApprovalsScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToDetail = onNavigateToDetail,
        )
    }
}

/**
 * Navigate to the PAM access approvals screen.
 */
fun NavController.navigateToPamApprovals(navOptions: NavOptions? = null) {
    this.navigate(route = PamApprovalsRoute, navOptions = navOptions)
}
