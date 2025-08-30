package com.x8bit.bitwarden.ui.tools.feature.send.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import com.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.model.AddEditSendType
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendItemType
import kotlinx.serialization.Serializable

/**
 * The type-safe route for the add send screen.
 */
@Serializable
data class AddEditSendRoute(
    val sendType: SendItemType,
    val modeType: ModeType,
    val sendId: String? = null,
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
data class AddEditSendArgs(
    val sendType: SendItemType,
    val addEditSendType: AddEditSendType,
)

/**
 * Constructs a [AddEditSendArgs] from the [SavedStateHandle] and internal route data.
 */
fun SavedStateHandle.toAddEditSendArgs(): AddEditSendArgs {
    val route = this.toRoute<AddEditSendRoute>()
    return AddEditSendArgs(
        sendType = route.sendType,
        addEditSendType = when (route.modeType) {
            ModeType.ADD -> AddEditSendType.AddItem
            ModeType.EDIT -> AddEditSendType.EditItem(sendItemId = requireNotNull(route.sendId))
        },
    )
}

/**
 * Add the add/edit send screen to the nav graph.
 */
fun NavGraphBuilder.addEditSendDestination(
    onNavigateBack: () -> Unit,
    onNavigateUpToSearchOrRoot: () -> Unit,
) {
    composableWithSlideTransitions<AddEditSendRoute> {
        AddEditSendScreen(
            onNavigateBack = onNavigateBack,
            onNavigateUpToSearchOrRoot = onNavigateUpToSearchOrRoot,
        )
    }
}

/**
 * Navigate to the add/edit send screen.
 */
fun NavController.navigateToAddEditSend(
    route: AddEditSendRoute,
    navOptions: NavOptions? = null,
) {
    this.navigate(route = route, navOptions = navOptions)
}
