package com.x8bit.bitwarden.ui.vault.feature.attachments

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the attachments screen.
 */
@Serializable
data class AttachmentsRoute(
    val cipherId: String,
)

/**
 * Class to retrieve arguments from the [SavedStateHandle].
 */
data class AttachmentsArgs(val cipherId: String)

/**
 * Constructs a [AttachmentsArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toAttachmentsArgs(): AttachmentsArgs {
    val route = this.toRoute<AttachmentsRoute>()
    return AttachmentsArgs(cipherId = route.cipherId)
}

/**
 * Add the attachments screen to the nav graph.
 */
fun NavGraphBuilder.attachmentDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions<AttachmentsRoute> {
        AttachmentsScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the attachments screen.
 */
fun NavController.navigateToAttachment(
    cipherId: String,
    navOptions: NavOptions? = null,
) {
    navigate(
        route = AttachmentsRoute(cipherId = cipherId),
        navOptions = navOptions,
    )
}
