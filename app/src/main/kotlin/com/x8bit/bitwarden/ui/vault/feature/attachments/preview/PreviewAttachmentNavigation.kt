package com.x8bit.bitwarden.ui.vault.feature.attachments.preview

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the preview attachment screen.
 */
@Serializable
data class PreviewAttachmentRoute(
    val cipherId: String,
    val attachmentId: String,
    val fileName: String,
)

/**
 * Class to retrieve arguments from the [SavedStateHandle].
 */
data class PreviewAttachmentArgs(
    val cipherId: String,
    val attachmentId: String,
    val fileName: String,
)

/**
 * Constructs a [PreviewAttachmentArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toPreviewAttachmentArgs(): PreviewAttachmentArgs {
    val route = this.toRoute<PreviewAttachmentRoute>()
    return PreviewAttachmentArgs(
        cipherId = route.cipherId,
        attachmentId = route.attachmentId,
        fileName = route.fileName,
    )
}

/**
 * Add the preview attachment screen to the nav graph.
 */
fun NavGraphBuilder.previewAttachmentDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions<PreviewAttachmentRoute> {
        PreviewAttachmentScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the preview attachment screen.
 */
fun NavController.navigateToPreviewAttachment(
    cipherId: String,
    attachmentId: String,
    fileName: String,
    navOptions: NavOptions? = null,
) {
    navigate(
        route = PreviewAttachmentRoute(
            cipherId = cipherId,
            attachmentId = attachmentId,
            fileName = fileName,
        ),
        navOptions = navOptions,
    )
}
