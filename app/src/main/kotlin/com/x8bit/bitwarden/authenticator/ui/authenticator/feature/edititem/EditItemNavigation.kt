package com.x8bit.bitwarden.authenticator.ui.authenticator.feature.edititem

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.composableWithPushTransitions

private const val EDIT_ITEM_PREFIX = "edit_item"
private const val EDIT_ITEM_ITEM_ID = "item_id"
private const val EDIT_ITEM_ROUTE = "$EDIT_ITEM_PREFIX/{$EDIT_ITEM_ITEM_ID}"

/**
 * Class to retrieve authenticator item arguments from the [SavedStateHandle].
 *
 * @property itemId ID of the item to be edited.
 */
data class EditItemArgs(val itemId: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[EDIT_ITEM_ITEM_ID]) as String,
    )
}

/**
 * Add the edit item screen to the nav graph.
 */
fun NavGraphBuilder.editItemDestination(
    onNavigateBack: () -> Unit = { },
) {
    composableWithPushTransitions(
        route = EDIT_ITEM_ROUTE,
        arguments = listOf(
            navArgument(EDIT_ITEM_ITEM_ID) { type = NavType.StringType },
        ),
    ) {
        EditItemScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the edit item screen.
 */
fun NavController.navigateToEditItem(
    itemId: String,
    navOptions: NavOptions? = null,
) {
    navigate(
        route = "$EDIT_ITEM_PREFIX/$itemId",
        navOptions = navOptions,
    )
}
