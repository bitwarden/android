package com.x8bit.bitwarden.ui.platform.feature.settings.collections.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import com.x8bit.bitwarden.ui.platform.feature.settings.collections.model.CollectionAddEditType
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the collection add & edit screen.
 */
@Serializable
data class CollectionAddEditRoute(
    val actionType: CollectionActionType,
    val collectionId: String?,
    val organizationId: String,
)

/**
 * Represents the action being done with a collection.
 */
@Serializable
enum class CollectionActionType {
    ADD,
    EDIT,
}

/**
 * Class to retrieve collection add & edit arguments from the [SavedStateHandle].
 */
data class CollectionAddEditArgs(
    val collectionAddEditType: CollectionAddEditType,
)

/**
 * Constructs a [CollectionAddEditArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toCollectionAddEditArgs(): CollectionAddEditArgs {
    val route = this.toRoute<CollectionAddEditRoute>()
    return CollectionAddEditArgs(
        collectionAddEditType = when (route.actionType) {
            CollectionActionType.ADD -> CollectionAddEditType.AddItem(
                organizationId = route.organizationId,
            )

            CollectionActionType.EDIT -> CollectionAddEditType.EditItem(
                collectionId = requireNotNull(route.collectionId),
                organizationId = route.organizationId,
            )
        },
    )
}

/**
 * Add the collection add & edit screen to the nav graph.
 */
fun NavGraphBuilder.collectionAddEditDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions<CollectionAddEditRoute> {
        CollectionAddEditScreen(onNavigateBack = onNavigateBack)
    }
}

/**
 * Navigate to the collection add & edit screen.
 */
fun NavController.navigateToCollectionAddEdit(
    collectionAddEditType: CollectionAddEditType,
    navOptions: NavOptions? = null,
) {
    this.navigate(
        route = CollectionAddEditRoute(
            actionType = collectionAddEditType.toCollectionActionType(),
            collectionId = collectionAddEditType.collectionId,
            organizationId = collectionAddEditType.organizationId,
        ),
        navOptions = navOptions,
    )
}

private fun CollectionAddEditType.toCollectionActionType(): CollectionActionType =
    when (this) {
        is CollectionAddEditType.AddItem -> CollectionActionType.ADD
        is CollectionAddEditType.EditItem -> CollectionActionType.EDIT
    }
