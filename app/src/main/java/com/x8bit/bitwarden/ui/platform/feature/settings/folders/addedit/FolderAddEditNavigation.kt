package com.x8bit.bitwarden.ui.platform.feature.settings.folders.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.model.FolderAddEditType
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the login approval screen.
 */
@Serializable
data class FolderAddEditRoute(
    val actionType: FolderActionType,
    val folderId: String?,
    val parentFolderName: String?,
)

/**
 * Represents the action being done with a folder.
 */
@Serializable
enum class FolderActionType {
    ADD,
    EDIT,
}

/**
 * Class to retrieve folder add & edit arguments from the [SavedStateHandle].
 */
data class FolderAddEditArgs(
    val folderAddEditType: FolderAddEditType,
    val parentFolderName: String?,
)

/**
 * Constructs a [FolderAddEditArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toFolderAddEditArgs(): FolderAddEditArgs {
    val route = this.toRoute<FolderAddEditRoute>()
    return FolderAddEditArgs(
        folderAddEditType = when (route.actionType) {
            FolderActionType.ADD -> FolderAddEditType.AddItem
            FolderActionType.EDIT -> FolderAddEditType.EditItem(requireNotNull(route.folderId))
        },
        parentFolderName = route.parentFolderName,
    )
}

/**
 * Add the folder add & edit screen to the nav graph.
 */
fun NavGraphBuilder.folderAddEditDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions<FolderAddEditRoute> {
        FolderAddEditScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the folder add & edit screen.
 */
fun NavController.navigateToFolderAddEdit(
    folderAddEditType: FolderAddEditType,
    parentFolderName: String? = null,
    navOptions: NavOptions? = null,
) {
    this.navigate(
        route = FolderAddEditRoute(
            actionType = folderAddEditType.toFolderActionType(),
            folderId = folderAddEditType.toIdOrNull(),
            parentFolderName = parentFolderName,
        ),
        navOptions = navOptions,
    )
}

private fun FolderAddEditType.toFolderActionType(): FolderActionType =
    when (this) {
        is FolderAddEditType.AddItem -> FolderActionType.ADD
        is FolderAddEditType.EditItem -> FolderActionType.EDIT
    }

private fun FolderAddEditType.toIdOrNull(): String? =
    when (this) {
        is FolderAddEditType.AddItem -> null
        is FolderAddEditType.EditItem -> folderId
    }
