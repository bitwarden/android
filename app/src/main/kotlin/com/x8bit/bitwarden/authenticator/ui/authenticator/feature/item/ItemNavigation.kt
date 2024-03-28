package com.x8bit.bitwarden.authenticator.ui.authenticator.feature.item

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.composableWithPushTransitions

private const val ITEM_PREFIX = "item"
private const val ITEM_ID = "item_id"
private const val ITEM_ROUTE = "$ITEM_PREFIX/{$ITEM_ID}"

/**
 * Class to retrieve authenticator item arguments from the [SavedStateHandle].
 *
 * @property itemId ID of the item to be retrieved.
 */
data class ItemArgs(val itemId: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ITEM_ID]) as String,
    )
}

/**
 * Add the item screen to the nav graph.
 */
fun NavGraphBuilder.itemDestination(
    onNavigateBack: () -> Unit = { },
    onNavigateToEditItem: (id: String) -> Unit = { },
) {
    composableWithPushTransitions(
        route = ITEM_ROUTE,
        arguments = listOf(
            navArgument(ITEM_ID) { type = NavType.StringType },
        ),
    ) {
        ItemScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToEditItem = onNavigateToEditItem,
        )
    }
}

/**
 * Navigate to the item screen.
 */
fun NavController.navigateToItem(
    itemId: String,
    navOptions: NavOptions? = null,
) {
    navigate(
        route = "$ITEM_PREFIX/$itemId",
        navOptions = navOptions,
    )
}
