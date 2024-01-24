package com.x8bit.bitwarden.ui.vault.feature.attachments

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions

private const val ATTACHMENTS_CIPHER_ID = "cipher_id"
private const val ATTACHMENTS_ROUTE_PREFIX = "attachments"
private const val ATTACHMENTS_ROUTE = "$ATTACHMENTS_ROUTE_PREFIX/{$ATTACHMENTS_CIPHER_ID}"

/**
 * Class to retrieve arguments from the [SavedStateHandle].
 */
@OmitFromCoverage
data class AttachmentsArgs(val cipherId: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        cipherId = checkNotNull(savedStateHandle.get<String>(ATTACHMENTS_CIPHER_ID)),
    )
}

/**
 * Add the attachments screen to the nav graph.
 */
fun NavGraphBuilder.attachmentDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions(
        route = ATTACHMENTS_ROUTE,
        arguments = listOf(
            navArgument(ATTACHMENTS_CIPHER_ID) { type = NavType.StringType },
        ),
    ) {
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
        route = "$ATTACHMENTS_ROUTE_PREFIX/$cipherId",
        navOptions = navOptions,
    )
}
