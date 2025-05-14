package com.x8bit.bitwarden.ui.tools.feature.send.addsend

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.model.AddSendType
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the add send screen.
 */
@Serializable
data class AddSendRoute(
    val type: ModeType,
    val editSendId: String?,
)

/**
 * Indicates the mode of send to be displayed.
 */
@Serializable
enum class ModeType {
    ADD,
    EDIT,
}

/**
 * Class to retrieve send add & edit arguments from the [SavedStateHandle].
 */
data class AddSendArgs(
    val sendAddType: AddSendType,
)

/**
 * Constructs a [AddSendArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toAddSendArgs(): AddSendArgs {
    val route = this.toRoute<AddSendRoute>()
    return AddSendArgs(
        sendAddType = when (route.type) {
            ModeType.ADD -> AddSendType.AddItem
            ModeType.EDIT -> AddSendType.EditItem(sendItemId = requireNotNull(route.editSendId))
        },
    )
}

private fun SavedStateHandle.toAddSendType(): AddSendType {
    val route = this.toRoute<AddSendRoute>()
    return when (route.type) {
        ModeType.ADD -> AddSendType.AddItem
        ModeType.EDIT -> AddSendType.EditItem(sendItemId = requireNotNull(route.editSendId))
    }
}

/**
 * Add the new send screen to the nav graph.
 */
fun NavGraphBuilder.addSendDestination(
    onNavigateBack: () -> Unit,
    onNavigateUpToRoot: () -> Unit,
) {
    composableWithSlideTransitions<AddSendRoute> {
        AddSendScreen(
            onNavigateBack = onNavigateBack,
            onNavigateUpToRoot = onNavigateUpToRoot,
        )
    }
}

/**
 * Navigate to the new send screen.
 */
fun NavController.navigateToAddSend(
    sendAddType: AddSendType,
    navOptions: NavOptions? = null,
) {
    this.navigate(
        route = AddSendRoute(
            type = when (sendAddType) {
                AddSendType.AddItem -> ModeType.ADD
                is AddSendType.EditItem -> ModeType.EDIT
            },
            editSendId = (sendAddType as? AddSendType.EditItem)?.sendItemId,
        ),
        navOptions = navOptions,
    )
}
