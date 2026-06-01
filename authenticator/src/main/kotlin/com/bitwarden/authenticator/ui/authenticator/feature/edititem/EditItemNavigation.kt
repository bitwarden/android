package com.bitwarden.authenticator.ui.authenticator.feature.edititem

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the edit item screen.
 */
@Serializable
data class EditItemRoute(
    val itemId: String,
)

/**
 * Class to retrieve authenticator item arguments from the [SavedStateHandle].
 *
 * @property itemId ID of the item to be edited.
 */
data class EditItemArgs(val itemId: String)

/**
 * Constructs a [EditItemArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toEditItemArgs(): EditItemArgs {
    val route = this.toRoute<EditItemRoute>()
    return EditItemArgs(itemId = route.itemId)
}

/**
 * Add the edit item screen to the nav graph.
 */
fun NavGraphBuilder.editItemDestination(
    onNavigateBack: () -> Unit = { },
) {
    composableWithSlideTransitions<EditItemRoute> {
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
        route = EditItemRoute(itemId = itemId),
        navOptions = navOptions,
    )
}
