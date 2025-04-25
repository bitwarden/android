@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.platform.feature.settings.folders.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.bitwarden.core.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.model.FolderAddEditType

private const val ADD_TYPE: String = "add"
private const val EDIT_TYPE: String = "edit"
private const val EDIT_ITEM_ID: String = "folder_edit_id"
private const val PARENT_FOLDER_NAME: String = "parent_folder_name"

private const val ADD_EDIT_ITEM_PREFIX: String = "folder_add_edit_item"
private const val ADD_EDIT_ITEM_TYPE: String = "folder_add_edit_type"

private const val ADD_EDIT_ITEM_ROUTE: String =
    "$ADD_EDIT_ITEM_PREFIX/{$ADD_EDIT_ITEM_TYPE}" +
        "?$EDIT_ITEM_ID={$EDIT_ITEM_ID}&$PARENT_FOLDER_NAME={$PARENT_FOLDER_NAME}"

/**
 * Class to retrieve folder add & edit arguments from the [SavedStateHandle].
 */
data class FolderAddEditArgs(
    val folderAddEditType: FolderAddEditType,
    val parentFolderName: String?,
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        folderAddEditType = when (requireNotNull(savedStateHandle[ADD_EDIT_ITEM_TYPE])) {
            ADD_TYPE -> FolderAddEditType.AddItem
            EDIT_TYPE -> FolderAddEditType.EditItem(requireNotNull(savedStateHandle[EDIT_ITEM_ID]))
            else -> throw IllegalStateException("Unknown FolderAddEditType.")
        },
        parentFolderName = savedStateHandle[PARENT_FOLDER_NAME],
    )
}

/**
 * Add the folder add & edit screen to the nav graph.
 */
fun NavGraphBuilder.folderAddEditDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions(
        route = ADD_EDIT_ITEM_ROUTE,
        arguments = listOf(
            navArgument(ADD_EDIT_ITEM_TYPE) { type = NavType.StringType },
            navArgument(EDIT_ITEM_ID) {
                nullable = true
                type = NavType.StringType
            },
            navArgument(PARENT_FOLDER_NAME) {
                nullable = true
                type = NavType.StringType
            },
        ),
    ) {
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
    navigate(
        route = "$ADD_EDIT_ITEM_PREFIX/${folderAddEditType.toTypeString()}" +
            "?$EDIT_ITEM_ID=${folderAddEditType.toIdOrNull()}" +
            "&$PARENT_FOLDER_NAME=$parentFolderName",
        navOptions = navOptions,
    )
}

private fun FolderAddEditType.toTypeString(): String =
    when (this) {
        is FolderAddEditType.AddItem -> ADD_TYPE
        is FolderAddEditType.EditItem -> EDIT_TYPE
    }

private fun FolderAddEditType.toIdOrNull(): String? =
    when (this) {
        is FolderAddEditType.AddItem -> null
        is FolderAddEditType.EditItem -> folderId
    }
