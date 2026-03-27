package com.x8bit.bitwarden.ui.vault.feature.media

import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the media viewer screen.
 *
 * @property cipherId The ID of the cipher owning the attachment.
 * @property attachmentId The ID of the attachment to preview.
 * @property fileName The original file name (used for title and media type).
 */
@Serializable
data class MediaViewerRoute(
    val cipherId: String,
    val attachmentId: String,
    val fileName: String,
)

/**
 * Class to retrieve media viewer arguments from the [SavedStateHandle].
 */
data class MediaViewerArgs(
    val cipherId: String,
    val attachmentId: String,
    val fileName: String,
)

/**
 * Constructs [MediaViewerArgs] from the [SavedStateHandle] and internal route.
 */
fun SavedStateHandle.toMediaViewerArgs(): MediaViewerArgs {
    val route = this.toRoute<MediaViewerRoute>()
    return MediaViewerArgs(
        cipherId = route.cipherId,
        attachmentId = route.attachmentId,
        fileName = route.fileName,
    )
}

/**
 * Add the media viewer screen to the nav graph.
 *
 * @param getSharedMediaViewModel A lambda that produces the NavGraph-scoped
 *   [VaultMediaViewerViewModel] so the fullscreen viewer shares
 *   the same instance as VaultItemScreen.
 */
fun NavGraphBuilder.mediaViewerDestination(
    getSharedMediaViewModel: @Composable () -> VaultMediaViewerViewModel,
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions<MediaViewerRoute> { navBackStackEntry ->
        val route = navBackStackEntry.toRoute<MediaViewerRoute>()
        MediaViewerScreen(
            viewModel = getSharedMediaViewModel(),
            cipherId = route.cipherId,
            attachmentId = route.attachmentId,
            fileName = route.fileName,
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the media viewer screen.
 */
fun NavController.navigateToMediaViewer(
    cipherId: String,
    attachmentId: String,
    fileName: String,
    navOptions: NavOptions? = null,
) {
    navigate(
        route = MediaViewerRoute(
            cipherId = cipherId,
            attachmentId = attachmentId,
            fileName = fileName,
        ),
        navOptions = navOptions,
    )
}
